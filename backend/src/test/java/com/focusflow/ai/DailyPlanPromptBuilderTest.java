package com.focusflow.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.focusflow.task.TaskPriority;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DailyPlanPromptBuilderTest {

	private final DailyPlanPromptBuilder promptBuilder = new DailyPlanPromptBuilder();

	@Test
	void build_includesAvailableMinutesAndTaskCoreFields() {
		AiDailyPlanRequest request =
				new AiDailyPlanRequest(
						List.of(
								new AiPlanTask(
										1L,
										"Write tests",
										"TDD coverage",
										TaskPriority.HIGH,
										LocalDate.of(2026, 6, 1),
										45)),
						120);

		String prompt = promptBuilder.build(request);

		assertThat(prompt).contains("120");
		assertThat(prompt).contains("Write tests");
		assertThat(prompt).contains("HIGH");
	}

	@Test
	void build_omitsNullOptionalFieldsWithoutNullNoise() {
		AiDailyPlanRequest request =
				new AiDailyPlanRequest(
						List.of(
								new AiPlanTask(
										2L, "Minimal task", null, TaskPriority.MEDIUM, null, null)),
						60);

		String prompt = promptBuilder.build(request);

		assertThat(prompt).doesNotContain("null");
		assertThat(prompt).contains("Minimal task");
		assertThat(prompt).contains("MEDIUM");
	}

	@Test
	void build_includesOptionalFieldsWhenPresent() {
		AiDailyPlanRequest request =
				new AiDailyPlanRequest(
						List.of(
								new AiPlanTask(
										1L,
										"Write tests",
										"TDD coverage",
										TaskPriority.HIGH,
										LocalDate.of(2026, 6, 1),
										45)),
						120);

		String prompt = promptBuilder.build(request);

		assertThat(prompt).contains("TDD coverage");
		assertThat(prompt).contains("2026-06-01");
		assertThat(prompt).contains("45");
	}

	@Test
	void build_preservesTaskOrderAndIncludesTaskIds() {
		AiDailyPlanRequest request =
				new AiDailyPlanRequest(
						List.of(
								new AiPlanTask(
										10L,
										"First task",
										null,
										TaskPriority.HIGH,
										null,
										null),
								new AiPlanTask(
										20L,
										"Second task",
										null,
										TaskPriority.LOW,
										null,
										null)),
						90);

		String prompt = promptBuilder.build(request);

		assertThat(prompt).contains("Task 10");
		assertThat(prompt).contains("Task 20");
		assertThat(prompt.indexOf("First task")).isLessThan(prompt.indexOf("Second task"));
	}
}
