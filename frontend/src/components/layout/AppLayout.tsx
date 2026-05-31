import { Link, NavLink, Outlet } from 'react-router-dom'

import { cn } from '@/lib/utils'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    'rounded-md px-3 py-2 text-sm font-medium transition-colors',
    isActive
      ? 'bg-primary text-primary-foreground'
      : 'text-muted-foreground hover:bg-muted hover:text-foreground',
  )

export function AppLayout() {
  return (
    <div className="flex min-h-svh flex-col bg-background text-foreground">
      <header className="border-b border-border px-6 py-4">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-4">
          <Link to="/dashboard" className="text-lg font-semibold">
            FocusFlow
          </Link>
          <nav className="flex flex-wrap items-center gap-1">
            <NavLink to="/dashboard" className={navLinkClass}>
              Dashboard
            </NavLink>
            <NavLink to="/plans" className={navLinkClass}>
              Plans
            </NavLink>
            <NavLink to="/login" className={navLinkClass}>
              Login
            </NavLink>
            <NavLink to="/register" className={navLinkClass}>
              Register
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col px-6 py-8">
        <Outlet />
      </main>
    </div>
  )
}
