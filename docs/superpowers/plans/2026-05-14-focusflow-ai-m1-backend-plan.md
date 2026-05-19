# Implementation Plan: FocusFlow AI Milestone 1 - Backend MVP

**Spec:** `docs/superpowers/specs/2026-05-14-focusflow-ai-design.md`  
**Created:** 2026-05-15  
**Milestone scope:** Backend API, database, session security, OpenAI adapter, and backend tests.

## Summary

Ship a tested Spring Boot backend that supports user registration, session login/logout, current-user lookup, authenticated task CRUD, saved daily plans, and daily plan generation through a mockable AI client with a real OpenAI adapter. This milestone is the main Java learning milestone. It should finish with a backend that can be exercised through HTTP clients before the React app exists.

Out of scope: React UI, browser integration, production deployment, RAG, embeddings, agents, calendar integration, notifications, and team features.

## Discovery Notes

- Reuse: The repository currently contains the approved design spec and planning docs only. There is no existing app code to preserve.
- Constraints: Use Java 21, Spring Boot, Gradle, PostgreSQL, Spring Data JPA/Hibernate, Spring Security sessions, OpenAI API, JUnit 5, Mockito, Spring MVC tests, and Testcontainers.
- Patterns to establish: Backend packages should stay focused by responsibility: `auth`, `user`, `security`, `task`, `plan`, `ai`, and `common/error`.
- Anti-goals: Do not introduce JWT, a vector database, frontend-specific work, or broad infrastructure beyond local PostgreSQL and backend configuration.

## File Map

### Repository And Local Backend Development

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `.gitignore` | create | Ignore Java, Node, IDE, build, environment, and local generated artifacts. | Repository hygiene. |
| `.env.example` | create | Document backend environment variables without secrets. | Example values for database and `OPENAI_API_KEY`. |
| `docker-compose.yml` | create | Run local PostgreSQL for backend development. | `postgres` service. |
| `README.md` | create/modify | Explain backend-first setup and milestone commands. | Developer documentation. |

### Spring Boot Foundation

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `backend/settings.gradle` | create | Name the Gradle project. | Gradle project identity. |
| `backend/build.gradle` | create | Configure Java 21, Spring Boot, dependencies, and test tooling. | `test`, `bootRun`, and build tasks. |
| `backend/gradlew`, `backend/gradlew.bat`, `backend/gradle/wrapper/*` | create | Provide reproducible Gradle execution. | Gradle wrapper commands. |
| `backend/src/main/java/com/focusflow/FocusFlowApplication.java` | create | Start the Spring Boot application. | `main` entrypoint. |
| `backend/src/main/resources/application.yml` | create | Configure datasource, JPA, sessions, CORS/CSRF related properties, and OpenAI properties. | Spring configuration keys. |
| `backend/src/test/resources/application-test.yml` | create | Configure tests separately from local development. | Spring test profile. |

### Common API Errors

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `backend/src/main/java/com/focusflow/common/error/ApiErrorResponse.java` | create | Standardize error response JSON. | Error response contract. |
| `backend/src/main/java/com/focusflow/common/error/GlobalExceptionHandler.java` | create | Map validation, auth, forbidden, missing-record, and provider failures to HTTP responses. | Controller advice. |
| `backend/src/main/java/com/focusflow/common/error/NotFoundException.java` | create | Represent missing user-owned records. | Service-layer exception. |
| `backend/src/main/java/com/focusflow/common/error/ForbiddenOperationException.java` | create | Represent disallowed user actions. | Service-layer exception. |

### Auth, User, And Security

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `backend/src/main/java/com/focusflow/user/User.java` | create | Persist user account identity and password hash. | JPA entity. |
| `backend/src/main/java/com/focusflow/user/UserRepository.java` | create | Query users by email or username. | Spring Data repository. |
| `backend/src/main/java/com/focusflow/security/SecurityConfig.java` | create | Configure session auth, endpoint authorization, CSRF, CORS/dev origin behavior, logout, and password encoding. | Security filter chain and beans. |
| `backend/src/main/java/com/focusflow/security/CurrentUser.java` | create | Represent authenticated user identity in controllers/services. | Current-user access abstraction. |
| `backend/src/main/java/com/focusflow/auth/AuthController.java` | create | Expose auth endpoints. | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`. |
| `backend/src/main/java/com/focusflow/auth/AuthService.java` | create | Register users, validate uniqueness, encode passwords, and return current-user data. | Auth business operations. |
| `backend/src/main/java/com/focusflow/auth/dto/*.java` | create | Define auth request and response payloads. | Auth API DTOs. |

### Tasks

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `backend/src/main/java/com/focusflow/task/Task.java` | create | Persist user-owned tasks. | JPA entity. |
| `backend/src/main/java/com/focusflow/task/TaskPriority.java` | create | Define priority values. | Enum. |
| `backend/src/main/java/com/focusflow/task/TaskStatus.java` | create | Define task lifecycle values. | Enum. |
| `backend/src/main/java/com/focusflow/task/TaskRepository.java` | create | Query tasks by owner, status, due date, and id. | Spring Data repository. |
| `backend/src/main/java/com/focusflow/task/TaskService.java` | create | Enforce user scoping and task business behavior. | Task service methods. |
| `backend/src/main/java/com/focusflow/task/TaskController.java` | create | Expose task REST endpoints. | `/api/tasks` API. |
| `backend/src/main/java/com/focusflow/task/dto/*.java` | create | Define task request and response payloads. | Task API DTOs. |

### Daily Plans And AI

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `backend/src/main/java/com/focusflow/plan/DailyPlan.java` | create | Persist one generated plan for one user and date. | JPA entity. |
| `backend/src/main/java/com/focusflow/plan/DailyPlanItem.java` | create | Persist ordered plan items. | JPA entity. |
| `backend/src/main/java/com/focusflow/plan/DailyPlanRepository.java` | create | Query saved plans by owner, id, and date. | Spring Data repository. |
| `backend/src/main/java/com/focusflow/plan/DailyPlanService.java` | create | Coordinate active tasks, AI generation, validation, and persistence. | Daily-plan operations. |
| `backend/src/main/java/com/focusflow/plan/DailyPlanController.java` | create | Expose plan generation and read endpoints. | `/api/daily-plans` API. |
| `backend/src/main/java/com/focusflow/plan/dto/*.java` | create | Define plan request and response payloads. | Daily-plan API DTOs. |
| `backend/src/main/java/com/focusflow/ai/DailyPlanAiClient.java` | create | Provide a mockable AI generation boundary. | Java interface. |
| `backend/src/main/java/com/focusflow/ai/DailyPlanPromptBuilder.java` | create | Build prompts from tasks and user planning input. | Prompt builder method. |
| `backend/src/main/java/com/focusflow/ai/OpenAiProperties.java` | create | Bind OpenAI configuration. | Spring configuration properties. |
| `backend/src/main/java/com/focusflow/ai/OpenAiDailyPlanClient.java` | create | Call OpenAI and map structured output into internal data. | OpenAI adapter. |
| `backend/src/main/java/com/focusflow/ai/AiProviderException.java` | create | Represent provider failures safely. | Service-layer exception. |

### Backend Tests

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `backend/src/test/java/com/focusflow/testsupport/*.java` | create | Shared builders, factories, and security test helpers. | Test-only utilities. |
| `backend/src/test/java/com/focusflow/auth/AuthControllerTest.java` | create | Verify auth endpoint behavior. | Spring MVC tests. |
| `backend/src/test/java/com/focusflow/task/TaskServiceTest.java` | create | Verify task service behavior and user scoping. | Mockito unit tests. |
| `backend/src/test/java/com/focusflow/task/TaskControllerTest.java` | create | Verify task endpoint behavior. | Spring MVC tests. |
| `backend/src/test/java/com/focusflow/plan/DailyPlanServiceTest.java` | create | Verify plan orchestration and AI failure behavior. | Mockito unit tests. |
| `backend/src/test/java/com/focusflow/plan/DailyPlanControllerTest.java` | create | Verify daily-plan endpoint behavior. | Spring MVC tests. |
| `backend/src/test/java/com/focusflow/integration/PostgresIntegrationTest.java` | create | Verify selected persistence flows against PostgreSQL. | Testcontainers integration test. |

## Blast Radius

| Path | Why Sensitive | Plan Mode Before Implementation |
|------|---------------|---------------------------------|
| `backend/build.gradle` | Dependency and plugin choices affect all backend development. | high - confirm Spring Boot, Testcontainers, security, validation, and OpenAI client dependencies. |
| `backend/src/main/resources/application.yml` | Global app configuration can block boot, tests, database access, sessions, and provider calls. | high - confirm profile and secret handling strategy. |
| `backend/src/main/java/com/focusflow/security/SecurityConfig.java` | Security affects every endpoint and later browser integration. | high - confirm session, CSRF, CORS, and logout behavior. |
| `backend/src/main/java/com/focusflow/user/User.java` | User identity anchors ownership and authentication. | high - confirm fields, uniqueness, and password storage. |
| `backend/src/main/java/com/focusflow/task/Task.java` | Task shape drives CRUD, AI prompt input, and frontend forms. | high - confirm fields, enum persistence, nullability, and ownership. |
| `backend/src/main/java/com/focusflow/plan/DailyPlan.java` and `DailyPlanItem.java` | Persistence design crosses AI output, user ownership, and history views. | high - confirm relationships and cascade behavior. |
| `backend/src/main/java/com/focusflow/ai/OpenAiDailyPlanClient.java` | External API failures and spending must be isolated. | high - confirm client choice, structured output approach, and test strategy. |
| `docker-compose.yml` | Local database settings affect every backend task. | medium - confirm ports, credentials, and volume behavior. |

## Workflow For Implementers

1. Use Cursor Plan mode for subtasks marked `high`, especially security, persistence, and OpenAI work.
2. In Agent mode, use the `test-driven-development` skill: failing test, minimal code, refactor.
3. Keep the backend runnable without the frontend. Verify through tests and HTTP clients first.
4. If implementation changes file paths or task order, update this plan and add a changelog entry.

## Subtasks

Dependency notation: `Blocked by: B1` means start after B1 is done.

### B1 - Scaffold Backend Gradle Project

- [X] **Do:** Create the `backend` Spring Boot Gradle project with Java 21, the application entrypoint, wrapper, and a smoke test that proves the Spring context can load.
- **Blocked by:** -
- **Plan mode:** high
- **Verification:** `cd backend; .\gradlew.bat test`

### B2 - Add Local PostgreSQL Setup

- [X] **Do:** Add `docker-compose.yml`, `.env.example`, datasource configuration, and a test profile so the backend can connect to local PostgreSQL while tests stay isolated.
- **Blocked by:** B1
- **Plan mode:** high
- **Verification:** `docker compose config` and `cd backend; .\gradlew.bat test`

### B3 - Add Common Error Response Foundation

- [X] **Do:** Add standard API error response classes and exception handling for validation, not-found, forbidden, and provider-style failures.
- **Blocked by:** B1
- **Plan mode:** medium
- **Verification:** `cd backend; .\gradlew.bat test --tests "*GlobalExceptionHandler*"`

### B4 - Model Users And Repository

- [X] **Do:** Create the `User` entity and `UserRepository` with unique account identity and secure password-hash storage fields.
- **Blocked by:** B2
- **Plan mode:** high
- **Verification:** `cd backend; .\gradlew.bat test --tests "*PostgresIntegrationTest"`

### B5 - Model Tasks And Repository

- [X] **Do:** Create `Task`, `TaskPriority`, `TaskStatus`, and repository queries needed for owner-scoped task access.
- **Blocked by:** B4
- **Plan mode:** high
- **Verification:** `cd backend; .\gradlew.bat test --tests "*PostgresIntegrationTest"`

### B6 - Model Daily Plans And Repository

- [X] **Do:** Create `DailyPlan`, `DailyPlanItem`, and repository queries for owner-scoped plan history and plan lookup.
- **Blocked by:** B5
- **Plan mode:** high
- **Verification:** `cd backend; .\gradlew.bat test --tests "*PostgresIntegrationTest"`

### B7 - Add Backend Test Support Utilities

- [X] **Do:** Add small test builders, security helpers, and reusable fixtures for users, tasks, and plans so later tests remain readable.
- **Blocked by:** B4, B5, B6
- **Plan mode:** skip
- **Verification:** `cd backend; .\gradlew.bat test`

### B8 - Implement User Registration

- [X] **Do:** Add registration DTOs, service behavior, endpoint, validation, uniqueness checks, and password hashing.
- **Blocked by:** B3, B4, B7
- **Plan mode:** high
- **Verification:** `cd backend; .\gradlew.bat test --tests "*AuthControllerTest"`

### B9 - Implement Session Login And Logout

- [X] **Do:** Configure Spring Security session login/logout behavior and verify successful and failed login paths.
- **Blocked by:** B8
- **Plan mode:** high
- **Verification:** `cd backend; .\gradlew.bat test --tests "*AuthControllerTest"`

### B10 - Implement Current User Endpoint

- [ ] **Do:** Add current-user resolution and `GET /api/auth/me` so clients can derive authentication state from the backend.
- **Blocked by:** B9
- **Plan mode:** medium
- **Verification:** `cd backend; .\gradlew.bat test --tests "*AuthControllerTest"`

### B11 - Implement Task Create And List

- [ ] **Do:** Add task DTOs, service methods, and endpoints for creating tasks and listing only the authenticated user's tasks.
- **Blocked by:** B10
- **Plan mode:** medium
- **Verification:** `cd backend; .\gradlew.bat test --tests "*Task*"`

### B12 - Implement Task Read, Update, And Delete

- [ ] **Do:** Add owner-scoped task detail, update, and delete behavior, including 404/403-style safeguards where appropriate.
- **Blocked by:** B11
- **Plan mode:** medium
- **Verification:** `cd backend; .\gradlew.bat test --tests "*Task*"`

### B13 - Define Daily Plan AI Boundary

- [ ] **Do:** Add `DailyPlanAiClient`, plan-generation request/response DTOs, and internal data objects needed to mock AI generation in service tests.
- **Blocked by:** B12
- **Plan mode:** high
- **Verification:** `cd backend; .\gradlew.bat test --tests "*DailyPlanServiceTest"`

### B14 - Implement Daily Plan Generation With Mockable AI

- [ ] **Do:** Implement daily plan generation service and endpoint using active user tasks and the AI boundary, then persist validated plan output.
- **Blocked by:** B13
- **Plan mode:** high
- **Verification:** `cd backend; .\gradlew.bat test --tests "*DailyPlan*"`

### B15 - Implement Saved Plan Read Endpoints

- [ ] **Do:** Add owner-scoped saved plan list and detail endpoints for plan history.
- **Blocked by:** B14
- **Plan mode:** medium
- **Verification:** `cd backend; .\gradlew.bat test --tests "*DailyPlanControllerTest"`

### B16 - Add OpenAI Configuration And Prompt Builder

- [ ] **Do:** Add OpenAI configuration properties and prompt-building logic that turns active tasks and available focus time into structured provider input.
- **Blocked by:** B14
- **Plan mode:** high
- **Verification:** `cd backend; .\gradlew.bat test --tests "*DailyPlanPromptBuilder*"`

### B17 - Implement OpenAI Adapter

- [ ] **Do:** Implement `OpenAiDailyPlanClient` behind the AI interface, including provider error handling and structured output validation.
- **Blocked by:** B16
- **Plan mode:** high
- **Verification:** `cd backend; .\gradlew.bat test` and one manual local generation check with `OPENAI_API_KEY`

### B18 - Backend Full Pass And Documentation Notes

- [ ] **Do:** Run all backend tests, verify endpoints manually through an HTTP client, and update README with backend setup and Java learning notes.
- **Blocked by:** B17
- **Plan mode:** skip
- **Verification:** `cd backend; .\gradlew.bat test` and README backend setup works from a clean checkout

## TDD Note For Agent Mode

When implementing, follow the `test-driven-development` skill. This plan does not replace red/green/refactor. Backend work should generally start with a service, controller, or integration test before production code.

## Plan Changelog

| Date | Change |
|------|--------|
| 2026-05-15 | Created backend milestone plan from the original monolithic FocusFlow AI plan. |
