import { afterEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/lib/api'
import type { UserResponse } from '@/types/api'

import { getCurrentUser } from '@/features/auth/api'

describe('getCurrentUser', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns the current user on success', async () => {
    const user: UserResponse = {
      id: 1,
      email: 'user@example.com',
      username: 'user',
    }
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(user), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )

    await expect(getCurrentUser()).resolves.toEqual(user)
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/auth/me'),
      expect.objectContaining({ credentials: 'include' }),
    )
  })

  it('returns null when the session is unauthenticated (401)', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response('', { status: 401 })),
    )

    await expect(getCurrentUser()).resolves.toBeNull()
  })

  it('rethrows non-401 API errors', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            status: 500,
            message: 'Server error',
          }),
          {
            status: 500,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
      ),
    )

    await expect(getCurrentUser()).rejects.toBeInstanceOf(ApiError)
    await expect(getCurrentUser()).rejects.toMatchObject({ status: 500 })
  })
})
