import { Navigate, Outlet } from 'react-router-dom'

import { useCurrentUser } from '@/features/auth/hooks'

export function ProtectedRoute() {
  const { isPending, isAuthenticated } = useCurrentUser()

  if (isPending) {
    return (
      <div className="flex min-h-svh items-center justify-center" role="status">
        <p className="text-sm text-muted-foreground">Loading session…</p>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
