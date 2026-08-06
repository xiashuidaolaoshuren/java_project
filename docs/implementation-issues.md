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