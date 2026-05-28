# FocusFlow AI

FocusFlow AI is a Java-first learning project: a Spring Boot backend for task management and AI-assisted daily plan generation. Milestone 1 ships the backend API only (no frontend yet).

## Prerequisites

- Java 21 (JDK)
- Docker Desktop (for local PostgreSQL)
- Git

Gradle wrapper is included; you do not need a global Gradle install.

## Quick start (backend)

1. Clone the repository and copy environment defaults:

   ```powershell
   copy .env.example .env
   ```

2. Start PostgreSQL:

   ```powershell
   docker compose up -d
   ```

3. Set AI provider variables in `.env` if you want live plan generation (optional for CRUD testing):

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

4. Run the backend (creates/updates schema on first local run):

   ```powershell
   $env:SPRING_JPA_HIBERNATE_DDL_AUTO="update"
   cd backend
   .\gradlew.bat bootRun
   ```

   API base URL: `http://localhost:8080`

5. Run tests:

   ```powershell
   cd backend
   .\gradlew.bat test
   ```

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

Do not commit `.env`. Use `.env.example` as the template.

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

## Manual HTTP verification

Use an HTTP client that keeps cookies (curl cookie jar, Postman, etc.).

### CSRF bootstrap (curl)

Spring Security uses cookie-based CSRF. For curl:

1. Send one state-changing request without CSRF to receive the `XSRF-TOKEN` cookie (expect `403`).
2. Read `XSRF-TOKEN` from the cookie jar.
3. Send subsequent `POST`/`PUT`/`DELETE` requests with header `X-XSRF-TOKEN: <token>`.

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

## B18 verification checklist

Automated tests (2026-05-27):

- `cd backend; .\gradlew.bat test` — **82 tests, all passing**

Manual HTTP pass:

- Register → 201
- Login → 200
- `GET /api/auth/me` → 200
- Task create/list/get/update → 201/200
- Daily plan generate (DeepSeek-compatible config) → 201
- Daily plan get by id → 200
- Provider failure with invalid API key → 502, plans remain empty

Notes from verification:

- Unauthenticated API requests return **401 Unauthorized** (not 403).
- Deleting a task referenced by a saved daily plan item fails with a database constraint (expected with current schema).
- For local dev, set `SPRING_JPA_HIBERNATE_DDL_AUTO=update` because production config uses `ddl-auto: none`.

## Layering checklist (contributors)

- Controllers delegate to services only; never inject repositories.
- Services own business logic and transactions; repositories handle persistence only.
- Cross-feature reads go through facades (e.g. `TaskQueryService`), not another feature's repository.
- Security exposes `UserContext`, not auth API DTOs, to other layers.
- Architecture rules are enforced by `LayeredArchitectureTest` (ArchUnit) in `backend` tests.

## Java learning notes (Milestone 1)

- **Layered packages**: `auth`, `user`, `security`, `task`, `plan`, `ai`, and `common/error` keep responsibilities focused.
- **Session security**: Spring Security stores authentication in the HTTP session. Login establishes `JSESSIONID`; protected endpoints require that cookie.
- **CSRF**: Cookie + header pattern protects state-changing requests while keeping a session-based API.
- **JPA ownership scoping**: Tasks and plans belong to a user. Services/repositories query by owner id so users cannot access each other's data.
- **AI provider boundary**: `DailyPlanAiClient` is a mockable interface. `OpenAiDailyPlanClient` is the OpenAI-compatible adapter; services stay provider-agnostic.
- **Error contract**: Validation → 400, unauthenticated → 401, not found → 404, conflicts → 409, AI provider failures → 502 via `AiProviderException`.
- **Testing mix**: JUnit 5 + Mockito for services, `@WebMvcTest` for controllers, Testcontainers PostgreSQL for selected integration tests.

## Project layout

```
backend/          Spring Boot application
docker-compose.yml
.env.example
scripts/          Manual verification helpers
docs/             Design and milestone plans
```

## Next milestone

Frontend (React) will consume these APIs in Milestone 2.
