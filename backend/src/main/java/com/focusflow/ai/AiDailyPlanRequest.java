package com.focusflow.ai;

import java.util.List;

public record AiDailyPlanRequest(List<AiPlanTask> tasks, int availableMinutes) {}
