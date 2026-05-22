package com.focusflow.plan;

import com.focusflow.plan.dto.DailyPlanResponse;
import com.focusflow.plan.dto.GeneratePlanRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daily-plans")
public class DailyPlanController {

	private final DailyPlanService dailyPlanService;

	public DailyPlanController(DailyPlanService dailyPlanService) {
		this.dailyPlanService = dailyPlanService;
	}

	@GetMapping
	public List<DailyPlanResponse> list(@RequestParam(required = false) LocalDate planDate) {
		return dailyPlanService.listForCurrentUser(planDate);
	}

	@GetMapping("/{id}")
	public DailyPlanResponse getById(@PathVariable Long id) {
		return dailyPlanService.getForCurrentUser(id);
	}

	@PostMapping("/generate")
	@ResponseStatus(HttpStatus.CREATED)
	public DailyPlanResponse generate(@Valid @RequestBody GeneratePlanRequest request) {
		return dailyPlanService.generate(request);
	}
}
