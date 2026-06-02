import { describe, expect, it } from 'vitest'

import { queryClient } from '@/lib/queryClient'

describe('queryClient', () => {
  it('exports a QueryClient instance with default options', () => {
    expect(queryClient).toBeDefined()
    expect(queryClient.getDefaultOptions().queries?.retry).toBe(1)
  })
})
