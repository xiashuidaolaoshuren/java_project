# FocusFlow AI

FocusFlow AI is a Java-first full-stack learning project. The Spring Boot backend manages tasks and AI-assisted daily plans; the React/Vite frontend provides the browser experience.

## Prerequisites

- Java 21 (JDK)
- Docker Desktop (for local PostgreSQL)
- Node.js LTS and npm (for the React frontend)
- Git

The Gradle wrapper is included, so a global Gradle installation is not required. The backend targets Java 21. On Gradle 9, the included Foojay toolchain resolver can provision Java 21 when it is not installed locally.

## Quick start

1. Clone the repository, then copy the environment template at the repository root:

   ```powershell
   copy .env.example .env
   ```

2. Start PostgreSQL:

   ```powershell
   docker compose up -d
   ```

3. Set AI provider variables in `.env` if you want live plan generation. They are optional for registration, login, and task CRUD:

   ```env
   OPENAI_API_KEY=your-provider-key
   OPENAI_MODEL=gpt-4.1-mini
   OPENAI_BASE_URL=https://api.openai.com/v1
   ```

   For DeepSeek (OpenAI-compatible):

   ```env
   OPENAI_API_KEY=your-deepseek-key
   OPENAI_MODEL=deepseek-chat
   OPENAI_BASE_URL=https://api.deepseek.com/v1
   ```

   `.\gradlew.bat bootRun` loads the repo-root `.env` into the Spring process automatically. You do not need `echo $env:OPENAI_*` to show values in your shell; restart `bootRun` after editing `.env`. Shell or CI environment variables still take precedence when set.

4. In one terminal, run the backend. The local database schema is created or updated only when `SPRING_JPA_HIBERNATE_DDL_AUTO=update` is set because the default configuration uses `ddl-auto: none`:

   ```powershell
   $env:SPRING_JPA_HIBERNATE_DDL_AUTO="update"
   cd backend
   .\gradlew.bat bootRun
   ```

   API base URL: `http://localhost:8080`

5. In a second terminal, install frontend dependencies once and start Vite:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

   Open `http://localhost:5173`. During development, Vite proxies `/api` requests to the Spring backend at `http://localhost:8080`; use port 5173 for browser and cookie-aware HTTP checks.

## Environment variables

| Variable | Purpose | Default |
|----------|---------|---------|
| `POSTGRES_DB` | Docker Postgres database name | `focusflow` |
| `POSTGRES_USER` | Docker Postgres user | `focusflow` |
| `POSTGRES_PASSWORD` | Docker Postgres password | `focusflow` |
| `POSTGRES_PORT` | Host port for Postgres | `5432` |
| `SPRING_DATASOURCE_URL` | JDBC URL for Spring Boot | `jdbc:postgresql://127.0.0.1:5432/focusflow` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `focusflow` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `focusflow` |
| `OPENAI_API_KEY` | Provider API key | empty |
| `OPENAI_MODEL` | Model id sent to provider | `gpt-4.1-mini` |
| `OPENAI_BASE_URL` | OpenAI-compatible API base URL | `https://api.openai.com/v1` |

Do not commit `.env`. Use `.env.example` as the template. Docker Compose and `bootRun` both read the repo-root `.env`; database settings also have matching defaults in `application.yml` when those variables are unset.

## Test and build commands

Run these from separate terminals as needed:

```powershell
# Backend: Testcontainers requires Docker Desktop to be running.
cd backend
.\gradlew.bat test

# Frontend production build
cd frontend
npm run build

# Optional frontend test suite
npm run test:run
```

## API overview

All endpoints except register/login require an authenticated session cookie (`JSESSIONID`). State-changing requests also require CSRF protection (`XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header).

### Auth

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | Public | Create account |
| POST | `/api/auth/login` | Public | Start session |
| POST | `/api/auth/logout` | Session | End session |
| GET | `/api/auth/me` | Session | Current user |

### Tasks

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/tasks` | Create task |
| GET | `/api/tasks` | List current user's tasks |
| GET | `/api/tasks/{id}` | Get one task |
| PUT | `/api/tasks/{id}` | Update task |
| DELETE | `/api/tasks/{id}` | Delete task |

### Daily plans

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/daily-plans/generate` | Generate plan from active tasks |
| GET | `/api/daily-plans` | List saved plans (optional `?planDate=YYYY-MM-DD`) |
| GET | `/api/daily-plans/{id}` | Get one saved plan |

## Manual verification checklist

With Docker, the backend, and Vite running, verify these through the browser at `http://localhost:5173` or an HTTP client that keeps cookies:

- An unauthenticated `GET /api/auth/me` or `GET /api/tasks` returns **401**.
- Register or log in. The browser holds an HttpOnly `JSESSIONID`; refresh still succeeds at `GET /api/auth/me`; do not store an auth token in local storage.
- Create, list, update, change status, and delete a task. Changes survive refresh. Mutating requests send the `X-XSRF-TOKEN` header.
- With at least one OPEN task and a configured provider key, generate a plan. It returns **201** and is available from `/plans` and `/plans/:id`.
- For a user with zero OPEN tasks, generation returns **400** with `no open tasks available for planning`; it does not call the provider or save an empty plan.
- A failed provider call returns **502** and saves no partial plan. Use an empty or invalid key only when intentionally checking this path; do not spend an extra provider credit just to repeat it.
- Register two users. A foreign task or daily-plan id returns **404**, and neither user sees the other user's records in list endpoints.

### Optional HTTP and CSRF checks

For curl, Postman, or similar clients, keep a cookie jar. Spring Security uses the readable `XSRF-TOKEN` cookie plus a matching header for state-changing requests:

1. Send `GET /api/auth/me` through the Vite proxy. It returns **401** while seeding the `XSRF-TOKEN` cookie.
2. Read `XSRF-TOKEN` from the cookie jar.
3. Send `POST`/`PUT`/`DELETE` requests with `X-XSRF-TOKEN: <token>` and the same cookie jar.

Example register payload:

```json
{
  "email": "you@example.com",
  "username": "youruser",
  "password": "password123"
}
```

Example login payload:

```json
{
  "username": "youruser",
  "password": "password123"
}
```

Example task create payload:

```json
{
  "title": "Write tests",
  "description": "TDD coverage",
  "priority": "HIGH",
  "dueDate": "2026-06-01",
  "estimatedMinutes": 60
}
```

Example plan generation payload:

```json
{
  "availableMinutes": 120,
  "planDate": "2026-06-01"
}
```

### Helper scripts

From repo root (backend must be running):

```powershell
powershell -ExecutionPolicy Bypass -File scripts/b18-manual-verify.ps1
powershell -ExecutionPolicy Bypass -File scripts/b18-provider-failure.ps1
```

Set `OPENAI_API_KEY` to an invalid value before running the failure script to verify `502 Bad Gateway` and no partial plan persistence.

## Layering checklist (contributors)

- Controllers delegate to services only; never inject repositories.
- Services own business logic and transactions; repositories handle persistence only.
- Cross-feature reads go through facades (e.g. `TaskQueryService`), not another feature's repository.
- Security exposes `UserContext`, not auth API DTOs, to other layers.
- Architecture rules are enforced by `LayeredArchitectureTest` (ArchUnit) in `backend` tests.

## Java and Spring learning map

Open these packages in roughly this order to trace a request from HTTP to persistence and back:

- **`auth`** — `AuthController` and `AuthService` show registration, login, the Spring Security authentication manager, BCrypt password hashing, and clear duplicate-email/username conflicts.
- **`security`** — `SecurityConfig`, `CsrfCookieFilter`, `CurrentUser`, and `UserContext` teach filter-chain configuration, cookie-based CSRF, a **401** authentication entry point, session logout, and how application code receives the current user without depending on auth API DTOs.
- **`user`** — `User` and `UserRepository` are the core JPA account entity and repository. Study the uniqueness constraints before following the auth service.
- **`task`** — `TaskController`, `TaskService`, `TaskQueryService`, and `TaskRepository` teach owner-scoped CRUD, transactions, mapping, and a small query facade. `TaskQueryService` lets the plan feature read tasks without coupling directly to another feature's repository.
- **`plan`** — `DailyPlanController`, `DailyPlanService`, `DailyPlan`, `DailyPlanItem`, and `DailyPlanRepository` show orchestration, persistence of an aggregate, and owner-scoped reads. The service rejects generation with no OPEN tasks before making an external request.
- **`ai`** — `DailyPlanAiClient` is the mockable provider boundary; `OpenAiDailyPlanClient` is the OpenAI-compatible implementation. Follow `DailyPlanPromptBuilder` to see structured prompt construction and `AiProviderException` to see provider failures become **502** responses.
- **`common/error`** — `GlobalExceptionHandler` and the exception types define the API error contract: validation or domain preconditions **400**, unauthenticated **401**, missing owner-scoped data **404**, conflicts **409**, and AI-provider failures **502**.
- **Repositories and JPA fetching** — Compare owner-id repository methods. `DailyPlanRepository` uses `@EntityGraph` and `SELECT DISTINCT` to load plan items and their tasks while `open-in-view` is disabled, avoiding lazy-initialization failures on response mapping.
- **DTOs and validation** — Request/response records under each feature's `dto` package separate the HTTP contract from JPA entities. `RegisterRequest`, for example, validates email, username, and password before `AuthService` runs.
- **Tests** — Mockito service tests isolate business behavior; `@WebMvcTest` controller tests exercise JSON, validation, and security boundaries; `PostgresIntegrationTest` uses Testcontainers for real PostgreSQL behavior; `LayeredArchitectureTest` uses ArchUnit to enforce dependency rules.

The frontend is the practical counterpart: Vite proxies `/api`, `frontend/src/lib/api.ts` uses `credentials: 'include'`, and it reads the CSRF cookie to attach the matching header for mutating requests.

## Project layout

```
backend/          Spring Boot application and JUnit tests
frontend/         React, TypeScript, Vite application and Vitest tests
docker-compose.yml Local PostgreSQL service
.env.example      Local database and OpenAI-compatible provider template
scripts/          Optional manual HTTP verification helpers
docs/             Design notes, plans, and implementation issues
```
