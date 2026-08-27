package com.focusflow.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class OpenAiDailyPlanClient implements DailyPlanAiClient {

	private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
	static final String STRUCTURED_OUTPUT_SUFFIX =
			"""

			Return JSON only with this exact shape: {"items":[{"taskId":<id>,"position":<order>}]}
			Use only the listed task ids and positive position values starting at 1.""";

	private final RestClient restClient;
	private final DailyPlanPromptBuilder promptBuilder;
	private final OpenAiProperties properties;
	private final ObjectMapper objectMapper;
	private final Sleeper sleeper;

	public OpenAiDailyPlanClient(
			RestClient openAiRestClient,
			DailyPlanPromptBuilder promptBuilder,
			OpenAiProperties properties,
			ObjectMapper objectMapper,
			Sleeper sleeper) {
		this.restClient = openAiRestClient;
		this.promptBuilder = promptBuilder;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.sleeper = sleeper;
	}

	@Override
	public AiDailyPlanResponse generate(AiDailyPlanRequest request) {
		String prompt = promptBuilder.build(request) + STRUCTURED_OUTPUT_SUFFIX;
		Map<String, Object> requestBody =
				Map.of(
						"model",
						properties.model(),
						"messages",
						List.of(Map.of("role", "user", "content", prompt)));

		RestClientException lastException = null;
		for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
			try {
				String responseBody =
						restClient
								.post()
								.uri(CHAT_COMPLETIONS_PATH)
								.header("Authorization", "Bearer " + properties.apiKey())
								.contentType(MediaType.APPLICATION_JSON)
								.body(requestBody)
								.retrieve()
								.body(String.class);
				return parseProviderResponse(responseBody);
			} catch (RestClientException ex) {
				lastException = ex;
				if (!isRetryable(ex) || attempt >= properties.maxAttempts()) {
					throw new AiProviderException("AI provider request failed", ex);
				}
				sleepBeforeRetry(ex);
			}
		}

		throw new AiProviderException("AI provider request failed", lastException);
	}

	private void sleepBeforeRetry(RestClientException ex) {
		try {
			sleeper.sleep(resolveRetryDelay(ex));
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new AiProviderException("AI provider request interrupted", interrupted);
		}
	}

	private Duration resolveRetryDelay(RestClientException ex) {
		if (ex instanceof HttpClientErrorException.TooManyRequests tooManyRequests) {
			String retryAfterHeader = tooManyRequests.getResponseHeaders().getFirst("Retry-After");
			if (retryAfterHeader != null) {
				try {
					long retryAfterSeconds = Long.parseLong(retryAfterHeader.trim());
					Duration retryAfter = Duration.ofSeconds(retryAfterSeconds);
					return retryAfter.compareTo(properties.maxRetryAfter()) > 0
							? properties.maxRetryAfter()
							: retryAfter;
				} catch (NumberFormatException ignored) {
					// fall through to default retry delay
				}
			}
		}
		return properties.retryDelay();
	}

	private boolean isRetryable(RestClientException ex) {
		if (ex instanceof HttpClientErrorException clientError) {
			return clientError.getStatusCode().value() == 429;
		}
		if (ex instanceof HttpServerErrorException serverError) {
			int status = serverError.getStatusCode().value();
			return status == 502 || status == 503 || status == 504;
		}
		if (ex instanceof org.springframework.web.client.ResourceAccessException resourceAccessException) {
			Throwable cause = resourceAccessException.getCause();
			if (cause instanceof SocketTimeoutException) {
				return false;
			}
			return cause instanceof ConnectException;
		}
		return false;
	}

	private AiDailyPlanResponse parseProviderResponse(String responseBody) {
		try {
			ChatCompletionResponse completionResponse =
					objectMapper.readValue(responseBody, ChatCompletionResponse.class);
			if (completionResponse.choices() == null || completionResponse.choices().isEmpty()) {
				throw new AiProviderException("AI provider returned no choices");
			}

			String content = completionResponse.choices().getFirst().message().content();
			StructuredPlanResponse structuredPlanResponse =
					objectMapper.readValue(content, StructuredPlanResponse.class);
			if (structuredPlanResponse.items() == null) {
				throw new AiProviderException("AI provider response missing items");
			}

			List<AiPlanItem> items =
					structuredPlanResponse.items().stream()
							.map(this::toValidatedPlanItem)
							.toList();
			return new AiDailyPlanResponse(items);
		} catch (JsonProcessingException ex) {
			throw new AiProviderException("AI provider returned invalid JSON", ex);
		}
	}

	private AiPlanItem toValidatedPlanItem(StructuredPlanItem item) {
		if (item.taskId() <= 0) {
			throw new AiProviderException("AI provider response has invalid taskId");
		}
		if (item.position() <= 0) {
			throw new AiProviderException("AI provider response has invalid position");
		}
		return new AiPlanItem(item.taskId(), item.position());
	}

	private record ChatCompletionResponse(List<ChatCompletionChoice> choices) {}

	private record ChatCompletionChoice(ChatCompletionMessage message) {}

	private record ChatCompletionMessage(String content) {}

	private record StructuredPlanResponse(List<StructuredPlanItem> items) {}

	private record StructuredPlanItem(long taskId, int position) {}
}
