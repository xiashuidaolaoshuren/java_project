# FocusFlow

FocusFlow helps an individual turn owned tasks into a realistic clock schedule for focused work. This glossary defines the product language shared by planning, task management, and scheduling.

## Tasks

**Task**:
A user-owned unit of work with a status, priority, optional due date, and optional effort estimate.
_Avoid_: Plan item, work block, commitment

**Plannable task**:
A Task that may appear in a new Daily plan. Its status is Open or In progress.
_Avoid_: Active task, open task (when referring to both statuses)

**Must-continue work**:
Every plannable Task already In progress. It must appear before newly started work in the ranking.

**Due/overdue work**:
Open work whose due date is on or before the Planning date. It must appear before Optional work in the ranking.
_Avoid_: Due today

**Optional work**:
Open work with no due date or a due date after the Planning date. The scheduler may leave it unplaced when the day is full.

**Must-include work**:
Must-continue work together with Due/overdue work.

## Time

**Work window**:
The wall-clock span of one planning day, from a start time to an end time.
_Avoid_: Available minutes, day length, available focus minutes

**Focus cadence**:
A maximum continuous focus stretch together with a cadence-break length.

**Minimum session**:
The shortest work block the scheduler emits as its own session, unless a leftover fragment is all that remains.

**Fixed break**:
A labelled unavailable window that repeats every planning day, stored with Scheduling preferences.
_Avoid_: Lunch (when meaning the general concept)

**Commitment**:
A labelled unavailable window on one calendar date. It is not a Task.

**Trailing buffer**:
The last slice of the Work window reserved as contingency and shown as its own block.

**Peak window**:
An optional wall-clock span shaded on the timeline. It does not change placement.

**Wall-clock time**:
A time of day without a timezone. Nine o'clock means 09:00 on the Planning date.
_Avoid_: Instant, UTC, local now (at an API boundary)

## Plans

**Daily plan**:
The single user-owned schedule generated for one Planning date.
_Avoid_: Latest plan for a date, schedule (as a second noun for the same aggregate)

**Planning date**:
The calendar date assigned to a Daily plan. Planning operations name it explicitly rather than relying on an unspecified “today.”
_Avoid_: Server date, today (at an API boundary)

**Plan detail**:
The complete view of one Daily plan, including its Timed blocks.

**Plan summary**:
The reduced view of a Daily plan used in plan history. It describes the plan without including its Timed blocks.

**The plan for a date**:
The one Daily plan for an owner and Planning date. Generating again replaces it.
_Avoid_: Latest plan for a date

**Generation snapshot**:
The Plannable task state, effective Scheduling preferences, and Commitments used to make decisions for one plan-generation attempt. Later edits do not retroactively change those decisions.

**Schedule snapshot**:
The Work window and Peak window copied onto a Daily plan at generate time so later preference edits do not rewrite history.

**Timed block**:
One item in a Daily plan: placed work, a cadence break, a Fixed break, a Commitment, the Trailing buffer, or Unplaced work.

**Work session**:
A placed Timed block of work for one Task. Several sessions of the same Task may exist in one Daily plan.

**Unplaced work**:
A Timed block that is in the Daily plan but not on the clock, because the Task has no estimate or because the day ran out of time.

**Day capacity**:
Minutes remaining in the Work window after Fixed breaks, overlapping Commitments, and the Trailing buffer are removed. Cadence breaks consume this during placement.

**Required minutes**:
The sum of known estimates on Must-include work at generate time.

**Shortfall warning**:
A notice that some Must-include work is Unplaced work.

## Preferences

**Scheduling preferences**:
The owner's Work window, Focus cadence, Minimum session, Trailing buffer, optional Peak window, and Fixed breaks. Missing preferences resolve to in-code defaults.
_Avoid_: Settings (when meaning the persisted aggregate)
