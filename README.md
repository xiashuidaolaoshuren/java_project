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

4. In one terminal, run the backend. The local database schema is created or updated only when `SPRING_JPA_HIBERNATE_DDL_AUTO=update` is set because the default configuration uses `ddl-auto: none`. Restart with that same `update` setting after pulling new nullable columns (for example `available_minutes` and `warning` on `daily_plans`) so Hibernate adds them. Do not use `create` or `create-drop` on a Docker volume you want to keep:

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

## Documentation

- [API reference](docs/api.md) — session/CSRF calling rules, request/response schemas, status codes
- [OpenAPI](docs/openapi.yaml) — machine-readable contract (OpenAPI 3.0)
- [Architecture](docs/architecture.md) — system overview, backend components, request and generate flows, data model

## Manual verification checklist

With Docker, the backend, and Vite running, verify these through the browser at `http://localhost:5173` or an HTTP client that keeps cookies:

- An unauthenticated `GET /api/auth/me` or `GET /api/tasks` returns **401**.
- Register or log in. The browser holds an HttpOnly `JSESSIONID`; refresh still succeeds at `GET /api/auth/me`; do not store an auth token in local storage.
- Create, list, update, change status, and delete a task. Changes survive refresh. Mutating requests send the `X-XSRF-TOKEN` header.
- With at least one plannable task (`OPEN` or `IN_PROGRESS`) and a configured provider key, generate a plan. It returns **201** and is available from `/plans` and `/plans/:id`. `IN_PROGRESS`-only is valid.
- For a user with no plannable tasks, generation returns **400** with `no plannable tasks available for planning`; it does not call the provider or save an empty plan.
- A failed provider call returns **502** and saves no partial plan. Use an empty or invalid key only when intentionally checking this path; do not spend an extra provider credit just to repeat it.
- Delete a plan from `/plans` or `/plans/:id` after the confirm dialog. Success is **204**; the plan and its items are gone, tasks remain. Detail delete navigates to `/plans`. The dashboard has no delete control.
- Register two users. A foreign task or daily-plan id returns **404**, and neither user sees the other user's records in list endpoints.

See [API reference](docs/api.md) for CSRF setup, example payloads, and full endpoint schemas.

### Helper scripts

From repo root (backend must be running):

```powershell
powershell -ExecutionPolicy Bypass -File scripts/b18-manual-verify.ps1
powershell -ExecutionPolicy Bypass -File scripts/b18-provider-failure.ps1
```

Set `OPENAI_API_KEY` to an invalid value before running the failure script to verify `502 Bad Gateway` and no partial plan persistence.

## Project layout

```
backend/             Spring Boot application and JUnit tests
frontend/            React, TypeScript, Vite application and Vitest tests
docker-compose.yml   Local PostgreSQL service
.env.example         Local database and OpenAI-compatible provider template
scripts/             Optional manual HTTP verification helpers
docs/                API reference, OpenAPI, architecture, design notes, and plans
  api.md             Human-readable API calling guide
  openapi.yaml       OpenAPI 3.0 contract
  architecture.md    System architecture, components, and learning map
```
