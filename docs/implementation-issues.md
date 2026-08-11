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