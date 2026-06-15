import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import type { TaskResponse } from '@/types/api'
import { TaskActions } from '@/features/tasks/TaskActions'

const sampleTask: TaskResponse = {
  id: 1,
  title: 'Write report',
  description: 'Quarterly summary',
  priority: 'HIGH',
  status: 'OPEN',
  dueDate: '2026-06-15',
  estimatedMinutes: 60,
}

function renderTaskActions(
  overrides: Partial<{
    onEdit: (task: TaskResponse) => void
    onStatusChange: (task: TaskResponse, status: TaskResponse['status']) => void
    onDelete: (task: TaskResponse) => void
    isUpdating: boolean
    isDeleting: boolean
  }> = {},
) {
  const onEdit = overrides.onEdit ?? vi.fn()
  const onStatusChange = overrides.onStatusChange ?? vi.fn()
  const onDelete = overrides.onDelete ?? vi.fn()

  render(
    <TaskActions
      task={sampleTask}
      onEdit={onEdit}
      onStatusChange={onStatusChange}
      onDelete={onDelete}
      isUpdating={overrides.isUpdating ?? false}
      isDeleting={overrides.isDeleting ?? false}
    />,
  )

  return { onEdit, onStatusChange, onDelete }
}

describe('TaskActions', () => {
  it('calls onStatusChange when quick status control changes', () => {
    const onStatusChange = vi.fn()
    renderTaskActions({ onStatusChange })

    fireEvent.change(screen.getByLabelText(/change status for write report/i), {
      target: { value: 'IN_PROGRESS' },
    })

    expect(onStatusChange).toHaveBeenCalledWith(sampleTask, 'IN_PROGRESS')
  })

  it('calls onEdit when edit action is clicked', async () => {
    const onEdit = vi.fn()
    renderTaskActions({ onEdit })

    fireEvent.click(screen.getByRole('button', { name: /task actions for write report/i }))
    fireEvent.click(await screen.findByRole('menuitem', { name: /edit/i }))

    expect(onEdit).toHaveBeenCalledWith(sampleTask)
  })

  it('does not call onDelete until delete is confirmed', async () => {
    const onDelete = vi.fn()
    renderTaskActions({ onDelete })

    fireEvent.click(screen.getByRole('button', { name: /task actions for write report/i }))
    fireEvent.click(await screen.findByRole('menuitem', { name: /delete/i }))

    expect(onDelete).not.toHaveBeenCalled()
    expect(
      screen.getByRole('alertdialog', { name: /delete task/i }),
    ).toBeInTheDocument()
  })

  it('calls onDelete when delete confirmation is accepted', async () => {
    const onDelete = vi.fn()
    renderTaskActions({ onDelete })

    fireEvent.click(screen.getByRole('button', { name: /task actions for write report/i }))
    fireEvent.click(await screen.findByRole('menuitem', { name: /delete/i }))
    fireEvent.click(screen.getByRole('button', { name: /^delete task$/i }))

    expect(onDelete).toHaveBeenCalledWith(sampleTask)
  })

  it('disables actions while update or delete is in flight', () => {
    renderTaskActions({ isUpdating: true, isDeleting: true })

    expect(
      screen.getByLabelText(/change status for write report/i),
    ).toBeDisabled()
    expect(
      screen.getByRole('button', { name: /task actions for write report/i }),
    ).toBeDisabled()
  })
})
