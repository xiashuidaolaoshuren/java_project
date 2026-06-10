import { ListTodoIcon } from 'lucide-react'

import {
  Alert,
  AlertAction,
  AlertDescription,
  AlertTitle,
} from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useTasks } from '@/features/tasks/hooks'
import type { TaskResponse } from '@/types/api'

function formatDueDate(dueDate: string | null): string {
  return dueDate ?? 'No due date'
}

function formatEstimatedMinutes(minutes: number | null): string {
  return minutes != null ? `${minutes} min` : 'No estimate'
}

function TaskRow({ task }: { task: TaskResponse }) {
  return (
    <Card size="sm">
      <CardContent className="flex flex-col gap-2 py-3">
        <div className="flex items-start justify-between gap-3">
          <h3 className="font-medium">{task.title}</h3>
          <span className="text-xs font-medium text-muted-foreground">
            {task.priority}
          </span>
        </div>
        <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
          <span>{task.status}</span>
          <span>{formatDueDate(task.dueDate)}</span>
          <span>{formatEstimatedMinutes(task.estimatedMinutes)}</span>
        </div>
      </CardContent>
    </Card>
  )
}

function TaskListSkeleton() {
  return (
    <div
      role="status"
      aria-label="Loading tasks"
      className="flex flex-col gap-3"
    >
      {Array.from({ length: 3 }).map((_, index) => (
        <Skeleton
          key={index}
          data-testid="task-skeleton-row"
          className="h-20 w-full rounded-xl"
        />
      ))}
    </div>
  )
}

function TaskListEmpty() {
  return (
    <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-border px-6 py-10 text-center">
      <ListTodoIcon className="size-8 text-muted-foreground" aria-hidden />
      <div className="space-y-1">
        <p className="font-medium">No tasks yet</p>
        <p className="text-sm text-muted-foreground">
          Create your first one in a future update.
        </p>
      </div>
    </div>
  )
}

export function TaskList() {
  const { isPending, isError, isEmpty, tasks, error, refetch } = useTasks()

  if (isPending) {
    return <TaskListSkeleton />
  }

  if (isError) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Could not load tasks</AlertTitle>
        <AlertDescription>
          {error instanceof Error ? error.message : 'Something went wrong'}
        </AlertDescription>
        <AlertAction>
          <Button type="button" size="sm" variant="outline" onClick={() => refetch()}>
            Retry
          </Button>
        </AlertAction>
      </Alert>
    )
  }

  if (isEmpty) {
    return <TaskListEmpty />
  }

  return (
    <div className="flex flex-col gap-3">
      {tasks.map((task) => (
        <TaskRow key={task.id} task={task} />
      ))}
    </div>
  )
}
