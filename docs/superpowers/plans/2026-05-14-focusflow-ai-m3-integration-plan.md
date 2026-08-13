# Implementation Plan: FocusFlow AI Milestone 3 - Integration And Polish

**Spec:** `docs/superpowers/specs/2026-05-14-focusflow-ai-design.md`  
**Created:** 2026-05-15  
**Milestone scope:** Full local integration, browser-to-backend session behavior, real OpenAI smoke check, documentation, and learning polish.

## Summary

Ship the MVP as a working local full-stack app. This milestone connects the Spring Boot backend from Milestone 1 and the React frontend from Milestone 2, verifies cookie/session auth between Vite and Spring Security, checks task and daily-plan workflows through the browser, runs a real OpenAI smoke test, and updates documentation so the project can be learned and rerun later.

Out of scope: production deployment, CI/CD, cloud database setup, real user monitoring, complex E2E automation, RAG, embeddings, agents, calendar integration, and notification systems.

## Discovery Notes

- Reuse: This milestone assumes Milestone 1 and Milestone 2 are complete enough to run locally.
- Constraints: Keep integration fixes narrow. If a backend or frontend contract is wrong, update the relevant milestone plan before changing broad architecture.
- Patterns to follow: Treat integration issues as contract issues first: API shape, cookies, CSRF, CORS/proxy, error payloads, and TypeScript types.
- Fix protocol: When a browser check reveals a backend bug, add or extend an automated backend test first, then re-run the manual browser check.
- Anti-goals: Do not add new major features during integration. Do not use integration work to introduce JWT, Redux, SSR, or deployment infrastructure.

## File Map

### Local Run And Documentation

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `README.md` | modify | Document complete setup, run commands, manual checks, and learning map. | Developer documentation. |
| `.env.example` | modify | Ensure all backend and frontend environment variables are documented. | Example environment contract. |
| `docker-compose.yml` | modify | Confirm local PostgreSQL setup works with backend defaults. | `postgres` service. |

### Backend Integration Touchpoints

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `backend/src/main/resources/application.yml` | modify | Align CORS, CSRF, sessions, datasource, and OpenAI config with local full-stack runtime. | Spring configuration keys. |
| `backend/src/main/java/com/focusflow/security/SecurityConfig.java` | modify | Finalize browser-facing session, CSRF, CORS/proxy, and logout behavior. | Security filter chain. |
| `backend/src/main/java/com/focusflow/auth/AuthController.java` | modify | Confirm current-user and auth responses match frontend expectations. | Auth API. |
| `backend/src/main/java/com/focusflow/task/TaskController.java` | modify | Confirm task payloads and error responses match frontend expectations. | Task API. |
| `backend/src/main/java/com/focusflow/plan/DailyPlanController.java` | modify | Confirm generation and saved-plan responses match frontend expectations. | Daily-plan API. |
| `backend/src/main/java/com/focusflow/ai/OpenAiDailyPlanClient.java` | modify | Tune real-provider behavior found during smoke testing. | OpenAI adapter. |

### Frontend Integration Touchpoints

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `frontend/vite.config.ts` | modify | Finalize local API proxy or backend origin settings. | Vite dev server behavior. |
| `frontend/src/lib/api.ts` | modify | Finalize credentials, CSRF, error normalization, and base URL behavior. | API helper functions. |
| `frontend/src/types/api.ts` | modify | Align TypeScript payloads with actual backend DTOs. | API contract types. |
| `frontend/src/features/auth/*` | modify | Verify browser session behavior across login, refresh, logout, and unauthenticated redirects. | Auth hooks/components. |
| `frontend/src/features/tasks/*` | modify | Verify task CRUD behavior against backend. | Task hooks/components. |
| `frontend/src/features/plans/*` | modify | Verify plan generation and saved-plan behavior against backend and provider errors. | Plan hooks/components. |

## Blast Radius

| Path | Why Sensitive | Plan Mode Before Implementation |
|------|---------------|---------------------------------|
| `backend/src/main/java/com/focusflow/security/SecurityConfig.java` | Small changes can break all authenticated browser requests. | high - confirm observed failure and desired Spring Security behavior before editing. |
| `backend/src/main/resources/application.yml` | Config changes can break local startup, sessions, database, or provider calls. | high - confirm active profile and runtime environment. |
| `frontend/src/lib/api.ts` | All browser-to-backend calls pass through this file. | high - confirm cookie, CSRF, error, and base URL behavior before editing. |
| `frontend/vite.config.ts` | Proxy/origin behavior affects cookies and CSRF. | high - confirm whether final local setup uses proxy or CORS. |
| `frontend/src/types/api.ts` | Contract drift can hide backend/frontend mismatch. | medium - update only to match actual backend DTOs. |
| `README.md` | Documentation becomes the user's repeatable learning path. | medium - keep commands accurate and beginner-friendly. |

## Workflow For Implementers

1. **writing-plans** produced this file (type-1 decomposition only).
2. Start with evidence: run the app, observe exact failures, then use Cursor **Plan mode** and the **planning-subtasks** skill for high-risk fixes (security, proxy, API helper) before Agent mode.
3. In **Agent mode**, obey each subtask's **TDD suitable** tag: **`yes`** → strict **test-driven-development**; **`partial`** → manual verification first, then TDD only the named **TDD slice** for any code fix; **`no`** → satisfy **Verification** and record the manual check.
4. If integration reveals a wrong contract in Milestone 1 or 2, pause, update the affected type-1 plan, and add a **Plan changelog** row.

## Subtasks

Dependency notation: `Blocked by: I1` means start after I1 is done.

### I1 - Verify Backend Local Startup

- [X] **Do:** Start PostgreSQL and the Spring Boot backend using documented commands. Confirm the backend boots and test profile/local profile behavior is clear.
- **TDD suitable:** no - Runtime/environment verification task, not a behavior implementation slice.
- **Blocked by:** Milestone 1 complete
- **Plan mode:** medium
- **Verification:** Backend starts locally and `cd backend; .\gradlew.bat test` passes

### I2 - Verify Frontend Local Startup

- [X] **Do:** Start the Vite frontend and confirm it can render public routes before connecting full auth flow.
- **TDD suitable:** no - Startup/smoke verification task with manual observability checks.
- **Blocked by:** Milestone 2 complete
- **Plan mode:** medium
- **Verification:** `cd frontend; npm run build` passes and `npm run dev` serves the app

### I3 - Verify Vite To Spring API Connectivity

- [X] **Do:** Confirm the frontend can call a backend endpoint through the chosen local strategy, either Vite proxy or explicit backend origin.
- **TDD suitable:** partial - Config-only checks are manual, but any backend/frontend contract fix should be test-driven.
- **Blocked by:** I1, I2
- **Plan mode:** high
- **Verification:** Browser network tab shows successful API response from the Spring backend

### I4 - Verify Session Cookie Login Flow

- [X] **Do:** Register and log in through the browser, then confirm the browser stores and sends the session cookie on authenticated requests.
- **TDD suitable:** partial - Browser session verification is manual, while backend auth behavior changes are testable.
- **Blocked by:** I3
- **Plan mode:** high
- **Verification:** Browser login succeeds, `GET /api/auth/me` succeeds after refresh, and no auth token is stored in local storage

### I5 - Verify CSRF And State-Changing Requests

- [X] **Do:** Confirm create/update/delete and logout requests satisfy Spring Security's CSRF behavior without disabling security broadly.
- **TDD suitable:** partial - Manual browser checks lead, but security-rule code fixes should follow strict TDD.
- **Blocked by:** I4
- **Plan mode:** high
- **Verification:** Browser can create a task and log out without CSRF errors

### I6 - Verify Task CRUD End To End

- [X] **Do:** Exercise create, list, read, update, status change, and delete from the browser through PostgreSQL persistence.
- **TDD suitable:** partial - End-to-end verification is manual; discovered backend behavior defects should be test-driven.
- **Blocked by:** I5
- **Plan mode:** medium
- **Verification:** Task changes survive page refresh and remain scoped to the logged-in user

### I7 - Verify Daily Plan Generation With Mocked Or Disabled Provider

- [X] **Do:** Confirm the frontend plan generation flow handles backend success, loading state, empty/invalid inputs, and provider-style errors before spending real API credits.
- **TDD suitable:** partial - Integration checks are manual, while backend logic fixes should be implemented test-first.
- **Blocked by:** I6
- **Plan mode:** medium
- **Verification:** Browser shows successful generated plan or friendly backend error without saving broken data

### I8 - Verify Real OpenAI Generation

- [X] **Do:** Run one manual plan generation with a real `OPENAI_API_KEY`, inspect output quality, and confirm the saved plan can be reloaded.
- **TDD suitable:** no - Real provider smoke validation depends on external runtime behavior and manual review.
- **Blocked by:** I7
- **Plan mode:** high
- **Verification:** One real generated plan is saved, returned to the frontend, and visible in plan history

### I9 - Verify Saved Plan History And Detail Routes

- [X] **Do:** Confirm generated plans appear in history and individual plan detail pages reload correctly from backend data.
- **TDD suitable:** partial - Route/query bugs are testable, but this milestone task is primarily manual verification.
- **Blocked by:** I8
- **Plan mode:** skip
- **Verification:** `/plans` and `/plans/:id` work after browser refresh

### I10 - Verify Multi-User Isolation Manually

- [ ] **Do:** Create two users and confirm tasks and plans from one user are not visible to the other.
- **TDD suitable:** partial - Manual isolation check first; any ownership bug fix should use test-first backend coverage.
- **Blocked by:** I9
- **Plan mode:** high
- **Verification:** Browser and/or HTTP client confirms user-scoped task and plan access

### I11 - Final Error And Empty-State Pass

- [ ] **Do:** Review common failure paths: unauthenticated user, validation error, missing task/plan, OpenAI failure, and no tasks available for planning.
- **TDD suitable:** partial - Validation is manual at integration level; backend behavior regressions should be covered by tests.
- **Blocked by:** I10
- **Plan mode:** medium
- **Verification:** Each failure path returns or displays a clear user-facing message

### I12 - Full Build And Test Pass

- [ ] **Do:** Run backend tests and frontend build from a clean local state.
- **TDD suitable:** no - Verification-only checkpoint with no direct production behavior changes.
- **Blocked by:** I11
- **Plan mode:** skip
- **Verification:** `cd backend; .\gradlew.bat test` and `cd frontend; npm run build`

### I13 - Write End-To-End README Instructions

- [ ] **Do:** Update README with setup, environment variables, database startup, backend startup, frontend startup, test commands, and manual verification checklist.
- **TDD suitable:** no - Documentation-only task validated by reproducible command walkthrough.
- **Blocked by:** I12
- **Plan mode:** skip
- **Verification:** README commands work from a clean checkout

### I14 - Add Java Learning Map

- [ ] **Do:** Add a README section mapping project packages and features to the Java/Spring concepts they teach.
- **TDD suitable:** no - Documentation/learning artifact with no runtime behavior.
- **Blocked by:** I13
- **Plan mode:** skip
- **Verification:** README clearly explains what to study in `auth`, `security`, `task`, `plan`, `ai`, repositories, DTOs, and tests

## TDD Note For Agent Mode

Per subtask, obey **TDD suitable**: **`partial`** integration work starts with the **Manual slice** to reproduce the issue, then applies strict **test-driven-development** only to the named **TDD slice** before re-running the manual check; **`no`** tasks record the verification steps in README or task notes so the path stays repeatable. Any backend behavior change discovered during integration must land with an automated test even when the subtask is **`partial`**.

## Plan Changelog

| Date | Change |
|------|--------|
| 2026-05-15 | Created integration milestone plan from the original monolithic FocusFlow AI plan. |
| 2026-05-23 | Added `TDD suitable` classification to all integration subtasks (I1-I14). |
| 2026-05-23 | Aligned with **writing-plans** scaffold: workflow chain, subtask field order, TDD/manual slices for partial integration work, fix-protocol discovery note. |
