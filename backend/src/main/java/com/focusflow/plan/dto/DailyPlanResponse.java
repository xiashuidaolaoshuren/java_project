package com.focusflow.plan.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DailyPlanResponse(
		Long id,
		LocalDate planDate,
		Instant createdAt,
		List<DailyPlanItemResponse> items,
		Integer availableMinutes,
		DailyPlanWarning warning) {}
