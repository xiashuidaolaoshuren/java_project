package com.focusflow.ai;

import com.focusflow.task.TaskPriority;
import java.time.LocalDate;

public record AiPlanTask(
		long id,
		String title,
		String description,
		TaskPriority priority,
		LocalDate dueDate,
		Integer estimatedMinutes) {}
