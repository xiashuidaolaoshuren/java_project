import { afterEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/lib/api'
import type { UserResponse } from '@/types/api'

import { getCurrentUser, login, register } from '@/features/auth/api'

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

describe('login', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns the user on success', async () => {
    const user: UserResponse = {
      id: 1,
      email: 'user@example.com',
      username: 'user',
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(user), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      login({ username: 'user', password: 'password123' }),
    ).resolves.toEqual(user)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/auth/login'),
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({ username: 'user', password: 'password123' }),
      }),
    )
  })

  it('rethrows ApiError on non-2xx responses', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            status: 401,
            message: 'Invalid credentials',
          }),
          {
            status: 401,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
      ),
    )

    await expect(
      login({ username: 'user', password: 'wrong' }),
    ).rejects.toMatchObject({
      status: 401,
      message: 'Invalid credentials',
    })
  })
})

describe('register', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns the user on success (201)', async () => {
    const user: UserResponse = {
      id: 2,
      email: 'new@example.com',
      username: 'newuser',
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(user), {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      register({
        email: 'new@example.com',
        username: 'newuser',
        password: 'password123',
      }),
    ).resolves.toEqual(user)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/auth/register'),
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({
          email: 'new@example.com',
          username: 'newuser',
          password: 'password123',
        }),
      }),
    )
  })

  it('rethrows ApiError on validation failure (400)', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            status: 400,
            message: 'Validation failed',
            details: { username: ['Username already taken'] },
          }),
          {
            status: 400,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
      ),
    )

    await expect(
      register({
        email: 'new@example.com',
        username: 'taken',
        password: 'password123',
      }),
    ).rejects.toMatchObject({
      status: 400,
      message: 'Validation failed',
      details: { username: ['Username already taken'] },
    })
  })
})
