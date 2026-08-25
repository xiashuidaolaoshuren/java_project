package com.focusflow.ai;

import java.time.LocalDate;
import java.util.List;

public record AiDailyPlanRequest(List<AiPlanTask> tasks, int availableMinutes, LocalDate planDate) {}
