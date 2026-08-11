import { Link, Outlet } from 'react-router-dom'

import { useCurrentUser } from '@/features/auth/hooks'

export function PublicLayout() {
  useCurrentUser()

  return (
    <div className="flex min-h-svh flex-col items-center justify-center bg-background px-4 py-8 text-foreground">
      <div className="flex w-full max-w-md flex-col items-center gap-6">
        <Link to="/login" className="text-2xl font-semibold tracking-tight">
          FocusFlow
        </Link>
        <main className="w-full">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
