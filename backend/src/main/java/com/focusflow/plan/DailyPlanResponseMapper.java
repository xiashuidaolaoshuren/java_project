package com.focusflow.plan;

import com.focusflow.plan.dto.DailyPlanItemResponse;
import com.focusflow.plan.dto.DailyPlanResponse;
import com.focusflow.plan.dto.DailyPlanWarning;
import com.focusflow.task.TaskResponseMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DailyPlanResponseMapper {

	private final TaskResponseMapper taskResponseMapper;

	public DailyPlanResponseMapper(TaskResponseMapper taskResponseMapper) {
		this.taskResponseMapper = taskResponseMapper;
	}

	public DailyPlanResponse toResponse(DailyPlan plan) {
		List<DailyPlanItemResponse> items =
				plan.getItems().stream()
						.map(
								item ->
										new DailyPlanItemResponse(
												item.getPosition(),
												taskResponseMapper.toResponse(item.getTask())))
						.toList();
		return new DailyPlanResponse(
				plan.getId(),
				plan.getPlanDate(),
				plan.getCreatedAt(),
				items,
				plan.getAvailableMinutes(),
				DailyPlanWarning.from(plan.getWarning()));
	}
}
