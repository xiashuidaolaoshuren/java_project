import { useState, type FormEvent } from 'react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useCreateTask } from '@/features/tasks/hooks'
import { ApiError } from '@/lib/api'
import type { TaskPriority } from '@/types/api'

function getFieldError(
  error: Error | null,
  fieldName: string,
): string | undefined {
  if (!(error instanceof ApiError) || !error.details) {
    return undefined
  }
  return error.details[fieldName]?.[0]
}

type TaskFormProps = {
  onSuccess?: () => void
}

export function TaskForm({ onSuccess }: TaskFormProps) {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM')
  const [dueDate, setDueDate] = useState('')
  const [estimatedMinutes, setEstimatedMinutes] = useState('')
  const { mutate, isPending, isError, error } = useCreateTask()

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutate(
      {
        title,
        description: description || null,
        priority,
        dueDate: dueDate || null,
        estimatedMinutes:
          estimatedMinutes.trim() === ''
            ? null
            : Number.parseInt(estimatedMinutes, 10),
      },
      { onSuccess },
    )
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      {isError && error instanceof Error ? (
        <Alert variant="destructive">
          <AlertDescription>{error.message}</AlertDescription>
        </Alert>
      ) : null}
      <div className="flex flex-col gap-2">
        <Label htmlFor="title">Title</Label>
        <Input
          id="title"
          name="title"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          aria-invalid={Boolean(getFieldError(error, 'title'))}
          required
        />
        {isError && getFieldError(error, 'title') ? (
          <p className="text-sm text-destructive">
            {getFieldError(error, 'title')}
          </p>
        ) : null}
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor="description">Description</Label>
        <Input
          id="description"
          name="description"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          aria-invalid={Boolean(getFieldError(error, 'description'))}
        />
        {isError && getFieldError(error, 'description') ? (
          <p className="text-sm text-destructive">
            {getFieldError(error, 'description')}
          </p>
        ) : null}
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor="priority">Priority</Label>
        <select
          id="priority"
          name="priority"
          value={priority}
          onChange={(event) => setPriority(event.target.value as TaskPriority)}
          className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
        >
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
        </select>
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor="dueDate">Due date</Label>
        <Input
          id="dueDate"
          name="dueDate"
          type="date"
          value={dueDate}
          onChange={(event) => setDueDate(event.target.value)}
          aria-invalid={Boolean(getFieldError(error, 'dueDate'))}
        />
        {isError && getFieldError(error, 'dueDate') ? (
          <p className="text-sm text-destructive">
            {getFieldError(error, 'dueDate')}
          </p>
        ) : null}
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor="estimatedMinutes">Estimated minutes</Label>
        <Input
          id="estimatedMinutes"
          name="estimatedMinutes"
          type="number"
          min={1}
          value={estimatedMinutes}
          onChange={(event) => setEstimatedMinutes(event.target.value)}
          aria-invalid={Boolean(getFieldError(error, 'estimatedMinutes'))}
        />
        {isError && getFieldError(error, 'estimatedMinutes') ? (
          <p className="text-sm text-destructive">
            {getFieldError(error, 'estimatedMinutes')}
          </p>
        ) : null}
      </div>
      <Button type="submit" disabled={isPending}>
        Create task
      </Button>
    </form>
  )
}
