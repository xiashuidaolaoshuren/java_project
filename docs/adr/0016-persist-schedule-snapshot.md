# A Daily plan stores the window it was scheduled against

Each saved plan copies `windowStart`, `windowEnd`, and the optional peak window from effective preferences at generate time, plus derived `capacityMinutes` and `requiredMinutes`. The rail is painted from that snapshot, not from live preferences, so editing 09:00–18:00 down to 10:00–16:00 does not rewrite yesterday. Recomputing from current preferences would be simpler and wrong.
