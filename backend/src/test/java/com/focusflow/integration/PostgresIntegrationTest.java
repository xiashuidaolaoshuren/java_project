package com.focusflow.integration;

import static com.focusflow.testsupport.PersistenceFixtures.savedPlan;
import static com.focusflow.testsupport.PersistenceFixtures.savedTask;
import static com.focusflow.testsupport.PersistenceFixtures.savedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.focusflow.plan.DailyPlan;
import com.focusflow.plan.DailyPlanItem;
import com.focusflow.plan.DailyPlanRepository;
import com.focusflow.task.Task;
import com.focusflow.task.TaskPriority;
import com.focusflow.task.TaskRepository;
import com.focusflow.task.TaskStatus;
import com.focusflow.testsupport.DailyPlanTestBuilder;
import com.focusflow.testsupport.TaskTestBuilder;
import com.focusflow.testsupport.UserTestBuilder;
import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PostgresIntegrationTest {

	static {
		if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
			System.setProperty(
					"docker.client.strategy",
					"org.testcontainers.dockerclient.NpipeSocketClientProviderStrategy");
		}
	}

	@Container
	static final PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:16-alpine")
					.withDatabaseName("focusflow")
					.withUsername("focusflow")
					.withPassword("focusflow");

	@DynamicPropertySource
	static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
	}

	@Autowired UserRepository userRepository;

	@Autowired TaskRepository taskRepository;

	@Autowired DailyPlanRepository dailyPlanRepository;

	@Test
	void persistsUser_andFindsByEmail() {
		User user =
				UserTestBuilder.user()
						.withAccountPrefix("alice")
						.withPasswordHash("$2a$10$hashedPlaceholderForBcryptLater")
						.build();

		User saved = userRepository.save(user);

		assertThat(saved.getId()).isNotNull();
		assertThat(userRepository.findByEmail(user.getEmail()))
				.isPresent()
				.get()
				.satisfies(found -> {
					assertThat(found.getId()).isEqualTo(saved.getId());
					assertThat(found.getUsername()).isEqualTo(user.getUsername());
					assertThat(found.getPasswordHash()).isEqualTo(user.getPasswordHash());
				});
	}

	@Test
	void duplicateEmailViolatesUniqueness() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String email = "dup-" + suffix + "@example.com";

		User first =
				UserTestBuilder.user()
						.withUnique(suffix)
						.withEmail(email)
						.withUsername("user-a-" + suffix)
						.withPasswordHash("$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
						.build();
		userRepository.saveAndFlush(first);

		User second =
				UserTestBuilder.user()
						.withEmail(email)
						.withUsername("user-b-" + suffix)
						.withPasswordHash("$2a$10$bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
						.build();

		assertThatThrownBy(() -> userRepository.saveAndFlush(second))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void persistsTask_andFindsByOwnerAndId() {
		User savedOwner =
				savedUser(
						userRepository,
						UserTestBuilder.user()
								.withAccountPrefix("owner-task")
								.withPasswordHash("$2a$10$ccccccccccccccccccccccccccccccccccccccccccccccccccccccc"));

		Task saved =
				savedTask(
						taskRepository,
						TaskTestBuilder.task(savedOwner)
								.withTitle("Write integration tests")
								.withDescription("Cover owner-scoped persistence")
								.withPriority(TaskPriority.HIGH)
								.withStatus(TaskStatus.OPEN)
								.withDueDate(LocalDate.of(2026, 6, 1))
								.withEstimatedMinutes(45));

		assertThat(saved.getId()).isNotNull();
		assertThat(taskRepository.findByOwner_IdAndId(savedOwner.getId(), saved.getId()))
				.isPresent()
				.get()
				.satisfies(found -> {
					assertThat(found.getTitle()).isEqualTo("Write integration tests");
					assertThat(found.getOwner().getId()).isEqualTo(savedOwner.getId());
					assertThat(found.getPriority()).isEqualTo(TaskPriority.HIGH);
					assertThat(found.getStatus()).isEqualTo(TaskStatus.OPEN);
				});
	}

	@Test
	void taskQueriesAreOwnerScoped_forStatusAndDueDate() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);

		User ownerA =
				savedUser(
						userRepository,
						UserTestBuilder.user()
								.withUnique(suffix)
								.withAccountPrefix("owner-a")
								.withPasswordHash("$2a$10$ddddddddddddddddddddddddddddddddddddddddddddddddddddddd"));

		User ownerB =
				savedUser(
						userRepository,
						UserTestBuilder.user()
								.withUnique(suffix)
								.withAccountPrefix("owner-b")
								.withPasswordHash("$2a$10$eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"));

		Task taskLater =
				TaskTestBuilder.task(ownerA)
						.withTitle("Later")
						.withPriority(TaskPriority.MEDIUM)
						.withStatus(TaskStatus.OPEN)
						.withDueDate(LocalDate.of(2026, 6, 10))
						.build();

		Task taskSooner =
				TaskTestBuilder.task(ownerA)
						.withTitle("Sooner")
						.withPriority(TaskPriority.LOW)
						.withStatus(TaskStatus.OPEN)
						.withDueDate(LocalDate.of(2026, 6, 5))
						.build();

		Task taskB =
				TaskTestBuilder.task(ownerB)
						.withTitle("Other user")
						.withPriority(TaskPriority.HIGH)
						.withStatus(TaskStatus.OPEN)
						.withDueDate(LocalDate.of(2026, 6, 1))
						.build();

		taskLater = taskRepository.save(taskLater);
		taskRepository.save(taskSooner);
		taskRepository.save(taskB);
		taskRepository.flush();

		assertThat(taskRepository.findByOwner_IdAndId(ownerB.getId(), taskLater.getId())).isEmpty();

		assertThat(taskRepository.findByOwner_IdAndStatusOrderByDueDateAsc(ownerA.getId(), TaskStatus.OPEN))
				.extracting(Task::getTitle)
				.containsExactly("Sooner", "Later");

		assertThat(taskRepository.findByOwner_IdAndDueDateBetween(
						ownerB.getId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 30)))
				.singleElement()
				.satisfies(t -> assertThat(t.getTitle()).isEqualTo("Other user"));
	}

	@Test
	void persistsMultipleDailyPlans_sameOwnerAndDate_orderedByCreatedAtDesc() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		User owner =
				savedUser(
						userRepository,
						UserTestBuilder.user()
								.withUnique(suffix)
								.withAccountPrefix("plan-owner")
								.withPasswordHash("$2a$10$fffffffffffffffffffffffffffffffffffffffffffffffffffffff"));

		LocalDate planDate = LocalDate.of(2026, 8, 15);

		DailyPlan savedEarlier =
				dailyPlanRepository.save(
						DailyPlanTestBuilder.plan(owner, planDate)
								.withCreatedAt(Instant.parse("2026-08-15T08:00:00Z"))
								.build());

		DailyPlan savedLater =
				dailyPlanRepository.save(
						DailyPlanTestBuilder.plan(owner, planDate)
								.withCreatedAt(Instant.parse("2026-08-15T14:00:00Z"))
								.build());
		dailyPlanRepository.flush();

		assertThat(dailyPlanRepository.findByOwner_IdAndPlanDateOrderByCreatedAtDesc(owner.getId(), planDate))
				.extracting(DailyPlan::getId)
				.containsExactly(savedLater.getId(), savedEarlier.getId());

		assertThat(dailyPlanRepository.findByOwner_IdAndId(owner.getId(), savedEarlier.getId()))
				.isPresent()
				.get()
				.satisfies(p -> assertThat(p.getPlanDate()).isEqualTo(planDate));

		assertThat(dailyPlanRepository.findAllByOwner_IdOrderByCreatedAtDesc(owner.getId()))
				.extracting(DailyPlan::getId)
				.startsWith(savedLater.getId(), savedEarlier.getId());
	}

	@Test
	void dailyPlanQueriesAreOwnerScoped_itemsPreserveTaskFkAndOrder() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);

		User ownerA =
				savedUser(
						userRepository,
						UserTestBuilder.user()
								.withUnique(suffix)
								.withAccountPrefix("plan-a")
								.withPasswordHash("$2a$10$1111111111111111111111111111111111111111111111111111111"));

		User ownerB =
				savedUser(
						userRepository,
						UserTestBuilder.user()
								.withUnique(suffix)
								.withAccountPrefix("plan-b")
								.withPasswordHash("$2a$10$2222222222222222222222222222222222222222222222222222222"));

		final Task savedFirst =
				savedTask(
						taskRepository,
						TaskTestBuilder.task(ownerA)
								.withTitle("First task")
								.withPriority(TaskPriority.MEDIUM)
								.withStatus(TaskStatus.OPEN));

		final Task savedSecond =
				savedTask(
						taskRepository,
						TaskTestBuilder.task(ownerA)
								.withTitle("Second task")
								.withPriority(TaskPriority.MEDIUM)
								.withStatus(TaskStatus.OPEN));

		DailyPlan plan =
				savedPlan(
						dailyPlanRepository,
						DailyPlanTestBuilder.plan(ownerA, LocalDate.of(2026, 9, 20))
								.withCreatedAt(Instant.parse("2026-09-20T11:00:00Z"))
								.addItem(savedSecond, 0)
								.addItem(savedFirst, 1));
		dailyPlanRepository.flush();

		assertThat(dailyPlanRepository.findByOwner_IdAndId(ownerB.getId(), plan.getId())).isEmpty();

		assertThat(dailyPlanRepository.findByOwner_IdAndId(ownerA.getId(), plan.getId()))
				.isPresent()
				.get()
				.satisfies(
						p -> {
							assertThat(p.getItems())
									.extracting(DailyPlanItem::getPosition)
									.containsExactly(0, 1);
							assertThat(p.getItems())
									.extracting(i -> i.getTask().getId())
									.containsExactly(savedSecond.getId(), savedFirst.getId());
							assertThat(p.getItems())
									.extracting(i -> i.getTask().getTitle())
									.containsExactly("Second task", "First task");
						});
	}

	@Test
	void listDailyPlans_initializesItemsAndNestedTasks() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		User owner =
				savedUser(
						userRepository,
						UserTestBuilder.user()
								.withUnique(suffix)
								.withAccountPrefix("list-plan-owner")
								.withPasswordHash(
										"$2a$10$3333333333333333333333333333333333333333333333333333333"));

		Task savedFirst =
				savedTask(
						taskRepository,
						TaskTestBuilder.task(owner)
								.withTitle("First task")
								.withPriority(TaskPriority.MEDIUM)
								.withStatus(TaskStatus.OPEN));

		Task savedSecond =
				savedTask(
						taskRepository,
						TaskTestBuilder.task(owner)
								.withTitle("Second task")
								.withPriority(TaskPriority.MEDIUM)
								.withStatus(TaskStatus.OPEN));

		LocalDate planDate = LocalDate.of(2026, 9, 21);
		DailyPlan plan =
				savedPlan(
						dailyPlanRepository,
						DailyPlanTestBuilder.plan(owner, planDate)
								.withCreatedAt(Instant.parse("2026-09-21T10:00:00Z"))
								.addItem(savedSecond, 0)
								.addItem(savedFirst, 1));
		dailyPlanRepository.flush();

		List<DailyPlan> allPlans =
				dailyPlanRepository.findAllByOwner_IdOrderByCreatedAtDesc(owner.getId());
		assertThat(allPlans).hasSize(1).singleElement().satisfies(
				p -> {
					assertThat(p.getId()).isEqualTo(plan.getId());
					assertThat(p.getItems())
							.extracting(DailyPlanItem::getPosition)
							.containsExactly(0, 1);
					assertThat(p.getItems())
							.extracting(i -> i.getTask().getTitle())
							.containsExactly("Second task", "First task");
				});

		List<DailyPlan> plansByDate =
				dailyPlanRepository.findByOwner_IdAndPlanDateOrderByCreatedAtDesc(
						owner.getId(), planDate);
		assertThat(plansByDate).hasSize(1).singleElement().satisfies(
				p -> {
					assertThat(p.getId()).isEqualTo(plan.getId());
					assertThat(p.getItems())
							.extracting(DailyPlanItem::getPosition)
							.containsExactly(0, 1);
					assertThat(p.getItems())
							.extracting(i -> i.getTask().getTitle())
							.containsExactly("Second task", "First task");
				});
	}
}
