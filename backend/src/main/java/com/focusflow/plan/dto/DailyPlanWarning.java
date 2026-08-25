package com.focusflow.plan.dto;

import com.focusflow.plan.DailyPlanWarningSnapshot;
import java.util.List;

public record DailyPlanWarning(
		int minimumAvailableMinutes,
		List<EstimatedTask> estimatedTasks,
		List<UnestimatedTask> unestimatedTasks) {

	public record EstimatedTask(long taskId, String title, int estimatedMinutes) {}

	public record UnestimatedTask(long taskId, String title) {}

	public static DailyPlanWarning from(DailyPlanWarningSnapshot snapshot) {
		if (snapshot == null) {
			return null;
		}
		return new DailyPlanWarning(
				snapshot.minimumAvailableMinutes(),
				snapshot.estimatedTasks().stream()
						.map(
								task ->
										new EstimatedTask(
												task.taskId(),
												task.title(),
												task.estimatedMinutes()))
						.toList(),
				snapshot.unestimatedTasks().stream()
						.map(task -> new UnestimatedTask(task.taskId(), task.title()))
						.toList());
	}
}
