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
import { TaskActions } from '@/features/tasks/TaskActions'
import { useDeleteTask, useTasks, useUpdateTask } from '@/features/tasks/hooks'
import type { TaskResponse, TaskStatus, UpdateTaskRequest } from '@/types/api'

function formatDueDate(dueDate: string | null): string {
  return dueDate ?? 'No due date'
}

function formatEstimatedMinutes(minutes: number | null): string {
  return minutes != null ? `${minutes} min` : 'No estimate'
}

function toUpdateRequest(
  task: TaskResponse,
  overrides: Partial<UpdateTaskRequest> = {},
): UpdateTaskRequest {
  return {
    title: task.title,
    description: task.description,
    priority: task.priority,
    status: task.status,
    dueDate: task.dueDate,
    estimatedMinutes: task.estimatedMinutes,
    ...overrides,
  }
}

type TaskRowProps = {
  task: TaskResponse
  onEditTask: (task: TaskResponse) => void
  onStatusChange: (task: TaskResponse, status: TaskStatus) => void
  onDelete: (task: TaskResponse) => void
  isUpdating: boolean
  isDeleting: boolean
}

function TaskRow({
  task,
  onEditTask,
  onStatusChange,
  onDelete,
  isUpdating,
  isDeleting,
}: TaskRowProps) {
  return (
    <Card size="sm">
      <CardContent className="flex flex-col gap-2 py-3">
        <div className="flex items-start justify-between gap-3">
          <h3 className="font-medium">{task.title}</h3>
          <TaskActions
            task={task}
            onEdit={onEditTask}
            onStatusChange={onStatusChange}
            onDelete={onDelete}
            isUpdating={isUpdating}
            isDeleting={isDeleting}
          />
        </div>
        <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
          <span>{task.priority}</span>
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
          Click New Task to create your first one.
        </p>
      </div>
    </div>
  )
}

type TaskListProps = {
  onEditTask?: (task: TaskResponse) => void
}

export function TaskList({ onEditTask = () => undefined }: TaskListProps) {
  const { isPending, isError, isEmpty, tasks, error, refetch } = useTasks()
  const { mutate: updateTask, isPending: isUpdating } = useUpdateTask()
  const { mutate: deleteTask, isPending: isDeleting } = useDeleteTask()

  function handleStatusChange(task: TaskResponse, status: TaskStatus) {
    updateTask({
      id: task.id,
      request: toUpdateRequest(task, { status }),
    })
  }

  function handleDelete(task: TaskResponse) {
    deleteTask(task.id)
  }

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
        <TaskRow
          key={task.id}
          task={task}
          onEditTask={onEditTask}
          onStatusChange={handleStatusChange}
          onDelete={handleDelete}
          isUpdating={isUpdating}
          isDeleting={isDeleting}
        />
      ))}
    </div>
  )
}
