package com.focusflow.ai;

import org.springframework.stereotype.Component;

@Component
public class DailyPlanPromptBuilder {

	public String build(AiDailyPlanRequest request) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("Available focus minutes: ").append(request.availableMinutes()).append('\n');
		for (AiPlanTask task : request.tasks()) {
			prompt.append(formatTaskLine(task)).append('\n');
		}
		prompt.append("Ranking rules:\n");
		prompt.append("- Prefer HIGH over MEDIUM over LOW priority.\n");
		prompt.append(
				"- When priority is equal, prefer the sooner due date. Use title and description only to break ties when both priority and due date are equal.\n");
		prompt.append(
				"- Include every must-continue (in-progress) task, then every due-or-overdue open task, then optional work.\n");
		prompt.append(
				"- optional work must fit the leftover minutes after must-include work. You may leave unused leftover rather than squeeze in a lower-priority task.\n");
		prompt.append(
				"- Prefer tasks that have estimates. Do not pile on unestimated optional tasks.\n");
		prompt.append("Return an ordered daily plan using only the listed task ids.");
		return prompt.toString();
	}

	private String formatTaskLine(AiPlanTask task) {
		StringBuilder line =
				new StringBuilder("- Task ")
						.append(task.id())
						.append(": ")
						.append(task.title())
						.append(" (priority=")
						.append(task.priority());
		if (task.description() != null && !task.description().isBlank()) {
			line.append(", description=").append(task.description());
		}
		if (task.dueDate() != null) {
			line.append(", dueDate=").append(task.dueDate());
		}
		if (task.estimatedMinutes() != null) {
			line.append(", estimatedMinutes=").append(task.estimatedMinutes());
		}
		line.append(", status=").append(task.status());
		line.append(')');
		return line.toString();
	}
}
