package com.focusflow.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.focusflow.ai.AiDailyPlanRequest;
import com.focusflow.ai.AiDailyPlanResponse;
import com.focusflow.ai.AiPlanItem;
import com.focusflow.ai.AiProviderException;
import com.focusflow.ai.DailyPlanAiClient;
import com.focusflow.common.error.BadRequestException;
import com.focusflow.common.error.ConflictException;
import com.focusflow.common.error.NotFoundException;
import com.focusflow.common.web.PageResponse;
import com.focusflow.plan.dto.DailyPlanResponse;
import com.focusflow.plan.dto.DailyPlanSummaryResponse;
import com.focusflow.plan.dto.GeneratePlanRequest;
import com.focusflow.security.CurrentUser;
import com.focusflow.security.UserContext;
import com.focusflow.task.Task;
import com.focusflow.task.TaskPriority;
import com.focusflow.task.TaskQueryService;
import com.focusflow.task.TaskResponseMapper;
import com.focusflow.task.TaskStatus;
import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
class DailyPlanServiceTest {

	@Mock
	private DailyPlanAiClient aiClient;

	@Mock
	private TaskQueryService taskQueryService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private CurrentUser currentUser;

	@Mock
	private DailyPlanRepository dailyPlanRepository;

	@Spy
	private DailyPlanRankingValidator rankingValidator = new DailyPlanRankingValidator();

	private final TaskResponseMapper taskResponseMapper = new TaskResponseMapper();

	private DailyPlanService dailyPlanService;

	@BeforeEach
	void setUp() {
		dailyPlanService =
				new DailyPlanService(
						aiClient,
						taskQueryService,
						userRepository,
						currentUser,
						dailyPlanRepository,
						taskResponseMapper,
						rankingValidator);
	}

	private User stubSuccessfulPersist(Task... tasks) {
		User owner = new User();
		when(userRepository.findById(42L)).thenReturn(Optional.of(owner));
		when(taskQueryService.findOwnedTasksByIds(eq(42L), any()))
				.thenAnswer(
						invocation -> {
							@SuppressWarnings("unchecked")
							Collection<Long> requestedIds = invocation.getArgument(1);
							return java.util.Arrays.stream(tasks)
									.filter(
											task ->
													requestedIds.contains(
															task.getId() != null ? task.getId() : 0L))
									.toList();
						});
		when(dailyPlanRepository.save(any(DailyPlan.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		return owner;
	}

	@Test
	void generate_delegatesToTransactionalPersistPlan() throws Exception {
		Method generate = DailyPlanService.class.getMethod("generate", GeneratePlanRequest.class);
		assertThat(AnnotationUtils.findAnnotation(generate, Transactional.class)).isNull();

		Method persistPlan =
				java.util.Arrays.stream(DailyPlanService.class.getDeclaredMethods())
						.filter(method -> method.getName().equals("persistPlan"))
						.findFirst()
						.orElseThrow();
		assertThat(AnnotationUtils.findAnnotation(persistPlan, Transactional.class)).isNotNull();

		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		Task task = new Task();
		ReflectionTestUtils.setField(task, "id", 1L);
		task.setTitle("Continue work");
		task.setStatus(TaskStatus.IN_PROGRESS);
		task.setPriority(TaskPriority.MEDIUM);
		task.setEstimatedMinutes(30);

		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of(task));
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenReturn(new AiDailyPlanResponse(List.of(new AiPlanItem(1L, 1))));

		stubSuccessfulPersist(task);

		dailyPlanService.generate(new GeneratePlanRequest(120, LocalDate.of(2026, 6, 1)));

		verify(aiClient).generate(any(AiDailyPlanRequest.class));
		verify(dailyPlanRepository).save(any(DailyPlan.class));
	}

	@Test
	void generate_whenSelectedTaskDisappearedBeforePersist_throwsConflictException() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		Task task = new Task();
		ReflectionTestUtils.setField(task, "id", 1L);
		task.setTitle("Continue work");
		task.setStatus(TaskStatus.IN_PROGRESS);
		task.setPriority(TaskPriority.MEDIUM);

		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of(task));
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenReturn(new AiDailyPlanResponse(List.of(new AiPlanItem(1L, 1))));
		when(userRepository.findById(42L)).thenReturn(Optional.of(new User()));
		when(taskQueryService.findOwnedTasksByIds(eq(42L), eq(List.of(1L)))).thenReturn(List.of());

		assertThatThrownBy(
						() -> dailyPlanService.generate(new GeneratePlanRequest(120, LocalDate.of(2026, 6, 1))))
				.isInstanceOf(ConflictException.class)
				.hasMessage("a selected task is no longer available");

		verify(dailyPlanRepository, never()).save(any());
	}

	@Test
	void generate_callsAiClientWithCurrentUserActiveTasks() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		Task task = new Task();
		task.setTitle("Write tests");
		task.setPriority(TaskPriority.HIGH);
		task.setStatus(TaskStatus.OPEN);
		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of(task));
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenReturn(new AiDailyPlanResponse(List.of()));

		stubSuccessfulPersist(task);

		dailyPlanService.generate(new GeneratePlanRequest(120, LocalDate.of(2026, 6, 1)));

		ArgumentCaptor<AiDailyPlanRequest> captor = ArgumentCaptor.forClass(AiDailyPlanRequest.class);
		verify(aiClient).generate(captor.capture());
		assertThat(captor.getValue().tasks()).hasSize(1);
		assertThat(captor.getValue().availableMinutes()).isEqualTo(120);
		assertThat(captor.getValue().planDate()).isEqualTo(LocalDate.of(2026, 6, 1));
	}

	@Test
	void generate_whenNoPlannableTasks_rejectsBeforeProviderCall() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));
		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of());

		assertThatThrownBy(() -> dailyPlanService.generate(new GeneratePlanRequest(60, null)))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("no plannable tasks available for planning");

		verify(aiClient, never()).generate(any());
		verify(dailyPlanRepository, never()).save(any());
	}

	@Test
	void generate_delegatesToRankingValidator() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		Task task = new Task();
		ReflectionTestUtils.setField(task, "id", 1L);
		task.setTitle("Continue work");
		task.setStatus(TaskStatus.IN_PROGRESS);
		task.setPriority(TaskPriority.MEDIUM);
		task.setEstimatedMinutes(30);

		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of(task));
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenReturn(new AiDailyPlanResponse(List.of(new AiPlanItem(1L, 1))));

		stubSuccessfulPersist(task);

		LocalDate planDate = LocalDate.of(2026, 6, 1);
		dailyPlanService.generate(new GeneratePlanRequest(120, planDate));

		verify(rankingValidator)
				.validate(List.of(task), planDate, 120, List.of(new AiPlanItem(1L, 1)));
	}

	@Test
	void generate_whenOnlyInProgressTasks_callsProvider() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		Task task = new Task();
		ReflectionTestUtils.setField(task, "id", 1L);
		task.setTitle("Continue work");
		task.setPriority(TaskPriority.HIGH);
		task.setStatus(TaskStatus.IN_PROGRESS);
		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of(task));
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenReturn(new AiDailyPlanResponse(List.of(new AiPlanItem(1L, 1))));

		stubSuccessfulPersist(task);

		LocalDate planDate = LocalDate.of(2026, 6, 1);
		dailyPlanService.generate(new GeneratePlanRequest(90, planDate));

		ArgumentCaptor<AiDailyPlanRequest> captor = ArgumentCaptor.forClass(AiDailyPlanRequest.class);
		verify(aiClient).generate(captor.capture());
		assertThat(captor.getValue().tasks()).hasSize(1);
		assertThat(captor.getValue().tasks().getFirst().status()).isEqualTo(TaskStatus.IN_PROGRESS);
	}

	@Test
	void generate_doesNotFallbackToTodayWhenPlanDateNull() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		Task task = new Task();
		task.setTitle("Task 1");
		task.setPriority(TaskPriority.HIGH);
		task.setStatus(TaskStatus.OPEN);
		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of(task));
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenReturn(new AiDailyPlanResponse(List.of()));

		stubSuccessfulPersist(task);

		dailyPlanService.generate(new GeneratePlanRequest(60, null));

		ArgumentCaptor<AiDailyPlanRequest> captor = ArgumentCaptor.forClass(AiDailyPlanRequest.class);
		verify(aiClient).generate(captor.capture());
		assertThat(captor.getValue().planDate()).isNull();
	}

	@Test
	void generate_whenAiClientThrows_propagatesAiProviderException() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		Task task = new Task();
		task.setTitle("Task 1");
		task.setPriority(TaskPriority.HIGH);
		task.setStatus(TaskStatus.OPEN);
		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of(task));
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenThrow(new AiProviderException("provider down"));

		assertThatThrownBy(
						() ->
								dailyPlanService.generate(
										new GeneratePlanRequest(60, LocalDate.of(2026, 6, 1))))
				.isInstanceOf(AiProviderException.class)
				.hasMessage("provider down");
	}

	@Test
	void generate_persistsAvailableMinutesAndNullWarning_whenMustIncludeFits() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		Task task = new Task();
		ReflectionTestUtils.setField(task, "id", 1L);
		task.setTitle("Continue work");
		task.setPriority(TaskPriority.HIGH);
		task.setStatus(TaskStatus.IN_PROGRESS);
		task.setEstimatedMinutes(30);
		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of(task));
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenReturn(new AiDailyPlanResponse(List.of(new AiPlanItem(1L, 1))));

		stubSuccessfulPersist(task);

		DailyPlanResponse response =
				dailyPlanService.generate(new GeneratePlanRequest(60, LocalDate.of(2026, 6, 1)));

		ArgumentCaptor<DailyPlan> captor = ArgumentCaptor.forClass(DailyPlan.class);
		verify(dailyPlanRepository).save(captor.capture());
		assertThat(captor.getValue().getAvailableMinutes()).isEqualTo(60);
		assertThat(captor.getValue().getWarning()).isNull();
		assertThat(response.availableMinutes()).isEqualTo(60);
		assertThat(response.warning()).isNull();
	}

	@Test
	void generate_returnsWarning_whenMustIncludeOverflows() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		Task task = new Task();
		ReflectionTestUtils.setField(task, "id", 1L);
		task.setTitle("Continue work");
		task.setPriority(TaskPriority.HIGH);
		task.setStatus(TaskStatus.IN_PROGRESS);
		task.setEstimatedMinutes(60);
		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of(task));
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenReturn(new AiDailyPlanResponse(List.of(new AiPlanItem(1L, 1))));

		stubSuccessfulPersist(task);

		DailyPlanResponse response =
				dailyPlanService.generate(new GeneratePlanRequest(30, LocalDate.of(2026, 6, 1)));

		assertThat(response.availableMinutes()).isEqualTo(30);
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
	void generate_returnsWarning_whenMustIncludeHasUnestimated() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		Task task = new Task();
		ReflectionTestUtils.setField(task, "id", 1L);
		task.setTitle("Continue work");
		task.setPriority(TaskPriority.HIGH);
		task.setStatus(TaskStatus.IN_PROGRESS);
		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of(task));
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenReturn(new AiDailyPlanResponse(List.of(new AiPlanItem(1L, 1))));

		stubSuccessfulPersist(task);

		DailyPlanResponse response =
				dailyPlanService.generate(new GeneratePlanRequest(60, LocalDate.of(2026, 6, 1)));

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
	void generate_persistsPlanAndReturnsResponse() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		Task task = new Task();
		task.setTitle("Task 1");
		task.setPriority(TaskPriority.HIGH);
		task.setStatus(TaskStatus.OPEN);
		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of(task));

		AiPlanItem aiItem = new AiPlanItem(task.getId() != null ? task.getId() : 0L, 1);
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenReturn(new AiDailyPlanResponse(List.of(aiItem)));

		User owner = stubSuccessfulPersist(task);

		DailyPlanResponse response =
				dailyPlanService.generate(new GeneratePlanRequest(120, LocalDate.of(2026, 6, 1)));

		ArgumentCaptor<DailyPlan> captor = ArgumentCaptor.forClass(DailyPlan.class);
		verify(dailyPlanRepository).save(captor.capture());
		assertThat(captor.getValue().getOwner()).isSameAs(owner);
		assertThat(captor.getValue().getPlanDate()).isEqualTo(LocalDate.of(2026, 6, 1));
		assertThat(captor.getValue().getItems()).hasSize(1);
		assertThat(response).isNotNull();
	}

	@Test
	void generate_whenRankingValidationFails_doesNotSave() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		Task task = new Task();
		ReflectionTestUtils.setField(task, "id", 1L);
		task.setTitle("Continue work");
		task.setPriority(TaskPriority.HIGH);
		task.setStatus(TaskStatus.IN_PROGRESS);
		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of(task));
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenReturn(new AiDailyPlanResponse(List.of()));

		assertThatThrownBy(
						() ->
								dailyPlanService.generate(
										new GeneratePlanRequest(60, LocalDate.of(2026, 6, 1))))
				.isInstanceOf(AiProviderException.class);

		verify(dailyPlanRepository, never()).save(any());
	}

	@Test
	void generate_whenAiReturnsUnknownTaskId_throwsAiProviderException() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		Task task = new Task();
		task.setTitle("Task 1");
		task.setPriority(TaskPriority.HIGH);
		task.setStatus(TaskStatus.OPEN);
		when(taskQueryService.findPlannableTasksByOwnerId(42L)).thenReturn(List.of(task));
		when(aiClient.generate(any(AiDailyPlanRequest.class)))
				.thenReturn(new AiDailyPlanResponse(List.of(new AiPlanItem(999L, 1))));

		assertThatThrownBy(
						() ->
								dailyPlanService.generate(
										new GeneratePlanRequest(60, LocalDate.of(2026, 6, 1))))
				.isInstanceOf(AiProviderException.class);

		verify(dailyPlanRepository, never()).save(any());
	}

	@Test
	void listForCurrentUser_returnsPagedSummaries() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		DailyPlanSummaryProjection projection =
				new DailyPlanSummaryProjection() {
					@Override
					public Long getId() {
						return 1L;
					}

					@Override
					public LocalDate getPlanDate() {
						return LocalDate.of(2026, 6, 1);
					}

					@Override
					public Instant getCreatedAt() {
						return Instant.parse("2026-06-01T09:00:00Z");
					}

					@Override
					public Integer getAvailableMinutes() {
						return 30;
					}

					@Override
					public Boolean getHasWarning() {
						return true;
					}

					@Override
					public Integer getItemCount() {
						return 2;
					}
				};
		when(dailyPlanRepository.findSummariesByOwner(eq(42L), eq(PageRequest.of(0, 20))))
				.thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 20), 1));

		PageResponse<DailyPlanSummaryResponse> response = dailyPlanService.listForCurrentUser(0, 20);

		assertThat(response.content())
				.singleElement()
				.satisfies(
						summary -> {
							assertThat(summary.id()).isEqualTo(1L);
							assertThat(summary.planDate()).isEqualTo(LocalDate.of(2026, 6, 1));
							assertThat(summary.itemCount()).isEqualTo(2);
							assertThat(summary.hasWarning()).isTrue();
							assertThat(summary.availableMinutes()).isEqualTo(30);
						});
		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.totalElements()).isEqualTo(1L);
		assertThat(response.totalPages()).isEqualTo(1);
	}

	@Test
	void listForCurrentUser_whenSizeExceedsMax_throwsBadRequestException() {
		assertThatThrownBy(() -> dailyPlanService.listForCurrentUser(0, 101))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("size must be between 1 and 100");
	}

	@Test
	void latestForCurrentUser_returnsNewestPlanForDate() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		LocalDate planDate = LocalDate.of(2026, 6, 1);
		DailyPlan newer = new DailyPlan();
		ReflectionTestUtils.setField(newer, "id", 2L);
		newer.setPlanDate(planDate);
		newer.setCreatedAt(Instant.parse("2026-06-01T14:00:00Z"));

		when(dailyPlanRepository.findFirstByOwner_IdAndPlanDateOrderByCreatedAtDescIdDesc(
						42L, planDate))
				.thenReturn(Optional.of(newer));

		Optional<DailyPlanResponse> response = dailyPlanService.latestForCurrentUser(planDate);

		assertThat(response)
				.isPresent()
				.get()
				.satisfies(plan -> assertThat(plan.id()).isEqualTo(2L));
	}

	@Test
	void latestForCurrentUser_whenNoneExist_returnsEmpty() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		when(dailyPlanRepository.findFirstByOwner_IdAndPlanDateOrderByCreatedAtDescIdDesc(
						42L, LocalDate.of(2026, 6, 1)))
				.thenReturn(Optional.empty());

		assertThat(dailyPlanService.latestForCurrentUser(LocalDate.of(2026, 6, 1))).isEmpty();
	}

	@Test
	void latestForCurrentUser_whenPlanDateMissing_throwsBadRequestException() {
		assertThatThrownBy(() -> dailyPlanService.latestForCurrentUser(null))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("planDate is required");
	}

	@Test
	void listForCurrentUser_mapsAvailableMinutesAndWarningFromProjection() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		DailyPlanSummaryProjection projection =
				new DailyPlanSummaryProjection() {
					@Override
					public Long getId() {
						return 1L;
					}

					@Override
					public LocalDate getPlanDate() {
						return LocalDate.of(2026, 6, 1);
					}

					@Override
					public Instant getCreatedAt() {
						return Instant.parse("2026-06-01T09:00:00Z");
					}

					@Override
					public Integer getAvailableMinutes() {
						return 30;
					}

					@Override
					public Boolean getHasWarning() {
						return true;
					}

					@Override
					public Integer getItemCount() {
						return 0;
					}
				};
		when(dailyPlanRepository.findSummariesByOwner(eq(42L), eq(PageRequest.of(0, 20))))
				.thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 20), 1));

		PageResponse<DailyPlanSummaryResponse> response = dailyPlanService.listForCurrentUser(0, 20);

		assertThat(response.content()).singleElement().satisfies(summary -> {
			assertThat(summary.availableMinutes()).isEqualTo(30);
			assertThat(summary.hasWarning()).isTrue();
		});
	}

	@Test
	void getForCurrentUser_whenOldPlanHasNullMinutesAndWarning_mapsNulls() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		DailyPlan plan = new DailyPlan();
		plan.setPlanDate(LocalDate.of(2026, 6, 1));
		plan.setCreatedAt(Instant.parse("2026-06-01T09:00:00Z"));
		when(dailyPlanRepository.findByOwner_IdAndId(42L, 7L)).thenReturn(Optional.of(plan));

		DailyPlanResponse response = dailyPlanService.getForCurrentUser(7L);

		assertThat(response.availableMinutes()).isNull();
		assertThat(response.warning()).isNull();
	}

	@Test
	void getForCurrentUser_whenOwnedPlanExists_returnsMappedResponse() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		DailyPlan plan = new DailyPlan();
		plan.setPlanDate(LocalDate.of(2026, 6, 1));
		plan.setCreatedAt(Instant.parse("2026-06-01T09:00:00Z"));
		when(dailyPlanRepository.findByOwner_IdAndId(42L, 7L)).thenReturn(Optional.of(plan));

		DailyPlanResponse response = dailyPlanService.getForCurrentUser(7L);

		verify(dailyPlanRepository).findByOwner_IdAndId(eq(42L), eq(7L));
		assertThat(response.planDate()).isEqualTo(LocalDate.of(2026, 6, 1));
		assertThat(response.items()).isEmpty();
	}

	@Test
	void getForCurrentUser_whenMissing_throwsNotFoundException() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));
		when(dailyPlanRepository.findByOwner_IdAndId(42L, 99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> dailyPlanService.getForCurrentUser(99L))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("daily plan not found");
	}

	@Test
	void deleteForCurrentUser_whenOwned_deletesPlan() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));

		DailyPlan plan = new DailyPlan();
		plan.setPlanDate(LocalDate.of(2026, 6, 1));
		when(dailyPlanRepository.findByOwner_IdAndId(42L, 7L)).thenReturn(Optional.of(plan));

		dailyPlanService.deleteForCurrentUser(7L);

		verify(dailyPlanRepository).delete(plan);
	}

	@Test
	void deleteForCurrentUser_whenMissing_throwsNotFoundException() {
		when(currentUser.getCurrentUser())
				.thenReturn(new UserContext(42L, "user@example.com", "user"));
		when(dailyPlanRepository.findByOwner_IdAndId(42L, 99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> dailyPlanService.deleteForCurrentUser(99L))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("daily plan not found");

		verify(dailyPlanRepository, never()).delete(any(DailyPlan.class));
	}
}
