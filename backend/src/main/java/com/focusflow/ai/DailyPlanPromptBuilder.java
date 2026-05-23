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
		line.append(')');
		return line.toString();
	}
}
