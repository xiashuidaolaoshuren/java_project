import { Link, Outlet } from 'react-router-dom'

export function PublicLayout() {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center bg-background px-4 py-8 text-foreground">
      <div className="flex w-full max-w-md flex-col items-center gap-6">
        <Link to="/login" className="text-2xl font-semibold tracking-tight">
          FocusFlow
        </Link>
        <div className="w-full">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
