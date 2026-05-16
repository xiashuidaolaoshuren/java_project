package com.focusflow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.focusflow.user.User;
import com.focusflow.user.UserRepository;
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
}
