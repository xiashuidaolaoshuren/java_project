package com.focusflow.schema;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;

public final class LegacySchemaAdoptionCommand {

	private LegacySchemaAdoptionCommand() {}

	public static void adopt(String jdbcUrl, String username, String password) {
		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
			if (flywayHistoryPresent(connection)) {
				throw new IllegalStateException(
						"Flyway schema history already exists; legacy adoption is one-shot only");
			}

			new LegacySchemaAdoptionPreflight().check(connection);
		} catch (LegacySchemaMismatchException exception) {
			throw exception;
		} catch (SQLException exception) {
			throw new LegacySchemaMismatchException(
					"Failed to inspect schema: " + exception.getMessage());
		}

		Flyway.configure()
				.dataSource(jdbcUrl, username, password)
				.baselineVersion("1")
				.load()
				.baseline();
	}

	public static void main(String[] args) {
		String jdbcUrl =
				envOrDefault(
						"SPRING_DATASOURCE_URL", "jdbc:postgresql://127.0.0.1:5432/focusflow");
		String username = envOrDefault("SPRING_DATASOURCE_USERNAME", "focusflow");
		String password = envOrDefault("SPRING_DATASOURCE_PASSWORD", "focusflow");

		try {
			adopt(jdbcUrl, username, password);
			System.out.println("Legacy schema adopted at Flyway version 1.");
		} catch (RuntimeException exception) {
			System.err.println("Legacy schema adoption failed: " + exception.getMessage());
			System.exit(1);
		}
	}

	private static boolean flywayHistoryPresent(Connection connection) throws SQLException {
		try (PreparedStatement statement =
						connection.prepareStatement(
								"SELECT to_regclass('public.flyway_schema_history') IS NOT NULL AS present");
				ResultSet resultSet = statement.executeQuery()) {
			resultSet.next();
			return resultSet.getBoolean("present");
		}
	}

	private static String envOrDefault(String name, String defaultValue) {
		String value = System.getenv(name);
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		return value;
	}
}
