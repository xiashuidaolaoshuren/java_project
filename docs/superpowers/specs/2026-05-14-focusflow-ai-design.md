# FocusFlow AI Design

Date: 2026-05-14

## Goal

FocusFlow AI is a Java-first full-stack learning project. The product is an AI-assisted productivity web app, but the main learning goal is to understand how a real Java web application is structured with Spring Boot.

The app lets users manage tasks and generate a realistic daily focus plan from their active task list using OpenAI. The AI feature is intentionally narrow so the project can focus on Java, Spring Boot, Spring Security, JPA, PostgreSQL, API design, validation, error handling, and backend testing.

## Technology Stack

- Backend: Java 21, Spring Boot, Gradle
- Database: PostgreSQL
- Persistence: Spring Data JPA with Hibernate
- Security: Spring Security with cookie-based server sessions
- Frontend: React, Vite, TypeScript, shadcn/ui, Tailwind CSS, React Router, TanStack Query
- AI provider: OpenAI API
- Testing: JUnit 5, Mockito, Spring MVC tests, Testcontainers with PostgreSQL

## Product Scope

The first version includes:

- User registration, login, and logout.
- Authenticated task management.
- Task fields: title, description, priority, due date, status, and optional estimated minutes.
- A daily focus plan generator.
- Saved daily plans with plan items.
- A simple React single-page application for task management and plan review.

The first version excludes:

- RAG, vector databases, embeddings, and agent workflows.
- Calendar integration.
- Team/shared tasks.
- Push notifications or email reminders.
- Complex analytics.

These exclusions keep the project focused on learning Java application development.

## User Workflow

1. A user registers or logs in.
2. The user creates tasks with enough information to plan their day.
3. The user reviews active tasks on the dashboard.
4. The user enters available focus time for today.
5. The user clicks "Generate Today's Plan."
6. The backend loads the user's active tasks, calls OpenAI, validates the response, saves the plan, and returns it.
7. The user views the generated plan and can later review past plans.

## Architecture

The backend uses a layered Spring Boot architecture:

- Controllers expose REST endpoints.
- DTOs define request and response payloads.
- Services contain business logic.
- Repositories handle database access through Spring Data JPA.
- Entities model persisted data.
- Security components handle login, logout, sessions, and current-user access.
- An AI client/service isolates OpenAI integration from the rest of the app.

The main domain objects are:

- User: account and authentication identity.
- Task: user-owned work item.
- DailyPlan: generated plan for a user on a date.
- DailyPlanItem: ordered item inside a daily plan.

The frontend is a React single-page application built with Vite and TypeScript. It uses React Router for page structure, TanStack Query for server-state fetching and cache invalidation, and shadcn/ui with Tailwind CSS for the UI layer.

React is the client. Spring Boot is the product backend. OpenAI is an external service.

## Authentication And Authorization

The app uses Spring Security with cookie-based server sessions.

Every task and daily-plan endpoint requires an authenticated user. Backend queries are always scoped by the current user. A user must not be able to read, update, delete, or generate plans from another user's tasks.

Passwords are stored only as secure hashes, using Spring Security's password encoding support such as BCrypt.

Because the frontend is a separate React SPA and the backend uses cookies, the design must account for browser security rules. In development, the Vite dev server should call the Spring backend through an allowed origin or proxy. State-changing requests should follow Spring Security's CSRF expectations instead of disabling security broadly.

The frontend should not store auth tokens in local storage. The browser should hold the session cookie, and API requests should include credentials. Authentication state should be derived from the backend, such as a current-user endpoint, instead of duplicating session logic in the client.

This session-based approach is beginner-friendly because it uses Spring Security's natural server-side model and avoids introducing JWT complexity before it is needed.

## API Shape

Initial backend endpoints:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/tasks`
- `POST /api/tasks`
- `GET /api/tasks/{id}`
- `PUT /api/tasks/{id}`
- `DELETE /api/tasks/{id}`
- `POST /api/daily-plans/generate`
- `GET /api/daily-plans`
- `GET /api/daily-plans/{id}`

The plan generation endpoint accepts the user's available focus time and optional preferences. It does not accept arbitrary task IDs from other users. The backend determines eligible tasks from the authenticated user's data.

The frontend routes should stay small and product-focused, such as login, register, dashboard, plan history, and plan detail pages. TanStack Query should manage server data such as the current user, task list, and saved plans. Local React state should be reserved for UI concerns such as dialog visibility, form draft values, filters, and tab selection.

## AI Plan Generation

When a user generates a plan:

1. The service loads active tasks for the authenticated user.
2. The service builds a structured prompt with task titles, descriptions, priorities, due dates, and estimated minutes.
3. The OpenAI service requests a structured response.
4. The backend validates the response before persistence.
5. The backend saves a DailyPlan and its DailyPlanItems.
6. The saved plan is returned to React.

Automated tests mock the OpenAI API. This avoids flaky tests and prevents accidental API spending.

## Error Handling

The API returns consistent errors:

- `400 Bad Request` for validation errors.
- `401 Unauthorized` for unauthenticated requests.
- `403 Forbidden` for authenticated users attempting forbidden actions.
- `404 Not Found` for missing user-owned records.
- `502 Bad Gateway` for OpenAI/provider failures.

If OpenAI fails or returns invalid output, the backend returns a friendly error and does not save a partial or broken daily plan.

## Testing Strategy

Tests should teach normal Java backend testing practice:

- JUnit 5 for all tests.
- Mockito for service-level unit tests.
- Spring MVC tests for controller endpoints.
- Testcontainers with PostgreSQL for selected integration tests involving persistence and user scoping.

The highest-priority tests cover:

- Authenticated users can manage only their own tasks.
- Task validation works.
- Daily plan generation uses only the current user's active tasks.
- OpenAI failures do not save broken plans.
- Generated plans are persisted and returned correctly.

## Learning Priorities

This project should emphasize:

- Java 21 syntax and object-oriented design.
- Spring Boot project structure.
- Dependency injection.
- Controller, service, repository, DTO, and entity responsibilities.
- JPA relationships and query methods.
- Spring Security session authentication.
- Configuration management for secrets such as the OpenAI API key.
- Backend testing habits.

The React frontend should stay simple and support the backend learning goal. The frontend should avoid unnecessary complexity such as Redux, SSR, or micro-frontend patterns unless a later learning goal specifically requires them.
