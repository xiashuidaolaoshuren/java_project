package com.focusflow.task;

import com.focusflow.auth.dto.UserResponse;
import com.focusflow.common.error.NotFoundException;
import com.focusflow.security.CurrentUser;
import com.focusflow.task.dto.CreateTaskRequest;
import com.focusflow.task.dto.TaskResponse;
import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

	private final TaskRepository taskRepository;
	private final UserRepository userRepository;
	private final CurrentUser currentUser;

	public TaskService(
			TaskRepository taskRepository,
			UserRepository userRepository,
			CurrentUser currentUser) {
		this.taskRepository = taskRepository;
		this.userRepository = userRepository;
		this.currentUser = currentUser;
	}

	@Transactional
	public TaskResponse create(CreateTaskRequest request) {
		User owner = loadCurrentUserEntity();

		Task task = new Task();
		task.setOwner(owner);
		task.setTitle(request.title());
		task.setDescription(request.description());
		task.setPriority(
				request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
		task.setStatus(TaskStatus.OPEN);
		task.setDueDate(request.dueDate());
		task.setEstimatedMinutes(request.estimatedMinutes());

		return toResponse(taskRepository.save(task));
	}

	public List<TaskResponse> listForCurrentUser() {
		Long ownerId = currentUser.getCurrentUser().id();
		return taskRepository.findByOwner_IdOrderByDueDateAsc(ownerId).stream()
				.map(this::toResponse)
				.toList();
	}

	private User loadCurrentUserEntity() {
		UserResponse current = currentUser.getCurrentUser();
		return userRepository
				.findById(current.id())
				.orElseThrow(() -> new NotFoundException("user not found"));
	}

	private TaskResponse toResponse(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getTitle(),
				task.getDescription(),
				task.getPriority(),
				task.getStatus(),
				task.getDueDate(),
				task.getEstimatedMinutes());
	}
}
