# FocusFlow

FocusFlow helps an individual turn owned tasks into realistic plans for focused work. This glossary defines the product language shared by planning, task management, and future scheduling features.

## Tasks

**Task**:
A user-owned unit of work with a status, priority, optional due date, and optional effort estimate.
_Avoid_: Plan item, work block

**Plannable task**:
A Task that may appear in a new Daily plan. Its status is Open or In progress.
_Avoid_: Active task, open task (when referring to both statuses)

**Must-continue work**:
Every plannable Task already In progress. It must appear before newly started work in a generated plan.

**Due/overdue work**:
Open work whose due date is on or before the Planning date. It must appear before Optional work.
_Avoid_: Due today

**Optional work**:
Open work with no due date or a due date after the Planning date. A generated plan may omit it.

**Must-include work**:
Must-continue work together with Due/overdue work.

## Plans

**Daily plan**:
A user-owned plan generated for one Planning date from that user's Plannable tasks.
_Avoid_: Schedule (until timed scheduling is introduced)

**Planning date**:
The calendar date assigned to a Daily plan. Planning operations name it explicitly rather than relying on an unspecified “today.”
_Avoid_: Server date, today (at an API boundary)

**Plan detail**:
The complete view of one Daily plan, including its ordered work.

**Plan summary**:
The reduced view of a Daily plan used in plan history. It describes the plan without including its ordered work.

**Latest plan for a date**:
The most recently created Daily plan for a Planning date. Several plans may currently exist for the same date.
_Avoid_: Today's plan (when the date is not explicit)

**Generation snapshot**:
The Plannable task state used to make decisions for one plan-generation attempt. Later task edits do not retroactively change those decisions.

**Available focus minutes**:
The amount of focused work time the user offers to one Daily plan.
_Avoid_: Day length

**Leftover minutes**:
Available focus minutes minus known estimates for Must-include work, with a minimum of zero.
_Avoid_: Remaining effort

**Shortfall warning**:
A notice that known Must-include work exceeds Available focus minutes or that some Must-include work lacks an estimate.
