package com.focusflow.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.focusflow.auth.dto.UserResponse;
import com.focusflow.security.CurrentUser;
import com.focusflow.task.dto.CreateTaskRequest;
import com.focusflow.task.dto.TaskResponse;
import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private CurrentUser currentUser;

	@InjectMocks
	private TaskService taskService;

	@Test
	void create_bindsOwnerFromCurrentUser() {
		UserResponse current = new UserResponse(42L, "owner@example.com", "owner");
		when(currentUser.getCurrentUser()).thenReturn(current);

		User owner = new User();
		owner.setEmail("owner@example.com");
		owner.setUsername("owner");
		when(userRepository.findById(42L)).thenReturn(Optional.of(owner));

		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		taskService.create(new CreateTaskRequest("My task", null, TaskPriority.HIGH, null, null));

		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).save(captor.capture());
		assertThat(captor.getValue().getOwner()).isSameAs(owner);
	}

	@Test
	void create_defaultsStatusOpenAndPriorityMediumWhenOmitted() {
		UserResponse current = new UserResponse(42L, "owner@example.com", "owner");
		when(currentUser.getCurrentUser()).thenReturn(current);

		User owner = new User();
		owner.setEmail("owner@example.com");
		owner.setUsername("owner");
		when(userRepository.findById(42L)).thenReturn(Optional.of(owner));
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		taskService.create(new CreateTaskRequest("My task", null, null, null, null));

		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(TaskStatus.OPEN);
		assertThat(captor.getValue().getPriority()).isEqualTo(TaskPriority.MEDIUM);
	}

	@Test
	void list_fetchesByOwnerId() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserResponse(42L, "owner@example.com", "owner"));

		Task task = new Task();
		task.setTitle("Listed");
		when(taskRepository.findByOwner_IdOrderByDueDateAsc(42L)).thenReturn(List.of(task));

		List<TaskResponse> responses = taskService.listForCurrentUser();

		verify(taskRepository).findByOwner_IdOrderByDueDateAsc(eq(42L));
		assertThat(responses).singleElement().satisfies(r -> assertThat(r.title()).isEqualTo("Listed"));
	}
}
