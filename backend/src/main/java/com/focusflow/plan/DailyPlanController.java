package com.focusflow.plan;

import com.focusflow.plan.dto.DailyPlanResponse;
import com.focusflow.plan.dto.GeneratePlanRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daily-plans")
public class DailyPlanController {

	private final DailyPlanService dailyPlanService;

	public DailyPlanController(DailyPlanService dailyPlanService) {
		this.dailyPlanService = dailyPlanService;
	}

	@PostMapping("/generate")
	@ResponseStatus(HttpStatus.CREATED)
	public DailyPlanResponse generate(@Valid @RequestBody GeneratePlanRequest request) {
		return dailyPlanService.generate(request);
	}
}
