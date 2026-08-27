package com.focusflow.testsupport;

import java.util.Locale;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainerConfig {

	static {
		configureWindowsDockerStrategy();
	}

	private static final PostgreSQLContainer<?> CONTAINER =
			new PostgreSQLContainer<>("postgres:16-alpine")
					.withDatabaseName("focusflow")
					.withUsername("focusflow")
					.withPassword("focusflow");

	public static void configureWindowsDockerStrategy() {
		if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
			System.setProperty(
					"docker.client.strategy",
					"org.testcontainers.dockerclient.NpipeSocketClientProviderStrategy");
		}
	}

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		return CONTAINER;
	}
}
