# Require positive estimates and use long aggregate totals

Task `estimatedMinutes` is either absent or a positive Integer. Schedule-wide totals such as `requiredMinutes` use Java `long` and PostgreSQL `bigint`, while free and scheduled clock minutes remain bounded day integers. Rejecting non-positive estimates prevents backward or zero-duration scheduling; long totals prevent overflow without imposing a one-day or arbitrary one-year cap that would conflict with future multi-day tasks.
