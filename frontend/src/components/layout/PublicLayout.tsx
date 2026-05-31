import { Link, Outlet } from 'react-router-dom'

import { Button } from '@/components/ui/button'

export function PublicLayout() {
  return (
    <div className="flex min-h-svh flex-col bg-background text-foreground">
      <header className="border-b border-border px-6 py-4">
        <div className="mx-auto flex max-w-3xl items-center justify-between">
          <Link to="/login" className="text-lg font-semibold">
            FocusFlow
          </Link>
          <nav className="flex items-center gap-2">
            <Button variant="ghost" render={<Link to="/login" />}>
              Login
            </Button>
            <Button variant="outline" render={<Link to="/register" />}>
              Register
            </Button>
          </nav>
        </div>
      </header>
      <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col px-6 py-8">
        <Outlet />
      </main>
    </div>
  )
}
