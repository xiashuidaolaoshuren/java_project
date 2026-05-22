package com.focusflow.plan.dto;

import com.focusflow.task.dto.TaskResponse;

public record DailyPlanItemResponse(int position, TaskResponse task) {}
