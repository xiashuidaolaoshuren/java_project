# Implementation Plan: FocusFlow AI Milestone 2 - Frontend MVP

**Spec:** `docs/superpowers/specs/2026-05-14-focusflow-ai-design.md`  
**UI design spec:** `docs/superpowers/specs/2026-05-31-focusflow-frontend-ui-design.md`  
**Created:** 2026-05-15  
**Milestone scope:** React/Vite frontend for auth, tasks, daily plan generation, and saved plan review.

## Summary

Ship a thin React single-page application that talks to the Spring Boot backend from Milestone 1. The frontend uses Vite, TypeScript, shadcn/ui, Tailwind CSS, React Router, and TanStack Query. It should provide login, registration, protected routes, task management, daily plan generation, plan history, and plan detail pages without introducing unnecessary frontend architecture.

Out of scope: Next.js, SSR, Redux, micro-frontends, complex animation systems, full design-system work, and replacing backend validation with frontend-only validation.

## Discovery Notes

- Reuse: Milestone 1 backend API is implemented and is the contract source for frontend types and API calls.
- UI design: `docs/superpowers/specs/2026-05-31-focusflow-frontend-ui-design.md` defines the visual direction (modern/polished, indigo–violet accent, light+dark), the left-sidebar app shell, two-column dashboard, sheet-based task editing, timeline plan view, and the loading/empty/error state conventions that **F3b** and F7–F15 must follow.
- Constraints: Follow the approved frontend stack: React, Vite, TypeScript, shadcn/ui, Tailwind CSS, React Router, and TanStack Query.
- Patterns to follow: Keep API access in `src/lib/api.ts`, server state in TanStack Query hooks, route pages in `src/routes`, and feature-specific UI in `src/features`.
- Test tooling: Introduce Vitest and React Testing Library during **F4** so **`TDD suitable: yes`** subtasks can test hooks, API helpers, and route guards; until then rely on build checks plus manual browser verification.
- Anti-goals: Do not store auth tokens in local storage. Do not introduce Redux or SSR. Do not let the frontend duplicate backend authorization rules.

## File Map

### Frontend Foundation

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `frontend/package.json` | create | Define frontend scripts and dependencies. | `dev`, `build`, `test`, `lint`. |
| `frontend/vite.config.ts` | create | Configure Vite, path aliases, and optional dev proxy. | Vite dev server behavior. |
| `frontend/tsconfig.json`, `frontend/tsconfig.app.json`, `frontend/tsconfig.node.json` | create | Configure TypeScript. | TypeScript compiler settings. |
| `frontend/components.json` | create | Configure shadcn/ui aliases and style. | shadcn/ui project config. |
| `frontend/src/main.tsx` | create | Mount React and top-level providers. | Browser entrypoint. |
| `frontend/src/App.tsx` | create | Define route tree and top-level app shell. | React Router routes. |
| `frontend/src/index.css` | create | Hold Tailwind and shadcn theme styles. | Global CSS. |
| `frontend/src/lib/utils.ts` | create | Provide shared utility helpers such as `cn`. | Utility exports. |

### API And State

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `frontend/src/lib/api.ts` | create | Centralize HTTP calls, credentials, CSRF handling, and error normalization. | API helper functions. |
| `frontend/src/lib/queryClient.ts` | create | Configure TanStack Query defaults. | Query client. |
| `frontend/src/types/api.ts` | create | Define frontend API request/response types that mirror backend DTOs. | Shared TypeScript types. |

### Routes And Layout

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `frontend/src/routes/LoginPage.tsx` | create | Render login route. | `/login`. |
| `frontend/src/routes/RegisterPage.tsx` | create | Render registration route. | `/register`. |
| `frontend/src/routes/DashboardPage.tsx` | create | Render authenticated task dashboard and plan generation entrypoint. | `/dashboard`. |
| `frontend/src/routes/PlanHistoryPage.tsx` | create | Render saved plans. | `/plans`. |
| `frontend/src/routes/PlanDetailPage.tsx` | create | Render one saved plan. | `/plans/:id`. |
| `frontend/src/components/layout/AppLayout.tsx` | create | Provide shared authenticated **left-sidebar** shell with nav, theme toggle, and user menu (per UI spec). | Layout component. |
| `frontend/src/components/layout/PublicLayout.tsx` | create | Provide public auth-page layout with a centered auth card. | Layout component. |
| `frontend/src/components/theme/*` | create | Provide light/dark theme provider, toggle, and persisted preference (per UI spec). | Theme provider and toggle. |

### Features

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `frontend/src/features/auth/api.ts` | create | Wrap auth endpoints with typed calls. | Auth API functions. |
| `frontend/src/features/auth/hooks.ts` | create | Provide current-user query and auth mutations. | `useCurrentUser`, login/register/logout hooks. |
| `frontend/src/features/auth/ProtectedRoute.tsx` | create | Gate authenticated pages. | Protected route component. |
| `frontend/src/features/auth/LoginForm.tsx` | create | Render and submit login form. | Login form component. |
| `frontend/src/features/auth/RegisterForm.tsx` | create | Render and submit registration form. | Registration form component. |
| `frontend/src/features/tasks/api.ts` | create | Wrap task endpoints with typed calls. | Task API functions. |
| `frontend/src/features/tasks/hooks.ts` | create | Provide task queries and mutations. | Task query/mutation hooks. |
| `frontend/src/features/tasks/TaskList.tsx` | create | Render task list and empty state. | Task list component. |
| `frontend/src/features/tasks/TaskForm.tsx` | create | Render create/edit task form. | Task form component. |
| `frontend/src/features/tasks/TaskActions.tsx` | create | Render status, edit, and delete actions. | Task action component. |
| `frontend/src/features/plans/api.ts` | create | Wrap daily plan endpoints with typed calls. | Plan API functions. |
| `frontend/src/features/plans/hooks.ts` | create | Provide plan generation and history queries. | Plan query/mutation hooks. |
| `frontend/src/features/plans/GeneratePlanCard.tsx` | create | Render available-time input and generate action. | Plan generation component. |
| `frontend/src/features/plans/DailyPlanView.tsx` | create | Render generated or saved plan detail. | Plan display component. |
| `frontend/src/features/plans/PlanHistoryList.tsx` | create | Render saved plan list. | Plan history component. |

### UI Components

| Path | Create/Modify | Responsibility | Public Surface |
|------|----------------|----------------|----------------|
| `frontend/src/components/ui/*` | create | Store shadcn/ui generated components. Existing: button, card, input, label, field, form, alert, separator. UI spec adds: sheet, dialog, dropdown-menu, sonner, badge, skeleton, select, and a date input as feature work reaches them. | UI component imports. |

## Blast Radius

| Path | Why Sensitive | Plan Mode Before Implementation |
|------|---------------|---------------------------------|
| `frontend/package.json` | Dependency choices affect all frontend code. | high - confirm Vite, React, TypeScript, shadcn/ui, Tailwind, Router, and Query versions. |
| `frontend/vite.config.ts` | Proxy and alias choices affect backend communication and imports. | high - confirm whether dev proxy or explicit backend origin is used. |
| `frontend/components.json` | shadcn/ui aliases and style affect every generated component import. | high - confirm aliases before adding components. |
| `frontend/src/lib/api.ts` | Central API helper controls cookies, CSRF, error handling, and all backend calls. | high - confirm contract with Spring Security behavior from Milestone 1. |
| `frontend/src/types/api.ts` | Type drift from backend DTOs can cause confusing integration bugs. | medium - confirm endpoint payloads after backend milestone. |
| `frontend/src/features/auth/*` | Auth flow controls access to every protected screen. | high - confirm current-user, login, logout, and refresh behavior. |

## Workflow For Implementers

1. **writing-plans** produced this file (type-1 decomposition only).
2. For each subtask with **Plan mode: high** or **medium**, use Cursor **Plan mode** and the **planning-subtasks** skill to produce a type-2 plan (`.cursor/plans/*.plan.md`) before Agent mode when design risk warrants it.
3. In **Agent mode**, obey each subtask's **TDD suitable** tag with the **test-driven-development** skill: **`yes`** → strict red/green/refactor for hooks, API helpers, guards, and form behavior; **`partial`** → TDD only the named **TDD slice**; **`no`** → satisfy **Verification** without forcing test-first.
4. Keep frontend state simple: TanStack Query for server data, React state for local UI only.
5. If backend API contracts change, pause, update this plan and `src/types/api.ts`, and add a **Plan changelog** row.

## Subtasks

Dependency notation: `Blocked by: F1` means start after F1 is done.

### F1 - Scaffold Vite React TypeScript App

- [X] **Do:** Create the `frontend` Vite React TypeScript project and verify it builds before adding product code.
- **TDD suitable:** no - Project bootstrap is structural setup validated by build checks.
- **Blocked by:** -
- **Plan mode:** high
- **Verification:** `cd frontend; npm run build`

### F2 - Configure Tailwind CSS And shadcn/ui

- [X] **Do:** Add Tailwind CSS, shadcn/ui config, global CSS, `cn` utility, and the first required UI components.
- **TDD suitable:** partial - Deterministic utility/config pieces can be test-driven; visual composition relies on build/manual verification.
- **Blocked by:** F1
- **Plan mode:** high
- **Verification:** `cd frontend; npm run build`

### F3 - Add Router And App Layout Shell

- [X] **Do:** Add React Router, public/authenticated layouts, initial route pages, and navigation structure.
- **TDD suitable:** partial - Routing and access behavior are testable, while layout presentation is primarily visual.
- **Blocked by:** F2
- **Plan mode:** medium
- **Verification:** `cd frontend; npm run build`

### F3b - Align App Shell With UI Design Spec

- [X] **Do:** Bring the F3 layout shell in line with `2026-05-31-focusflow-frontend-ui-design.md`: replace the centered top nav in `AppLayout` with a left sidebar (nav links, theme toggle, user-menu placeholder), center the auth card in `PublicLayout`, add the theme provider/toggle with persisted light/dark preference, and update `--primary` / related CSS variables to the indigo–violet accent. Add shadcn components needed for the shell (e.g. `dropdown-menu`, `skeleton` if used in nav).
- **TDD suitable:** partial - Theme preference persistence and routing/nav active state are testable; sidebar presentation is primarily visual.
- **Blocked by:** F3
- **Plan mode:** medium
- **Verification:** `cd frontend; npm run build` and manual check of sidebar nav, theme toggle, and public auth layout in the browser

### F4 - Add TanStack Query And API Client Foundation

- [X] **Do:** Configure QueryClient and `api.ts` with JSON handling, credentials support, normalized errors, and an initial CSRF handling path that matches the backend contract.
- **TDD suitable:** yes
- **Blocked by:** F3b
- **Plan mode:** high
- **Verification:** `cd frontend; npm run build`

### F5 - Define Frontend API Types

- [X] **Do:** Add TypeScript types for auth, task, plan, and error payloads based on the backend DTO contracts.
- **TDD suitable:** no - Static type declarations mirror contracts and are verified by typecheck/build.
- **Blocked by:** F4 and Milestone 1 API contracts
- **Plan mode:** medium
- **Verification:** `cd frontend; npm run build`

### F6 - Implement Current User Query And Protected Routes

- [X] **Do:** Add current-user query, loading behavior, unauthenticated redirects, and protected route behavior.
- **TDD suitable:** yes
- **Blocked by:** F5
- **Plan mode:** high
- **Verification:** `cd frontend; npm run build` and manual browser check with backend running

### F7 - Build Login And Registration Pages

- [X] **Do:** Build login and registration forms using shadcn/ui components, auth mutations, validation display, and success redirects.
- **TDD suitable:** yes
- **Blocked by:** F6
- **Plan mode:** medium
- **Verification:** `cd frontend; npm run build` and manual register/login check

### F8 - Build Logout And Session Refresh Behavior

- [X] **Do:** Add logout action, query invalidation, and refresh-safe authenticated shell behavior.
- **TDD suitable:** yes
- **Blocked by:** F7
- **Plan mode:** medium
- **Verification:** `cd frontend; npm run build` and manual logout/refresh check

### F9 - Build Task Read View

- [X] **Do:** Add task API calls, task query hook, dashboard task list, loading state, error state, and empty state.
- **TDD suitable:** yes
- **Blocked by:** F8
- **Plan mode:** medium
- **Verification:** `cd frontend; npm run build` and manual task list check

### F10 - Build Task Create Form

- [X] **Do:** Add task creation UI and mutation with query invalidation after successful create.
- **TDD suitable:** yes
- **Blocked by:** F9
- **Plan mode:** medium
- **Verification:** `cd frontend; npm run build` and manual create task check

### F11 - Build Task Edit, Status, And Delete UI

- [ ] **Do:** Add edit, status update, and delete actions with confirmation or safe UI feedback where appropriate.
- **TDD suitable:** yes
- **Blocked by:** F10
- **Plan mode:** medium
- **Verification:** `cd frontend; npm run build` and manual update/delete task check

### F12 - Build Daily Plan Generation UI

- [ ] **Do:** Add available focus time input, generate action, loading state, provider error display, and generated-plan rendering.
- **TDD suitable:** yes
- **Blocked by:** F11
- **Plan mode:** medium
- **Verification:** `cd frontend; npm run build` and manual generate-plan check

### F13 - Build Plan History Page

- [ ] **Do:** Add saved plan list query, history route, empty state, and links to detail pages.
- **TDD suitable:** partial - Query and routing behavior are testable; most value is in UI composition/flow checks.
- **Blocked by:** F12
- **Plan mode:** skip
- **Verification:** `cd frontend; npm run build` and manual plan history check

### F14 - Build Plan Detail Page

- [ ] **Do:** Add saved plan detail query and display for one plan by id.
- **TDD suitable:** partial - Data-fetch behavior is testable; rendering details are primarily visual.
- **Blocked by:** F13
- **Plan mode:** skip
- **Verification:** `cd frontend; npm run build` and manual plan detail check

### F15 - Frontend Polish And Accessibility Pass

- [ ] **Do:** Review loading states, empty states, form labels, keyboard behavior, and common shadcn composition rules.
- **TDD suitable:** no - Mostly visual/UX refinement validated through targeted manual checks.
- **Blocked by:** F14
- **Plan mode:** skip
- **Verification:** `cd frontend; npm run build` and manual smoke check across all routes

## TDD Note For Agent Mode

Per subtask, obey **TDD suitable**: **`yes`** → strict **test-driven-development** for hooks, API helpers, guards, and form submit/validation behavior (requires Vitest from **F4**); **`partial`** → TDD only the named **TDD slice**, then satisfy **Manual slice** via build/browser review; **`no`** → satisfy **Verification** without forcing test-first. UI polish, layout, and subjective feel stay in manual verification unless the team adds visual regression tooling later.

## Plan Changelog

| Date | Change |
|------|--------|
| 2026-05-15 | Created frontend milestone plan from the original monolithic FocusFlow AI plan. |
| 2026-05-23 | Added `TDD suitable` classification to all frontend subtasks (F1-F15). |
| 2026-05-23 | Aligned with **writing-plans** scaffold: workflow chain, subtask field order, TDD/manual slices for partial UI work, Vitest introduction in F4, updated discovery reuse note. |
| 2026-05-31 | Linked the new frontend UI design spec (`2026-05-31-focusflow-frontend-ui-design.md`): added UI spec references, sidebar shell + theme module file-map entries, expanded UI component inventory, and an F3 sidebar follow-up note. |
| 2026-05-31 | Added **F3b** (align app shell with UI spec) after completed F3; F4 now blocked by F3b; removed inline F3 follow-up note. |
