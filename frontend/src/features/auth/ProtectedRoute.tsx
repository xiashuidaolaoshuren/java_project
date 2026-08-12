import { Navigate, Outlet } from 'react-router-dom'
import { Loader2Icon } from 'lucide-react'

import { useCurrentUser } from '@/features/auth/hooks'

export function ProtectedRoute() {
  const { isPending, isAuthenticated } = useCurrentUser()

  if (isPending) {
    return (
      <div
        className="flex min-h-svh flex-col items-center justify-center gap-2"
        role="status"
        aria-live="polite"
      >
        <Loader2Icon className="size-6 animate-spin text-muted-foreground" aria-hidden="true" />
        <p className="text-sm text-muted-foreground">Loading session…</p>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
