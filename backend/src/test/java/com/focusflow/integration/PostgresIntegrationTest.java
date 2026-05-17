package com.focusflow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.focusflow.task.Task;
import com.focusflow.task.TaskPriority;
import com.focusflow.task.TaskRepository;
import com.focusflow.task.TaskStatus;
import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.context.SpringBootTest;
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

	@Test
	void persistsUser_andFindsByEmail() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		User user = new User();
		user.setEmail("alice-" + suffix + "@example.com");
		user.setUsername("alice-" + suffix);
		user.setPasswordHash("$2a$10$hashedPlaceholderForBcryptLater");

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

		User first = new User();
		first.setEmail(email);
		first.setUsername("user-a-" + suffix);
		first.setPasswordHash("$2a$10$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
		userRepository.saveAndFlush(first);

		User second = new User();
		second.setEmail(email);
		second.setUsername("user-b-" + suffix);
		second.setPasswordHash("$2a$10$bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");

		assertThatThrownBy(() -> userRepository.saveAndFlush(second))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void persistsTask_andFindsByOwnerAndId() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		User owner = new User();
		owner.setEmail("owner-task-" + suffix + "@example.com");
		owner.setUsername("owner-task-" + suffix);
		owner.setPasswordHash("$2a$10$ccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
		User savedOwner = userRepository.save(owner);

		Task task = new Task();
		task.setOwner(savedOwner);
		task.setTitle("Write integration tests");
		task.setDescription("Cover owner-scoped persistence");
		task.setPriority(TaskPriority.HIGH);
		task.setStatus(TaskStatus.OPEN);
		task.setDueDate(LocalDate.of(2026, 6, 1));
		task.setEstimatedMinutes(45);

		Task saved = taskRepository.save(task);

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

		User ownerA = new User();
		ownerA.setEmail("owner-a-" + suffix + "@example.com");
		ownerA.setUsername("owner-a-" + suffix);
		ownerA.setPasswordHash("$2a$10$ddddddddddddddddddddddddddddddddddddddddddddddddddddddd");
		ownerA = userRepository.save(ownerA);

		User ownerB = new User();
		ownerB.setEmail("owner-b-" + suffix + "@example.com");
		ownerB.setUsername("owner-b-" + suffix);
		ownerB.setPasswordHash("$2a$10$eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
		ownerB = userRepository.save(ownerB);

		Task taskLater = new Task();
		taskLater.setOwner(ownerA);
		taskLater.setTitle("Later");
		taskLater.setPriority(TaskPriority.MEDIUM);
		taskLater.setStatus(TaskStatus.OPEN);
		taskLater.setDueDate(LocalDate.of(2026, 6, 10));

		Task taskSooner = new Task();
		taskSooner.setOwner(ownerA);
		taskSooner.setTitle("Sooner");
		taskSooner.setPriority(TaskPriority.LOW);
		taskSooner.setStatus(TaskStatus.OPEN);
		taskSooner.setDueDate(LocalDate.of(2026, 6, 5));

		Task taskB = new Task();
		taskB.setOwner(ownerB);
		taskB.setTitle("Other user");
		taskB.setPriority(TaskPriority.HIGH);
		taskB.setStatus(TaskStatus.OPEN);
		taskB.setDueDate(LocalDate.of(2026, 6, 1));

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
}
