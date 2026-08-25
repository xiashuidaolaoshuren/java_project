package com.focusflow.plan;

import com.focusflow.ai.AiDailyPlanRequest;
import com.focusflow.ai.AiDailyPlanResponse;
import com.focusflow.ai.AiPlanItem;
import com.focusflow.ai.AiPlanTask;
import com.focusflow.ai.DailyPlanAiClient;
import com.focusflow.common.error.BadRequestException;
import com.focusflow.common.error.NotFoundException;
import com.focusflow.plan.dto.DailyPlanItemResponse;
import com.focusflow.plan.dto.DailyPlanResponse;
import com.focusflow.plan.dto.DailyPlanWarning;
import com.focusflow.plan.dto.GeneratePlanRequest;
import com.focusflow.security.CurrentUser;
import com.focusflow.task.Task;
import com.focusflow.task.TaskQueryService;
import com.focusflow.task.TaskResponseMapper;
import com.focusflow.task.TaskStatus;
import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyPlanService {

	private final DailyPlanAiClient aiClient;
	private final TaskQueryService taskQueryService;
	private final UserRepository userRepository;
	private final CurrentUser currentUser;
	private final DailyPlanRepository dailyPlanRepository;
	private final TaskResponseMapper taskResponseMapper;
	private final DailyPlanRankingValidator rankingValidator;

	public DailyPlanService(
			DailyPlanAiClient aiClient,
			TaskQueryService taskQueryService,
			UserRepository userRepository,
			CurrentUser currentUser,
			DailyPlanRepository dailyPlanRepository,
			TaskResponseMapper taskResponseMapper,
			DailyPlanRankingValidator rankingValidator) {
		this.aiClient = aiClient;
		this.taskQueryService = taskQueryService;
		this.userRepository = userRepository;
		this.currentUser = currentUser;
		this.dailyPlanRepository = dailyPlanRepository;
		this.taskResponseMapper = taskResponseMapper;
		this.rankingValidator = rankingValidator;
	}

	@Transactional
	public DailyPlanResponse generate(GeneratePlanRequest request) {
		Long ownerId = currentUser.getCurrentUser().id();
		LocalDate planDate = resolvePlanDate(request.planDate());
		List<Task> activeTasks = taskQueryService.findPlannableTasksByOwnerId(ownerId);
		if (activeTasks.isEmpty()) {
			throw new BadRequestException("no plannable tasks available for planning");
		}
		Map<Long, Task> taskById =
				activeTasks.stream()
						.collect(
								Collectors.toMap(
										task -> task.getId() != null ? task.getId() : 0L,
										Function.identity(),
										(first, second) -> first));
		List<AiPlanTask> aiTasks = activeTasks.stream().map(this::toAiPlanTask).toList();
		AiDailyPlanResponse aiResponse =
				aiClient.generate(new AiDailyPlanRequest(aiTasks, request.availableMinutes(), planDate));
		rankingValidator.validate(
				activeTasks, planDate, request.availableMinutes(), aiResponse.items());
		DailyPlanWarningSnapshot warning =
				computeWarning(activeTasks, planDate, request.availableMinutes());
		User owner =
				userRepository
						.findById(ownerId)
						.orElseThrow(() -> new NotFoundException("user not found"));
		DailyPlan plan =
				buildPlan(
						owner,
						planDate,
						aiResponse.items(),
						taskById,
						request.availableMinutes(),
						warning);
		return toPlanResponse(dailyPlanRepository.save(plan));
	}

	public List<DailyPlanResponse> listForCurrentUser(LocalDate planDate) {
		Long ownerId = currentUser.getCurrentUser().id();
		List<DailyPlan> plans =
				planDate != null
						? dailyPlanRepository.findByOwner_IdAndPlanDateOrderByCreatedAtDesc(
								ownerId, planDate)
						: dailyPlanRepository.findAllByOwner_IdOrderByCreatedAtDesc(ownerId);
		return plans.stream().map(this::toPlanResponse).toList();
	}

	public DailyPlanResponse getForCurrentUser(Long planId) {
		return toPlanResponse(loadPlanForCurrentUser(planId));
	}

	@Transactional
	public void deleteForCurrentUser(Long planId) {
		dailyPlanRepository.delete(loadPlanForCurrentUser(planId));
	}

	private DailyPlan loadPlanForCurrentUser(Long planId) {
		Long ownerId = currentUser.getCurrentUser().id();
		return dailyPlanRepository
				.findByOwner_IdAndId(ownerId, planId)
				.orElseThrow(() -> new NotFoundException("daily plan not found"));
	}

	private DailyPlan buildPlan(
			User owner,
			LocalDate planDate,
			List<AiPlanItem> aiItems,
			Map<Long, Task> taskById,
			int availableMinutes,
			DailyPlanWarningSnapshot warning) {
		DailyPlan plan = new DailyPlan();
		plan.setOwner(owner);
		plan.setPlanDate(planDate);
		plan.setCreatedAt(Instant.now());
		plan.setAvailableMinutes(availableMinutes);
		plan.setWarning(warning);
		for (AiPlanItem aiItem : aiItems) {
			Task task = taskById.get(aiItem.taskId());
			DailyPlanItem item = new DailyPlanItem();
			item.setTask(task);
			item.setPosition(aiItem.position());
			plan.addItem(item);
		}
		return plan;
	}

	private DailyPlanResponse toPlanResponse(DailyPlan plan) {
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

	private DailyPlanWarningSnapshot computeWarning(
			List<Task> candidates, LocalDate planDate, int availableMinutes) {
		List<DailyPlanWarningSnapshot.EstimatedTask> estimatedTasks = new ArrayList<>();
		List<DailyPlanWarningSnapshot.UnestimatedTask> unestimatedTasks = new ArrayList<>();
		int minimumAvailableMinutes = 0;

		for (Task candidate : candidates) {
			if (!isMustInclude(candidate, planDate)) {
				continue;
			}
			long taskId = candidate.getId() != null ? candidate.getId() : 0L;
			Integer estimate = candidate.getEstimatedMinutes();
			if (estimate != null) {
				minimumAvailableMinutes += estimate;
				estimatedTasks.add(
						new DailyPlanWarningSnapshot.EstimatedTask(
								taskId, candidate.getTitle(), estimate));
			} else {
				unestimatedTasks.add(
						new DailyPlanWarningSnapshot.UnestimatedTask(taskId, candidate.getTitle()));
			}
		}

		if (unestimatedTasks.isEmpty() && availableMinutes >= minimumAvailableMinutes) {
			return null;
		}
		return new DailyPlanWarningSnapshot(
				minimumAvailableMinutes, estimatedTasks, unestimatedTasks);
	}

	private boolean isMustInclude(Task task, LocalDate planDate) {
		if (task.getStatus() == TaskStatus.IN_PROGRESS) {
			return true;
		}
		if (task.getStatus() == TaskStatus.OPEN) {
			LocalDate dueDate = task.getDueDate();
			return dueDate != null && !dueDate.isAfter(planDate);
		}
		return false;
	}

	private LocalDate resolvePlanDate(LocalDate planDate) {
		return planDate != null ? planDate : LocalDate.now();
	}

	private AiPlanTask toAiPlanTask(Task task) {
		return new AiPlanTask(
				task.getId() != null ? task.getId() : 0L,
				task.getTitle(),
				task.getDescription(),
				task.getPriority(),
				task.getDueDate(),
				task.getEstimatedMinutes(),
				task.getStatus());
	}
}
