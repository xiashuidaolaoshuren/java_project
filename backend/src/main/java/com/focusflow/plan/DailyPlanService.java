package com.focusflow.plan;

import com.focusflow.ai.AiDailyPlanRequest;
import com.focusflow.ai.AiPlanTask;
import com.focusflow.ai.DailyPlanAiClient;
import com.focusflow.plan.dto.GeneratePlanRequest;
import com.focusflow.security.CurrentUser;
import com.focusflow.task.Task;
import com.focusflow.task.TaskRepository;
import com.focusflow.task.TaskStatus;
import com.focusflow.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DailyPlanService {

	private final DailyPlanAiClient aiClient;
	private final TaskRepository taskRepository;
	private final UserRepository userRepository;
	private final CurrentUser currentUser;
	private final DailyPlanRepository dailyPlanRepository;

	public DailyPlanService(
			DailyPlanAiClient aiClient,
			TaskRepository taskRepository,
			UserRepository userRepository,
			CurrentUser currentUser,
			DailyPlanRepository dailyPlanRepository) {
		this.aiClient = aiClient;
		this.taskRepository = taskRepository;
		this.userRepository = userRepository;
		this.currentUser = currentUser;
		this.dailyPlanRepository = dailyPlanRepository;
	}

	public DailyPlan generate(GeneratePlanRequest request) {
		Long ownerId = currentUser.getCurrentUser().id();
		LocalDate planDate = resolvePlanDate(request.planDate());
		List<Task> activeTasks =
				taskRepository.findByOwner_IdAndStatusOrderByDueDateAsc(ownerId, TaskStatus.OPEN);
		List<AiPlanTask> aiTasks = activeTasks.stream().map(this::toAiPlanTask).toList();
		aiClient.generate(new AiDailyPlanRequest(aiTasks, request.availableMinutes()));
		return null;
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
				task.getEstimatedMinutes());
	}
}
