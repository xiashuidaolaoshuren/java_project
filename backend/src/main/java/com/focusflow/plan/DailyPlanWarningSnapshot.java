package com.focusflow.plan;

import java.util.List;

public record DailyPlanWarningSnapshot(
		int minimumAvailableMinutes,
		List<EstimatedTask> estimatedTasks,
		List<UnestimatedTask> unestimatedTasks) {

	public record EstimatedTask(long taskId, String title, int estimatedMinutes) {}

	public record UnestimatedTask(long taskId, String title) {}
}
