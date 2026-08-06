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
