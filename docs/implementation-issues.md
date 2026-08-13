# Implementation Issues

A learning log of bugs and surprises found while building FocusFlow AI. Each entry records what went wrong, why, and what we changed so the lesson sticks.

---

## 1. First-login redirect race

**When:** Frontend auth (login flow)  
**Area:** `frontend/src/features/auth/hooks.ts`, `ProtectedRoute`  
**Symptom:** After a successful first login, the app briefly navigated to `/dashboard` then bounced back to `/login`. A second successful login then reached the dashboard.

### What we saw

1. Submit valid credentials on `/login`.
2. Login API succeeds (session cookie is set).
3. App navigates to `/dashboard`, then redirects back to `/login`.
4. Log in again → stays on `/dashboard`.

Failed logins correctly stayed on the login page with an error; the bug was only on the *first successful* login of a session.

### Root cause

`useLogin` used this success path:

```ts
onSuccess: async () => {
  await queryClient.invalidateQueries({ queryKey: currentUserQueryKey })
  navigate('/dashboard')
},
```

`invalidateQueries` marks the current-user query stale and triggers a refetch of `/api/auth/me`. Navigation happened while that refetch was still in flight (or after a refetch that still saw an unauthenticated state). On `/dashboard`, `ProtectedRoute` reads `useCurrentUser()`:

- While refetching / with cached `null` → `isAuthenticated` is `false`
- → `<Navigate to="/login" />`

So the session cookie was already valid, but the React Query cache had not been updated to the logged-in user yet.

On the second login, the cache and timing often lined up, so the race was less visible.

### Why `useRegister` did not have the same bug

Register already seeded the cache from the mutation response:

```ts
onSuccess: (user) => {
  queryClient.setQueryData(currentUserQueryKey, user)
  navigate('/dashboard')
},
```

Login returned a `UserResponse` too, but ignored it and relied on a background refetch instead.

### Fix

Align login with register: set the authenticated user into the cache *before* navigating.

```ts
onSuccess: (user) => {
  queryClient.setQueryData(currentUserQueryKey, user)
  navigate('/dashboard')
},
```

`ProtectedRoute` then sees `isAuthenticated: true` on the first render of `/dashboard`.

### Lesson

- Prefer **optimistic / synchronous cache updates** from a mutation response when the API already returns the data you need.
- `invalidateQueries` + navigate is a classic race when a route guard depends on that query.
- Mirror patterns across related hooks (`useLogin` / `useRegister`) so one path does not silently lag behind the safer one.
- Cover the race with a regression test: after login success, assert the current-user query is set and `isAuthenticated` is true *even if* a subsequent `/auth/me` refetch would return `null`.

---

## 2. First-register session never established

**When:** Backend auth (register flow)  
**Area:** `backend/src/main/java/com/focusflow/auth/AuthService.java`  
**Symptom:** After a successful registration, the UI navigated to `/dashboard` as the new user, but every authenticated API call (`/api/tasks`, `/api/auth/me`, etc.) returned **401**. Logging out and logging in again made everything work.

### What we saw

1. Submit valid register form on `/register`.
2. `POST /api/auth/register` returns `201` with the new user body.
3. Frontend seeds the current-user cache and navigates to `/dashboard`.
4. Dashboard data fetches fail with 401.
5. Logout → login with the same credentials → dashboard works.

### Root cause

`AuthService.register()` created and saved the user, then returned a `UserResponse` — but it never authenticated the session:

```java
User saved = userRepository.save(user);
return new UserResponse(saved.getId(), saved.getEmail(), saved.getUsername());
```

`login()` did the missing step:

```java
Authentication authentication =
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password()));
SecurityContextHolder.getContext().setAuthentication(authentication);
```

Without that, Spring Security had no authenticated `SecurityContext` / session for the new user. `SecurityConfig` requires `authenticated()` for all non-login/register routes, so subsequent requests got 401.

The frontend looked logged in because it trusted the register response body and called `setQueryData` — the client and server were out of sync.

### Why existing controller tests missed it

`AuthControllerTest` mocks `AuthService` with `@MockBean`, so it only asserts HTTP status and JSON shape. It never exercises the real `register()` session side effect.

### Fix

After saving the user, authenticate with the request credentials the same way `login()` does:

```java
User saved = userRepository.save(user);

Authentication authentication =
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.username(), request.password()));
SecurityContextHolder.getContext().setAuthentication(authentication);

return new UserResponse(saved.getId(), saved.getEmail(), saved.getUsername());
```

### Lesson

- Returning a user body is not the same as establishing a session. Related auth paths (`register` / `login`) must both set `SecurityContext` when the product expects an immediate logged-in state.
- Mocking the service in controller tests hides session side effects — add a service-level (or integration) test that asserts `SecurityContextHolder` after register.
- Client-side cache seeding can mask a missing server session until the first protected API call.

---

## 3. First login/register POST returns 401 (CSRF cookie never seeded)

**When:** Auth integration (login / register from the SPA)  
**Area:** `backend/.../security/CsrfCookieFilter.java`, `SecurityConfig.java`, `frontend/.../PublicLayout.tsx`  
**Symptom:** With correct credentials, the first `POST /api/auth/login` (or register) failed with **401** and the UI showed “Request failed with status 401”. The second attempt succeeded. Backend logs only showed DispatcherServlet initializing on that first request — easy to misread as “backend not ready.”

### What we saw

1. Start backend + Vite frontend; open `/login` with a clean browser (no cookies).
2. Submit valid credentials once → **401**.
3. Submit the same credentials again → **200** and session works.
4. Same pattern on `/register`.

Network tab: first POST had no `X-XSRF-TOKEN` header; the failing response set `XSRF-TOKEN`; the second POST sent the header and succeeded.

### Root cause (two layers)

**1. Public pages never called the API before the first POST**

`PublicLayout` (used by `/login` and `/register`) rendered the form only. No GET hit the backend, so the browser had no `XSRF-TOKEN` cookie.

`apiRequest` only attaches `X-XSRF-TOKEN` when the cookie already exists:

```ts
if (isMutatingMethod(method)) {
  const csrfToken = getCsrfToken()
  if (csrfToken) {
    // set X-XSRF-TOKEN
  }
}
```

Protected routes already called `useCurrentUser()` → `GET /api/auth/me`, which *could* seed the cookie. Public pages did not.

**2. Spring Security 6.x lazy CSRF: GET alone still did not set the cookie**

Even after calling `GET /api/auth/me` from `PublicLayout`, the cookie was still missing on that 401 response.

With Spring Boot 3.4 / Security 6.4, CSRF uses a deferred token (`DeferredCsrfToken` / `SupplierCsrfToken`). The cookie is written only when the token is **resolved** (`getToken()`), which happens when:

- A controller method injects `CsrfToken`, or
- `CsrfFilter` validates the token on a state-changing request (POST/PUT/…), including when validation fails.

For anonymous `GET /api/auth/me`:

1. `CsrfFilter` attaches an unresolved deferred token.
2. Auth fails → `HttpStatusEntryPoint` returns **401**.
3. Response commits → **no** `Set-Cookie: XSRF-TOKEN` (token never resolved).

The first login POST then fails CSRF. During rejection the token is resolved and the cookie is saved — so the second POST works. Spring maps anonymous CSRF failures through the authentication entry point, so the status is **401**, not 403.

DispatcherServlet “Initializing…” on the first request is normal lazy servlet init, not the cause of the 401.

### Fix

**Frontend:** Call `useCurrentUser()` in `PublicLayout` so mount triggers `GET /api/auth/me` before any login/register POST.

**Backend:** Add `CsrfCookieFilter` after `CsrfFilter` that forces resolution:

```java
CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
if (csrfToken != null) {
  csrfToken.getToken(); // resolve deferred token → CookieCsrfTokenRepository saves XSRF-TOKEN
}
filterChain.doFilter(request, response);
```

Register with `http.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)`.

Together: public page GET seeds the cookie; subsequent POSTs send `X-XSRF-TOKEN` and succeed on the first try.

### Lesson

- Cookie CSRF + SPA needs a **GET (or other non-mutating) round-trip that actually resolves the token** before the first POST — not only “hit any API.”
- On Spring Security 6+, assume deferred CSRF: a 401 from an entry point does **not** imply the CSRF cookie was set.
- A “first attempt fails, second works” auth bug is often missing CSRF on attempt 1, not wrong credentials.
- Cover with a regression test: unauthenticated `GET /api/auth/me` must return `Set-Cookie: XSRF-TOKEN` (and a frontend test that public layout loads current user on mount).

### Follow-up: residual race on fast first submit

**When:** After the `PublicLayout` + `CsrfCookieFilter` fix  
**Area:** `frontend/src/features/auth/hooks.ts`, `LoginForm.tsx`, `RegisterForm.tsx`  
**Symptom:** A fast first login or register POST could still fail with **401** if the user submitted before the seed `GET /api/auth/me` completed. `PublicLayout` kicked off `useCurrentUser()` on mount, but forms stayed submittable while that GET was in flight.

### Root cause

`apiRequest` only attaches `X-XSRF-TOKEN` when the `XSRF-TOKEN` cookie already exists. The seed GET and the user's POST were independent — no coordination between them.

### Fix

Gate `useLogin` and `useRegister` at the mutation level: each `mutationFn` awaits `queryClient.ensureQueryData({ queryKey: currentUserQueryKey, queryFn: getCurrentUser })` before calling `login()` / `register()`. This shares the in-flight seed query started by `PublicLayout` (deduped) and blocks the POST until the cookie is set. Regression tests assert login/register are not called while the seed query is pending.

### Lesson

- Seeding CSRF on layout mount is necessary but not sufficient — **mutations that need the cookie must await the seed query** before POSTing.
- Mutation-level gating is more robust than form-level disable alone (covers programmatic submits and keeps a single coordination point).

---

## 4. Daily plan list 500 (LazyInitializationException)

**When:** Dashboard and plan history pages load saved plans  
**Area:** `backend/src/main/java/com/focusflow/plan/DailyPlanRepository.java`, `DailyPlanService.toPlanResponse`  
**Symptom:** Both pages showed **“Could not load plans / Something went wrong while loading your plans.”** The browser console reported `GET /api/daily-plans` **500**. Generating a new plan and opening a single plan by id still worked.

### What we saw

1. Log in, open `/dashboard` or `/plans`.
2. Frontend calls `GET /api/daily-plans` (optionally with `?planDate=…`).
3. Backend logs:

```
org.hibernate.LazyInitializationException: failed to lazily initialize a collection of role: com.focusflow.plan.DailyPlan.items: could not initialize proxy - no Session
        at com.focusflow.plan.DailyPlanService.toPlanResponse(DailyPlanService.java:129)
        at com.focusflow.plan.DailyPlanService.listForCurrentUser(DailyPlanService.java:84)
        at com.focusflow.plan.DailyPlanController.list(DailyPlanController.java:30)
```

4. `POST /api/daily-plans/generate` and `GET /api/daily-plans/{id}` succeeded for the same user.

### Root cause

`toPlanResponse` always walks the lazy `items` collection (and each item’s lazy `task`):

```java
plan.getItems().stream()
    .map(item -> new DailyPlanItemResponse(
            item.getPosition(),
            taskResponseMapper.toResponse(item.getTask())))
```

`spring.jpa.open-in-view` is **false** (intentional). `listForCurrentUser` is **not** `@Transactional`, so the Hibernate session closes when the repository method returns.

Only `findByOwner_IdAndId` fetched associations:

```java
@EntityGraph(attributePaths = {"items", "items.task"})
Optional<DailyPlan> findByOwner_IdAndId(Long ownerId, Long planId);

List<DailyPlan> findByOwner_IdAndPlanDateOrderByCreatedAtDesc(...); // no graph
List<DailyPlan> findAllByOwner_IdOrderByCreatedAtDesc(...);         // no graph
```

List endpoints therefore returned `DailyPlan` rows whose `items` proxy was uninitialized. Mapping after the session closed threw `LazyInitializationException` → 500 → the UI error banner.

`generate` worked because it is `@Transactional` and maps the in-memory collection just saved. `getById` worked because of the EntityGraph.

### Why existing tests missed it

- `DailyPlanServiceTest` mocks the repository and returns a plain `DailyPlan` whose `items` is an initialized `ArrayList` — no Hibernate proxy.
- `DailyPlanControllerTest` mocks `DailyPlanService` and never hits JPA.
- `PostgresIntegrationTest` asserted item/task data only on `findByOwner_IdAndId` (the graph query). List queries only asserted plan ids, never `getItems()`.

### Fix

Give the two list methods the same graph as get-by-id, plus `SELECT DISTINCT` so a fetch-join of the `items` bag does not duplicate a plan once per item:

```java
@EntityGraph(attributePaths = {"items", "items.task"})
@Query("SELECT DISTINCT p FROM DailyPlan p WHERE p.owner.id = :ownerId ORDER BY p.createdAt DESC")
List<DailyPlan> findAllByOwner_IdOrderByCreatedAtDesc(@Param("ownerId") Long ownerId);
```

(and the matching date-filtered query). Keep `open-in-view: false`. Do not make `items` EAGER or “fix” this with `@Transactional` on the service alone — that would hide the missing fetch and add N+1 loads.

Regression: `PostgresIntegrationTest.listDailyPlans_initializesItemsAndNestedTasks` persists a two-item plan, calls both list methods **outside** an extra transaction, and asserts size 1 plus readable item positions and nested task titles.

### Lesson

- With OSIV off, **every query that feeds a mapper must fetch the associations the mapper touches**. A graph on get-by-id does not cover list.
- Mockito unit tests cannot catch lazy-init; you need a persistence test that accesses collections **after** the repository transaction ends.
- Adding `@EntityGraph` on a bag collection without `SELECT DISTINCT` can return duplicate parent rows — assert list size, not only that items load.
- Do not re-enable Open Session In View to paper over incomplete fetch graphs.