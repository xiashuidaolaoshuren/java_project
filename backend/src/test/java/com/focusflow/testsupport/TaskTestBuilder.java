package com.focusflow.testsupport;

import com.focusflow.task.Task;
import com.focusflow.task.TaskPriority;
import com.focusflow.task.TaskStatus;
import com.focusflow.user.User;
import java.time.LocalDate;

/**
 * Fluent builder for {@link Task} in tests. Requires a persisted or transient owner.
 */
public final class TaskTestBuilder {

	private final User owner;
	private String title = "Test task";
	private String description;
	private TaskPriority priority = TaskPriority.MEDIUM;
	private TaskStatus status = TaskStatus.OPEN;
	private LocalDate dueDate;
	private Integer estimatedMinutes;

	private TaskTestBuilder(User owner) {
		this.owner = owner;
	}

	public static TaskTestBuilder task(User owner) {
		return new TaskTestBuilder(owner);
	}

	public TaskTestBuilder withTitle(String title) {
		this.title = title;
		return this;
	}

	public TaskTestBuilder withDescription(String description) {
		this.description = description;
		return this;
	}

	public TaskTestBuilder withPriority(TaskPriority priority) {
		this.priority = priority;
		return this;
	}

	public TaskTestBuilder withStatus(TaskStatus status) {
		this.status = status;
		return this;
	}

	public TaskTestBuilder withDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
		return this;
	}

	public TaskTestBuilder withEstimatedMinutes(Integer estimatedMinutes) {
		this.estimatedMinutes = estimatedMinutes;
		return this;
	}

	public Task build() {
		Task task = new Task();
		task.setOwner(owner);
		task.setTitle(title);
		task.setDescription(description);
		task.setPriority(priority);
		task.setStatus(status);
		task.setDueDate(dueDate);
		task.setEstimatedMinutes(estimatedMinutes);
		return task;
	}
}
