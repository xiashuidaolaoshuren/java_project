import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import { PublicLayout } from '@/components/layout/PublicLayout'

function renderPublicLayout() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <Routes>
        <Route element={<PublicLayout />}>
          <Route path="/login" element={<div>Login page</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('PublicLayout', () => {
  it('wraps the outlet content in a main landmark', () => {
    renderPublicLayout()

    expect(screen.getByRole('main')).toBeInTheDocument()
  })
})
