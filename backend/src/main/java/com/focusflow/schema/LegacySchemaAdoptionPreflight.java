package com.focusflow.schema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LegacySchemaAdoptionPreflight {

	private static final List<String> REQUIRED_TABLES =
			List.of("users", "tasks", "daily_plans", "daily_plan_items");

	private static final List<ExpectedColumn> REQUIRED_COLUMNS =
			List.of(
					new ExpectedColumn("users", "id", "bigint", false),
					new ExpectedColumn("users", "email", "character varying", 255, false),
					new ExpectedColumn("users", "username", "character varying", 255, false),
					new ExpectedColumn("users", "password_hash", "character varying", 255, false),
					new ExpectedColumn("tasks", "id", "bigint", false),
					new ExpectedColumn("tasks", "owner_id", "bigint", false),
					new ExpectedColumn("tasks", "title", "character varying", 255, false),
					new ExpectedColumn("tasks", "description", "text", true),
					new ExpectedColumn("tasks", "priority", "character varying", 32, false),
					new ExpectedColumn("tasks", "status", "character varying", 32, false),
					new ExpectedColumn("tasks", "due_date", "date", true),
					new ExpectedColumn("tasks", "estimated_minutes", "integer", true),
					new ExpectedColumn("daily_plans", "id", "bigint", false),
					new ExpectedColumn("daily_plans", "owner_id", "bigint", false),
					new ExpectedColumn("daily_plans", "plan_date", "date", false),
					new ExpectedColumn("daily_plans", "created_at", "timestamp with time zone", false),
					new ExpectedColumn("daily_plans", "available_minutes", "integer", true),
					new ExpectedColumn("daily_plans", "warning", "jsonb", true),
					new ExpectedColumn("daily_plan_items", "id", "bigint", false),
					new ExpectedColumn("daily_plan_items", "daily_plan_id", "bigint", false),
					new ExpectedColumn("daily_plan_items", "task_id", "bigint", false),
					new ExpectedColumn("daily_plan_items", "position", "integer", false));

	private static final List<ExpectedForeignKey> REQUIRED_FOREIGN_KEYS =
			List.of(
					new ExpectedForeignKey("tasks", "owner_id", "users", "id"),
					new ExpectedForeignKey("daily_plans", "owner_id", "users", "id"),
					new ExpectedForeignKey("daily_plan_items", "daily_plan_id", "daily_plans", "id"),
					new ExpectedForeignKey("daily_plan_items", "task_id", "tasks", "id"));

	private static final List<ExpectedUnique> REQUIRED_UNIQUES =
			List.of(
					new ExpectedUnique("users", "email"),
					new ExpectedUnique("users", "username"));

	private static final List<ExpectedIndex> REQUIRED_INDEXES =
			List.of(
					new ExpectedIndex("tasks", List.of("owner_id")),
					new ExpectedIndex("daily_plans", List.of("owner_id", "plan_date")),
					new ExpectedIndex("daily_plans", List.of("owner_id", "created_at", "id")),
					new ExpectedIndex("daily_plan_items", List.of("daily_plan_id")));

	public void check(Connection connection) {
		List<String> mismatches = new ArrayList<>();
		try {
			checkTables(connection, mismatches);
			checkColumns(connection, mismatches);
			checkUniqueColumns(connection, mismatches);
			checkForeignKeys(connection, mismatches);
			checkIndexes(connection, mismatches);
		} catch (SQLException exception) {
			throw new LegacySchemaMismatchException(
					"Failed to inspect schema: " + exception.getMessage());
		}

		if (!mismatches.isEmpty()) {
			throw new LegacySchemaMismatchException(String.join("; ", mismatches));
		}
	}

	private void checkTables(Connection connection, List<String> mismatches) throws SQLException {
		Set<String> existingTables = new HashSet<>();
		try (PreparedStatement statement =
						connection.prepareStatement(
								"""
								SELECT table_name
								FROM information_schema.tables
								WHERE table_schema = 'public'
								  AND table_type = 'BASE TABLE'
								""");
				ResultSet resultSet = statement.executeQuery()) {
			while (resultSet.next()) {
				existingTables.add(resultSet.getString("table_name"));
			}
		}

		for (String table : REQUIRED_TABLES) {
			if (!existingTables.contains(table)) {
				mismatches.add("missing table: " + table);
			}
		}
	}

	private void checkColumns(Connection connection, List<String> mismatches) throws SQLException {
		for (ExpectedColumn column : REQUIRED_COLUMNS) {
			try (PreparedStatement statement =
					connection.prepareStatement(
							"""
							SELECT data_type, character_maximum_length, is_nullable
							FROM information_schema.columns
							WHERE table_schema = 'public'
							  AND table_name = ?
							  AND column_name = ?
							""")) {
				statement.setString(1, column.table());
				statement.setString(2, column.column());
				try (ResultSet resultSet = statement.executeQuery()) {
					if (!resultSet.next()) {
						mismatches.add("missing column: " + column.table() + "." + column.column());
						continue;
					}

					String dataType = resultSet.getString("data_type").toLowerCase(Locale.ROOT);
					Integer maxLength = (Integer) resultSet.getObject("character_maximum_length");
					boolean nullable = "YES".equalsIgnoreCase(resultSet.getString("is_nullable"));

					if (!dataType.equals(column.dataType())) {
						mismatches.add(
								"wrong type for "
										+ column.table()
										+ "."
										+ column.column()
										+ ": expected "
										+ column.dataType()
										+ ", found "
										+ dataType);
					}

					if (column.maxLength() != null
							&& (maxLength == null || maxLength.intValue() != column.maxLength())) {
						mismatches.add(
								"wrong length for "
										+ column.table()
										+ "."
										+ column.column()
										+ ": expected "
										+ column.maxLength()
										+ ", found "
										+ maxLength);
					}

					if (nullable != column.nullable()) {
						mismatches.add(
								"wrong nullability for "
										+ column.table()
										+ "."
										+ column.column()
										+ ": expected "
										+ (column.nullable() ? "nullable" : "not null"));
					}
				}
			}
		}
	}

	private void checkUniqueColumns(Connection connection, List<String> mismatches)
			throws SQLException {
		Set<String> uniqueColumns = new HashSet<>();
		try (PreparedStatement statement =
				connection.prepareStatement(
						"""
						SELECT kcu.table_name, kcu.column_name
						FROM information_schema.table_constraints tc
						JOIN information_schema.key_column_usage kcu
						  ON tc.constraint_name = kcu.constraint_name
						 AND tc.table_schema = kcu.table_schema
						WHERE tc.table_schema = 'public'
						  AND tc.constraint_type = 'UNIQUE'
						""");
				ResultSet resultSet = statement.executeQuery()) {
			while (resultSet.next()) {
				uniqueColumns.add(resultSet.getString("table_name") + "." + resultSet.getString("column_name"));
			}
		}

		for (ExpectedUnique unique : REQUIRED_UNIQUES) {
			if (!uniqueColumns.contains(unique.table() + "." + unique.column())) {
				mismatches.add("missing unique constraint on " + unique.table() + "." + unique.column());
			}
		}
	}

	private void checkForeignKeys(Connection connection, List<String> mismatches) throws SQLException {
		Set<String> foreignKeys = new HashSet<>();
		try (PreparedStatement statement =
				connection.prepareStatement(
						"""
						SELECT
						  src.relname AS source_table,
						  src_att.attname AS source_column,
						  tgt.relname AS target_table,
						  tgt_att.attname AS target_column
						FROM pg_constraint con
						JOIN pg_class src ON src.oid = con.conrelid
						JOIN pg_namespace src_ns ON src_ns.oid = src.relnamespace
						JOIN pg_class tgt ON tgt.oid = con.confrelid
						JOIN pg_namespace tgt_ns ON tgt_ns.oid = tgt.relnamespace
						JOIN unnest(con.conkey) WITH ORDINALITY AS src_cols(attnum, ord) ON true
						JOIN pg_attribute src_att
						  ON src_att.attrelid = src.oid
						 AND src_att.attnum = src_cols.attnum
						JOIN unnest(con.confkey) WITH ORDINALITY AS tgt_cols(attnum, ord)
						  ON src_cols.ord = tgt_cols.ord
						JOIN pg_attribute tgt_att
						  ON tgt_att.attrelid = tgt.oid
						 AND tgt_att.attnum = tgt_cols.attnum
						WHERE con.contype = 'f'
						  AND src_ns.nspname = 'public'
						  AND tgt_ns.nspname = 'public'
						""");
				ResultSet resultSet = statement.executeQuery()) {
			while (resultSet.next()) {
				foreignKeys.add(
						resultSet.getString("source_table")
								+ "."
								+ resultSet.getString("source_column")
								+ "->"
								+ resultSet.getString("target_table")
								+ "."
								+ resultSet.getString("target_column"));
			}
		}

		for (ExpectedForeignKey foreignKey : REQUIRED_FOREIGN_KEYS) {
			String key =
					foreignKey.sourceTable()
							+ "."
							+ foreignKey.sourceColumn()
							+ "->"
							+ foreignKey.targetTable()
							+ "."
							+ foreignKey.targetColumn();
			if (!foreignKeys.contains(key)) {
				mismatches.add("missing foreign key: " + key);
			}
		}
	}

	private void checkIndexes(Connection connection, List<String> mismatches) throws SQLException {
		List<IndexDefinition> indexes = loadIndexes(connection);

		for (ExpectedIndex expectedIndex : REQUIRED_INDEXES) {
			boolean found =
					indexes.stream()
							.anyMatch(
									index ->
											index.table().equals(expectedIndex.table())
													&& index.columns().equals(expectedIndex.columns()));
			if (!found) {
				mismatches.add(
						"missing index on "
								+ expectedIndex.table()
								+ "("
								+ String.join(", ", expectedIndex.columns())
								+ ")");
			}
		}
	}

	private List<IndexDefinition> loadIndexes(Connection connection) throws SQLException {
		List<IndexDefinition> indexes = new ArrayList<>();
		try (PreparedStatement statement =
				connection.prepareStatement(
						"""
						SELECT
						  tbl.relname AS table_name,
						  array_agg(att.attname ORDER BY cols.ordinality) AS column_names
						FROM pg_index idx
						JOIN pg_class tbl ON tbl.oid = idx.indrelid
						JOIN pg_namespace ns ON ns.oid = tbl.relnamespace
						JOIN unnest(idx.indkey) WITH ORDINALITY AS cols(attnum, ordinality) ON true
						JOIN pg_attribute att
						  ON att.attrelid = tbl.oid
						 AND att.attnum = cols.attnum
						WHERE ns.nspname = 'public'
						  AND idx.indisprimary = false
						GROUP BY tbl.relname, idx.indexrelid
						""");
				ResultSet resultSet = statement.executeQuery()) {
			while (resultSet.next()) {
				String tableName = resultSet.getString("table_name");
				String[] columnNames = (String[]) resultSet.getArray("column_names").getArray();
				indexes.add(new IndexDefinition(tableName, Arrays.asList(columnNames)));
			}
		}
		return indexes;
	}

	private record ExpectedColumn(
			String table, String column, String dataType, Integer maxLength, boolean nullable) {

		ExpectedColumn(String table, String column, String dataType, boolean nullable) {
			this(table, column, dataType, null, nullable);
		}
	}

	private record ExpectedForeignKey(
			String sourceTable, String sourceColumn, String targetTable, String targetColumn) {}

	private record ExpectedUnique(String table, String column) {}

	private record ExpectedIndex(String table, List<String> columns) {}

	private record IndexDefinition(String table, List<String> columns) {}
}
