import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import App from '@/App'
import { ThemeProvider } from '@/components/theme/ThemeProvider'
import { TooltipProvider } from '@/components/ui/tooltip'

vi.mock('@/features/auth/api', () => ({
  getCurrentUser: vi.fn(),
}))

import { getCurrentUser } from '@/features/auth/api'

const mockedGetCurrentUser = vi.mocked(getCurrentUser)

function renderApp(initialPath: string) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <TooltipProvider>
          <MemoryRouter initialEntries={[initialPath]}>
            <App />
          </MemoryRouter>
        </TooltipProvider>
      </QueryClientProvider>
    </ThemeProvider>,
  )
}

describe('App auth routing', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('redirects unauthenticated users away from protected routes', async () => {
    mockedGetCurrentUser.mockResolvedValue(null)

    renderApp('/dashboard')

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /sign in/i }),
      ).toBeInTheDocument()
    })
    expect(screen.queryByText('Dashboard placeholder')).not.toBeInTheDocument()
  })

  it('allows authenticated users to access protected routes', async () => {
    mockedGetCurrentUser.mockResolvedValue({
      id: 1,
      email: 'user@example.com',
      username: 'user',
    })

    renderApp('/dashboard')

    await waitFor(() => {
      expect(screen.getByText('Dashboard placeholder')).toBeInTheDocument()
    })
    expect(
      screen.queryByRole('button', { name: /sign in/i }),
    ).not.toBeInTheDocument()
  })
})
