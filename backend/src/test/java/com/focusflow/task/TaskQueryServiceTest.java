package com.focusflow.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskQueryServiceTest {

	@Mock
	private TaskRepository taskRepository;

	@InjectMocks
	private TaskQueryService taskQueryService;

	@Test
	void findOpenTasksByOwnerId_delegatesToRepository() {
		Task task = new Task();
		task.setTitle("Open task");
		when(taskRepository.findByOwner_IdAndStatusOrderByDueDateAsc(42L, TaskStatus.OPEN))
				.thenReturn(List.of(task));

		List<Task> tasks = taskQueryService.findOpenTasksByOwnerId(42L);

		verify(taskRepository).findByOwner_IdAndStatusOrderByDueDateAsc(42L, TaskStatus.OPEN);
		assertThat(tasks).singleElement().satisfies(t -> assertThat(t.getTitle()).isEqualTo("Open task"));
	}
}
