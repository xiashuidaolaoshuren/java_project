# Adopt legacy schemas explicitly

FocusFlow disables Flyway's automatic baseline-on-migrate behavior during normal startup. A pre-Flyway 1.0.2 database may be recorded as V1 only through an explicit, one-time procedure after a backup and structural preflight succeed; fresh databases run V1 normally. This adds an operator step, but prevents an unexpected non-empty database from being silently accepted even though Hibernate validation cannot prove that every constraint and index matches the canonical schema.
