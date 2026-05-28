package com.focusflow.task;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskQueryService {

	private final TaskRepository taskRepository;

	public TaskQueryService(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	public List<Task> findOpenTasksByOwnerId(Long ownerId) {
		return taskRepository.findByOwner_IdAndStatusOrderByDueDateAsc(ownerId, TaskStatus.OPEN);
	}
}
