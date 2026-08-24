package com.focusflow.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focusflow.task.TaskPriority;
import com.focusflow.task.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiDailyPlanClientTest {

	private static final String BASE_URL = "https://api.deepseek.com/v1";

	private MockRestServiceServer mockServer;
	private OpenAiDailyPlanClient client;
	private DailyPlanPromptBuilder promptBuilder;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		mockServer = MockRestServiceServer.bindTo(builder).build();
		RestClient restClient = builder.baseUrl(BASE_URL).build();
		OpenAiProperties properties =
				new OpenAiProperties("test-api-key", "deepseek-chat", BASE_URL);
		promptBuilder = new DailyPlanPromptBuilder();
		client = new OpenAiDailyPlanClient(restClient, promptBuilder, properties, new ObjectMapper());
	}

	@Test
	void generate_usesConfiguredBaseUrlModelAndPromptBuilderOutput() {
		AiDailyPlanRequest request =
				new AiDailyPlanRequest(
						List.of(
								new AiPlanTask(
										1L,
										"Write tests",
										"TDD coverage",
										TaskPriority.HIGH,
										LocalDate.of(2026, 6, 1),
										45,
										TaskStatus.OPEN)),
						120);
		String expectedPrompt = promptBuilder.build(request) + OpenAiDailyPlanClient.STRUCTURED_OUTPUT_SUFFIX;

		mockServer
				.expect(requestTo(BASE_URL + "/chat/completions"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer test-api-key"))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(
						content()
								.json(
										"""
										{
										  "model": "deepseek-chat",
										  "messages": [
										    {
										      "role": "user",
										      "content": "%s"
										    }
										  ]
										}
										"""
												.formatted(escapeJson(expectedPrompt))))
				.andRespond(
						withSuccess(
								"""
								{
								  "choices": [
								    {
								      "message": {
								        "content": "{\\"items\\":[]}"
								      }
								    }
								  ]
								}
								""",
								MediaType.APPLICATION_JSON));

		AiDailyPlanResponse response = client.generate(request);

		assertThat(response.items()).isEmpty();
		mockServer.verify();
	}

	@Test
	void generate_mapsValidStructuredProviderResponseIntoOrderedPlanItems() {
		AiDailyPlanRequest request =
				new AiDailyPlanRequest(
						List.of(
								new AiPlanTask(
										10L,
										"First task",
										null,
										TaskPriority.HIGH,
										null,
										null,
										TaskStatus.OPEN),
								new AiPlanTask(
										20L,
										"Second task",
										null,
										TaskPriority.LOW,
										null,
										null,
										TaskStatus.OPEN)),
						90);

		mockServer
				.expect(requestTo(BASE_URL + "/chat/completions"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withSuccess(
								"""
								{
								  "choices": [
								    {
								      "message": {
								        "content": "{\\"items\\":[{\\"taskId\\":20,\\"position\\":1},{\\"taskId\\":10,\\"position\\":2}]}"
								      }
								    }
								  ]
								}
								""",
								MediaType.APPLICATION_JSON));

		AiDailyPlanResponse response = client.generate(request);

		assertThat(response.items())
				.containsExactly(new AiPlanItem(20L, 1), new AiPlanItem(10L, 2));
		mockServer.verify();
	}

	@Test
	void generate_whenProviderReturnsHttpError_throwsAiProviderExceptionWithSafeMessage() {
		AiDailyPlanRequest request =
				new AiDailyPlanRequest(
						List.of(
								new AiPlanTask(
										1L,
										"Write tests",
										null,
										TaskPriority.HIGH,
										null,
										null,
										TaskStatus.OPEN)),
						60);

		mockServer
				.expect(requestTo(BASE_URL + "/chat/completions"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withServerError());

		assertThatThrownBy(() -> client.generate(request))
				.isInstanceOf(AiProviderException.class)
				.hasMessageContaining("AI provider request failed")
				.hasMessageNotContaining("test-api-key");

		mockServer.verify();
	}

	@Test
	void generate_whenStructuredOutputMissingItems_throwsAiProviderException() {
		expectChatCompletionResponse("{\"summary\":\"no items here\"}");

		assertMalformedOutputFailure();
	}

	@Test
	void generate_whenStructuredOutputHasNonPositiveTaskId_throwsAiProviderException() {
		expectChatCompletionResponse("{\"items\":[{\"taskId\":0,\"position\":1}]}");

		assertMalformedOutputFailure();
	}

	@Test
	void generate_whenStructuredOutputHasNonPositivePosition_throwsAiProviderException() {
		expectChatCompletionResponse("{\"items\":[{\"taskId\":1,\"position\":0}]}");

		assertMalformedOutputFailure();
	}

	@Test
	void generate_whenStructuredOutputIsInvalidJson_throwsAiProviderException() {
		expectChatCompletionResponse("not-json");

		assertMalformedOutputFailure();
	}

	private void expectChatCompletionResponse(String structuredContent) {
		String escapedContent =
				structuredContent.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
		mockServer
				.expect(requestTo(BASE_URL + "/chat/completions"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(
						withSuccess(
								"""
								{
								  "choices": [
								    {
								      "message": {
								        "content": "%s"
								      }
								    }
								  ]
								}
								"""
										.formatted(escapedContent),
								MediaType.APPLICATION_JSON));
	}

	private void assertMalformedOutputFailure() {
		AiDailyPlanRequest request =
				new AiDailyPlanRequest(
						List.of(
								new AiPlanTask(
										1L,
										"Write tests",
										null,
										TaskPriority.HIGH,
										null,
										null,
										TaskStatus.OPEN)),
						60);

		assertThatThrownBy(() -> client.generate(request))
				.isInstanceOf(AiProviderException.class);

		mockServer.verify();
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}
}
