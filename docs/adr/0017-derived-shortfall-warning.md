# The shortfall warning is derived from unplaced must-include rows

1.0.2 stored a JSON snapshot beside items that could drift from those items. Unplaced work is now real rows with `NO_ESTIMATE` or `OUT_OF_TIME`, so the warning mapper reads must-include unplaced items plus the plan's `requiredMinutes` and `capacityMinutes`. The `warning` jsonb column is dropped. Optional unplaced work does not warn. A persisted `hasWarning` bit is allowed on the summary projection only as a cache of that same predicate.
