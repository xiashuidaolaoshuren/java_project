package com.focusflow.plan;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.focusflow.ai.AiProviderException;
import com.focusflow.common.error.GlobalExceptionHandler;
import com.focusflow.plan.dto.DailyPlanItemResponse;
import com.focusflow.plan.dto.DailyPlanResponse;
import com.focusflow.plan.dto.GeneratePlanRequest;
import com.focusflow.security.FocusFlowUserDetailsService;
import com.focusflow.security.SecurityConfig;
import com.focusflow.task.TaskPriority;
import com.focusflow.task.TaskStatus;
import com.focusflow.task.dto.TaskResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DailyPlanController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DailyPlanControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private DailyPlanService dailyPlanService;

	@MockBean
	private FocusFlowUserDetailsService userDetailsService;

	@Test
	void generate_whenUnauthenticated_returns403() throws Exception {
		mockMvc.perform(
						post("/api/daily-plans/generate")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "availableMinutes": 120
										}
										"""))
				.andExpect(status().isForbidden());

		verify(dailyPlanService, never()).generate(any(GeneratePlanRequest.class));
	}

	@Test
	@WithMockUser
	void generate_withInvalidAvailableMinutes_returns400WithDetails() throws Exception {
		mockMvc.perform(
						post("/api/daily-plans/generate")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "availableMinutes": 0
										}
										"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.path").value("/api/daily-plans/generate"))
				.andExpect(jsonPath("$.details.availableMinutes").isArray());

		verify(dailyPlanService, never()).generate(any(GeneratePlanRequest.class));
	}

	@Test
	@WithMockUser
	void generate_whenAuthenticated_returns201AndBody() throws Exception {
		when(dailyPlanService.generate(any(GeneratePlanRequest.class)))
				.thenReturn(
						new DailyPlanResponse(
								1L,
								LocalDate.of(2026, 6, 1),
								Instant.parse("2026-06-01T09:00:00Z"),
								List.of(
										new DailyPlanItemResponse(
												1,
												new TaskResponse(
														10L,
														"Write tests",
														null,
														TaskPriority.HIGH,
														TaskStatus.OPEN,
														null,
														45)))));

		mockMvc.perform(
						post("/api/daily-plans/generate")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "availableMinutes": 120,
										  "planDate": "2026-06-01"
										}
										"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.planDate").value("2026-06-01"))
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].position").value(1))
				.andExpect(jsonPath("$.items[0].task.title").value("Write tests"));
	}

	@Test
	@WithMockUser
	void generate_whenAiProviderFails_returns502WithStandardBody() throws Exception {
		when(dailyPlanService.generate(any(GeneratePlanRequest.class)))
				.thenThrow(new AiProviderException("provider down"));

		mockMvc.perform(
						post("/api/daily-plans/generate")
								.with(csrf())
								.contentType(MediaType.APPLICATION_JSON)
								.content(
										"""
										{
										  "availableMinutes": 120
										}
										"""))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.status").value(502))
				.andExpect(jsonPath("$.message").value("provider down"))
				.andExpect(jsonPath("$.path").value("/api/daily-plans/generate"));
	}
}
