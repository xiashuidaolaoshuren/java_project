package com.focusflow.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.focusflow.ai.AiDailyPlanRequest;
import com.focusflow.ai.AiDailyPlanResponse;
import com.focusflow.ai.AiProviderException;
import com.focusflow.ai.DailyPlanAiClient;
import com.focusflow.auth.dto.UserResponse;
import com.focusflow.plan.dto.GeneratePlanRequest;
import com.focusflow.security.CurrentUser;
import com.focusflow.task.Task;
import com.focusflow.task.TaskPriority;
import com.focusflow.task.TaskRepository;
import com.focusflow.task.TaskStatus;
import com.focusflow.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyPlanServiceTest {

	@Mock
	private DailyPlanAiClient aiClient;

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private CurrentUser currentUser;

	@Mock
	private DailyPlanRepository dailyPlanRepository;

	@InjectMocks
	private DailyPlanService dailyPlanService;

	@Test
	void generate_callsAiClientWithCurrentUserActiveTasks() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserResponse(42L, "user@example.com", "user"));

		Task task = new Task();
		task.setTitle("Write tests");
		task.setPriority(TaskPriority.HIGH);
		task.setStatus(TaskStatus.OPEN);
		when(taskRepository.findByOwner_IdAndStatusOrderByDueDateAsc(42L, TaskStatus.OPEN))
				.thenReturn(List.of(task));
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenReturn(new AiDailyPlanResponse(List.of()));

		dailyPlanService.generate(new GeneratePlanRequest(120, LocalDate.of(2026, 6, 1)));

		ArgumentCaptor<AiDailyPlanRequest> captor = ArgumentCaptor.forClass(AiDailyPlanRequest.class);
		verify(aiClient).generate(captor.capture());
		assertThat(captor.getValue().tasks()).hasSize(1);
		assertThat(captor.getValue().availableMinutes()).isEqualTo(120);
	}

	@Test
	void generate_whenAiClientThrows_propagatesAiProviderException() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserResponse(42L, "user@example.com", "user"));
		when(taskRepository.findByOwner_IdAndStatusOrderByDueDateAsc(42L, TaskStatus.OPEN))
				.thenReturn(List.of());
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenThrow(new AiProviderException("provider down"));

		assertThatThrownBy(() -> dailyPlanService.generate(new GeneratePlanRequest(60, null)))
				.isInstanceOf(AiProviderException.class)
				.hasMessage("provider down");
	}
}
