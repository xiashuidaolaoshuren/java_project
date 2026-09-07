# One Daily plan per owner and date

A Daily plan is now a schedule for a calendar day, not one of several ranking suggestions. FocusFlow enforces a unique `(owner, planning date)` and treating generate as replace, so history is one row per date and `GET /api/daily-plans/latest` is simply the plan for that date. Accumulating rows (1.0.2) made sense for disposable orderings; versioning would preserve attempts but complicates 1.3.0's actuals. Regenerating in 1.2.0 confirms first because the plan is still disposable; 1.3.0 must refuse or merge once actuals exist.
