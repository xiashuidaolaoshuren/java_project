package com.focusflow.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.focusflow.common.error.ConflictException;
import com.focusflow.ai.AiPlanItem;
import com.focusflow.plan.dto.DailyPlanResponse;
import com.focusflow.task.Task;
import com.focusflow.task.TaskQueryService;
import com.focusflow.task.TaskResponseMapper;
import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class DailyPlanPersisterTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private TaskQueryService taskQueryService;

	@Mock
	private DailyPlanRepository dailyPlanRepository;

	private DailyPlanResponseMapper responseMapper;

	private DailyPlanPersister persister;

	@BeforeEach
	void setUp() {
		responseMapper = new DailyPlanResponseMapper(new TaskResponseMapper());
		persister =
				new DailyPlanPersister(
						userRepository, taskQueryService, dailyPlanRepository, responseMapper);
	}

	@Test
	void persistPlan_isTransactional() throws Exception {
		Method method =
				DailyPlanPersister.class.getMethod(
						"persistPlan",
						Long.class,
						LocalDate.class,
						List.class,
						int.class,
						DailyPlanWarningSnapshot.class);

		assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
	}

	@Test
	void persistPlan_persistsPlanAndReturnsResponse() {
		User owner = new User();
		Task task = createTask(1L, owner, "Continue work", 60);
		LocalDate planDate = LocalDate.of(2026, 8, 28);
		List<AiPlanItem> aiItems = List.of(new AiPlanItem(1L, 1));

		when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(taskQueryService.findOwnedTasksByIds(1L, List.of(1L))).thenReturn(List.of(task));
		when(dailyPlanRepository.save(any(DailyPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DailyPlanResponse response =
				persister.persistPlan(1L, planDate, aiItems, 120, null);

		ArgumentCaptor<DailyPlan> captor = ArgumentCaptor.forClass(DailyPlan.class);
		verify(dailyPlanRepository).save(captor.capture());
		DailyPlan savedPlan = captor.getValue();
		assertThat(savedPlan.getOwner()).isSameAs(owner);
		assertThat(savedPlan.getPlanDate()).isEqualTo(planDate);
		assertThat(savedPlan.getAvailableMinutes()).isEqualTo(120);
		assertThat(savedPlan.getItems()).hasSize(1);
		assertThat(savedPlan.getItems().get(0).getTask()).isSameAs(task);
		assertThat(savedPlan.getItems().get(0).getPosition()).isEqualTo(1);
		assertThat(response.planDate()).isEqualTo(planDate);
		assertThat(response.availableMinutes()).isEqualTo(120);
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).task().id()).isEqualTo(1L);
	}

	@Test
	void persistPlan_persistsAvailableMinutesAndNullWarning_whenSnapshotNull() {
		User owner = new User();
		Task task = createTask(1L, owner, "Continue work", 60);
		LocalDate planDate = LocalDate.of(2026, 8, 28);
		List<AiPlanItem> aiItems = List.of(new AiPlanItem(1L, 1));

		when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(taskQueryService.findOwnedTasksByIds(1L, List.of(1L))).thenReturn(List.of(task));
		when(dailyPlanRepository.save(any(DailyPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DailyPlanResponse response = persister.persistPlan(1L, planDate, aiItems, 120, null);

		ArgumentCaptor<DailyPlan> captor = ArgumentCaptor.forClass(DailyPlan.class);
		verify(dailyPlanRepository).save(captor.capture());
		assertThat(captor.getValue().getAvailableMinutes()).isEqualTo(120);
		assertThat(response.availableMinutes()).isEqualTo(120);
		assertThat(response.warning()).isNull();
	}

	@Test
	void persistPlan_returnsWarning_whenSnapshotPresent() {
		User owner = new User();
		Task task = createTask(1L, owner, "Continue work", 60);
		LocalDate planDate = LocalDate.of(2026, 8, 28);
		List<AiPlanItem> aiItems = List.of(new AiPlanItem(1L, 1));
		DailyPlanWarningSnapshot warning =
				new DailyPlanWarningSnapshot(
						60,
						List.of(new DailyPlanWarningSnapshot.EstimatedTask(1L, "Continue work", 60)),
						List.of());

		when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(taskQueryService.findOwnedTasksByIds(1L, List.of(1L))).thenReturn(List.of(task));
		when(dailyPlanRepository.save(any(DailyPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DailyPlanResponse response = persister.persistPlan(1L, planDate, aiItems, 30, warning);

		assertThat(response.warning()).isNotNull();
		assertThat(response.warning().minimumAvailableMinutes()).isEqualTo(60);
		assertThat(response.warning().estimatedTasks())
				.singleElement()
				.satisfies(
						estimated ->
								assertThat(estimated.taskId())
										.isEqualTo(1L)
										.extracting(id -> estimated.title(), id -> estimated.estimatedMinutes())
										.containsExactly("Continue work", 60));
		assertThat(response.warning().unestimatedTasks()).isEmpty();
	}

	@Test
	void persistPlan_returnsUnestimatedWarning_whenSnapshotPresent() {
		User owner = new User();
		Task task = createTask(1L, owner, "Continue work", null);
		LocalDate planDate = LocalDate.of(2026, 8, 28);
		List<AiPlanItem> aiItems = List.of(new AiPlanItem(1L, 1));
		DailyPlanWarningSnapshot warning =
				new DailyPlanWarningSnapshot(
						0,
						List.of(),
						List.of(new DailyPlanWarningSnapshot.UnestimatedTask(1L, "Continue work")));

		when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(taskQueryService.findOwnedTasksByIds(1L, List.of(1L))).thenReturn(List.of(task));
		when(dailyPlanRepository.save(any(DailyPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DailyPlanResponse response = persister.persistPlan(1L, planDate, aiItems, 120, warning);

		assertThat(response.warning()).isNotNull();
		assertThat(response.warning().minimumAvailableMinutes()).isEqualTo(0);
		assertThat(response.warning().estimatedTasks()).isEmpty();
		assertThat(response.warning().unestimatedTasks())
				.singleElement()
				.satisfies(
						unestimated -> {
							assertThat(unestimated.taskId()).isEqualTo(1L);
							assertThat(unestimated.title()).isEqualTo("Continue work");
						});
	}

	@Test
	void persistPlan_whenSelectedTaskDisappeared_throwsConflictException() {
		LocalDate planDate = LocalDate.of(2026, 8, 28);
		List<AiPlanItem> aiItems = List.of(new AiPlanItem(1L, 1));

		when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
		when(taskQueryService.findOwnedTasksByIds(1L, List.of(1L))).thenReturn(List.of());

		assertThatThrownBy(() -> persister.persistPlan(1L, planDate, aiItems, 120, null))
				.isInstanceOf(ConflictException.class)
				.hasMessage("a selected task is no longer available");
	}

	private Task createTask(Long id, User owner, String title, Integer estimatedMinutes) {
		Task task = new Task();
		task.setTitle(title);
		task.setEstimatedMinutes(estimatedMinutes);
		ReflectionTestUtils.setField(task, "id", id);
		return task;
	}
}
