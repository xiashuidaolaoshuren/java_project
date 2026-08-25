package com.focusflow.plan;

import static org.assertj.core.api.Assertions.assertThat;

import com.focusflow.plan.dto.DailyPlanWarning;
import java.util.List;
import org.junit.jupiter.api.Test;

class DailyPlanWarningTest {

	@Test
	void from_mapsAllFields() {
		DailyPlanWarningSnapshot snapshot =
				new DailyPlanWarningSnapshot(
						90,
						List.of(new DailyPlanWarningSnapshot.EstimatedTask(1L, "Continue work", 60)),
						List.of(new DailyPlanWarningSnapshot.UnestimatedTask(2L, "Due today")));

		DailyPlanWarning warning = DailyPlanWarning.from(snapshot);

		assertThat(warning.minimumAvailableMinutes()).isEqualTo(90);
		assertThat(warning.estimatedTasks())
				.singleElement()
				.satisfies(
						task -> {
							assertThat(task.taskId()).isEqualTo(1L);
							assertThat(task.title()).isEqualTo("Continue work");
							assertThat(task.estimatedMinutes()).isEqualTo(60);
						});
		assertThat(warning.unestimatedTasks())
				.singleElement()
				.satisfies(
						task -> {
							assertThat(task.taskId()).isEqualTo(2L);
							assertThat(task.title()).isEqualTo("Due today");
						});
	}
}
