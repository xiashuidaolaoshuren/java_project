# FocusFlow Roadmap — Enterprise-Grade Planning Iteration

Last updated: 2026-08-26

This tracks a multi-milestone iteration aimed at making FocusFlow's planning features closer to enterprise-grade: real scheduling on a clock, rest-aware planning, work that spans days, and the platform hardening needed to carry it. It exists so the milestones after the one currently in progress do not get lost between brainstorming sessions.

**How to use this doc:** Before brainstorming the next milestone, read its section below for the scope and decisions already settled. Brainstorming that milestone may refine or override anything here — treat this as a starting point, not a contract. After a milestone's spec is written, update this doc's status table and fold any new cross-milestone decisions back in here.

## Status

| Milestone | Status | Spec |
|---|---|---|
| 1.1.0 — Platform hardening | Spec written, awaiting review. Not yet built. | [`2026-08-26-focusflow-1.1.0-platform-hardening-design.md`](specs/2026-08-26-focusflow-1.1.0-platform-hardening-design.md) |
| 1.2.0 — The scheduled day | Not started. Scope outlined below. | — |
| 1.3.0 — Progress and carry-over | Not started. Scope outlined below. | — |
| 1.4.0 — Dependencies and the multi-day horizon | Not started. Scope outlined below. | — |

Deferred items with no milestone assigned are listed at the end.

## Cross-milestone decisions already settled

These were decided during the brainstorming session that produced this roadmap. They constrain 1.2.0 and 1.3.0 design and should be treated as settled unless something learned while building changes them.

- **Time model:** plan blocks get persisted, real clock start/end times, not just relative duration or position. A plan has a day window (e.g. 09:00–18:00).
- **Planning horizon:** this iteration plans one day at a time with clock times, breaks, and carry-over. A multi-day Gantt where long tasks span dates is a separate, later milestone (1.4.0) built on top of the single-day scheduler, not a replacement for it.
- **Who schedules:** the AI keeps producing a priority ordering only, exactly as it does today. A new deterministic Java scheduler takes that ordering plus the work window, break rules, and remaining effort, and places blocks on the clock, inserts breaks, and splits sessions. Correctness stops depending on model arithmetic. `DailyPlanRankingValidator` narrows from policing a full schedule to policing an ordering.
- **Progress tracking:** actuals are recorded against plan blocks (done / partly done / skipped, with optional actual minutes), not via a live start/pause/complete timer and not via a manual remaining-minutes field on Task. Remaining effort for carry-over is derived from block actuals. This has a clean upgrade path to a live timer later without changing the underlying model.
- **Rest model:** a focus cadence (maximum continuous focus stretch plus a break length) combined with fixed-time breaks (e.g. lunch 12:00–13:00) that the scheduler treats as unavailable time. Fixed-time commitments (meetings) use the same "unavailable time" mechanism.

## 1.2.0 — The scheduled day

A plan stops being an ordered list and becomes a schedule.

- Persisted user preferences: work window (day start/end), focus cadence, break length, fixed breaks, peak/energy hours.
- `DailyPlanItem` becomes a timed block with a start and end, and can represent work or rest.
- Deterministic Java scheduler: places must-include work first (reusing 1.0.2's must-continue / due-overdue / optional blocks), inserts cadence breaks and fixed breaks, honors fixed-time commitments and buffer time, and places work with respect to peak-hour preferences where feasible.
- Fixed-time commitments: a meeting or appointment pinned to a specific time window that the scheduler plans around, using the same unavailable-time mechanism as a fixed break.
- Buffer time: a reserved slice of the day held back as contingency.
- Settles **plan identity** — whether generating a new plan for a date replaces, versions, or keeps accumulating rows for that date (1.0.2 currently just accumulates and takes latest-by-createdAt). This decision was deferred from this brainstorming session and needs its own discussion when 1.2.0 is brainstormed.
- Day timeline UI (the Gantt-chart-like view the user asked for), rendering blocks against the day window.
- `DailyPlanRankingValidator` narrows to validating ordering only; a new scheduler-level validator (or the scheduler's own correctness, since it's deterministic Java) replaces the old block-order and leftover-minutes checks that assumed no clock.

## 1.3.0 — Progress and carry-over

Builds on 1.2.0's scheduler without changing it — only what "how much effort is left" means, changes.

- Record actuals against blocks: done, partly done (with actual minutes), or skipped.
- Derive remaining effort for a task from its block actuals across plans, replacing the whole-task `estimatedMinutes` as the only signal.
- Carry unfinished work into a later day's plan automatically.
- Re-plan the rest of a day: if it's 14:00 and the user is behind schedule, reschedule remaining blocks into the hours left, reusing the 1.2.0 scheduler with a shrunk window and adjusted remaining effort.
- Deadline feasibility warnings: extend the 1.0.2 shortfall-warning pattern to say a task will miss its due date given current pace, now that real remaining-effort data exists to make that claim honestly (it would be a rough estimate-only guess in 1.2.0, and accurate once actuals exist here).
- Timeline shows planned-vs-actual for a day.

## 1.4.0 — Dependencies and the multi-day horizon

- Task dependency graph: task B cannot start until task A finishes. Needs cycle detection at edit time, an API to add/remove links, and UI for setting them.
- Topological ordering layered over the AI's priority ranking — dependencies constrain ordering, not placement, so the 1.2.0/1.3.0 scheduler does not need to change to accommodate them.
- Needs an explicit rule for how a dependency interacts with 1.0.2's must-include rules (e.g. a task due today that is blocked by unfinished work — this collision was flagged during brainstorming and not yet resolved).
- Multi-day Gantt: dates across the top, task bars spanning multiple days, long tasks split into sessions planned across a horizon (e.g. a week) with per-day capacity, and re-planning the horizon when a day goes off track.
- This is where dependencies' cross-date ripple effects (a slipped blocker pushing every dependent task later) actually pay off, which is why they're grouped with the horizon work rather than built earlier.

## Deferred, no milestone assigned

Recorded so they aren't reconsidered from scratch, and so a future brainstorming session can slot them in deliberately rather than by default.

- **Recurring tasks** (daily/weekly repeats) — largely a separate subsystem for generating task instances; barely touches the scheduler, so it can slot into any milestone.
- **Subtasks and task hierarchy** (parent tasks with children, rolled-up estimates and progress) — a significant change to the `Task` aggregate and every existing query; large enough to deserve its own milestone if pursued.
- **Audit timestamps and optimistic locking** (`createdAt`/`updatedAt` via JPA auditing, `@Version` where concurrent edits are plausible) — small, low-risk backend improvement, considered during 1.1.0 platform hardening and cut for scope.
- **Generate rate limiting** (per-user cap on plan generation to bound provider cost) — considered during 1.1.0 and cut for scope.
- **Task list pagination** (`GET /api/tasks`) — 1.1.0 paginates plan history only; the task list was left unpaginated deliberately since it's typically much smaller.
- **Structured JSON logging** — deferred from 1.1.0 observability work; needs another dependency and makes local development harder to read without a log viewer.

## Related specs

- [Parent spec](specs/2026-05-14-focusflow-ai-design.md) — original product and architecture design.
- [1.0.2 design](specs/2026-08-22-focusflow-1.0.2-design.md) — plan delete, must-include ranking rules, shortfall warning; the milestone this roadmap builds on.
- [Frontend UI design](specs/2026-05-31-focusflow-frontend-ui-design.md) — confirmation, toast, and alert patterns reused by later milestones.
