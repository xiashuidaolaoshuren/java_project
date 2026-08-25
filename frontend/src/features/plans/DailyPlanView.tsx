import { CircleDotIcon, ClockIcon, FlagIcon } from 'lucide-react'

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Alert,
  AlertAction,
  AlertDescription,
  AlertTitle,
} from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import type { DailyPlanResponse } from '@/types/api'
import { PlanWarningAlert } from '@/features/plans/PlanWarningAlert'
import { PriorityBadge } from '@/features/tasks/PriorityBadge'
import { StatusBadge } from '@/features/tasks/StatusBadge'
import { TaskMetaItem } from '@/features/tasks/TaskMetaItem'

type DailyPlanViewProps = {
  plan: DailyPlanResponse | null
  title?: string
  emptyDescription?: string
  isPending?: boolean
  isError?: boolean
  onRetry?: () => void
}

function DailyPlanViewSkeleton({ title }: { title: string }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <CardDescription>
          <Skeleton className="h-4 w-32" />
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div
          role="status"
          aria-label="Loading plan"
          className="flex flex-col gap-3"
        >
          {Array.from({ length: 3 }).map((_, index) => (
            <Skeleton
              key={index}
              data-testid="plan-skeleton-row"
              className="h-12 w-full rounded-lg"
            />
          ))}
        </div>
      </CardContent>
    </Card>
  )
}

export function DailyPlanView({
  plan,
  title = "Today's plan",
  emptyDescription = 'No plan for today yet.',
  isPending = false,
  isError = false,
  onRetry,
}: DailyPlanViewProps) {
  if (isPending) {
    return <DailyPlanViewSkeleton title={title} />
  }

  if (isError) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Could not load plan</AlertTitle>
        <AlertDescription>
          Something went wrong while loading today&apos;s plan.
        </AlertDescription>
        <AlertAction>
          <Button type="button" size="sm" variant="outline" onClick={onRetry}>
            Retry
          </Button>
        </AlertAction>
      </Alert>
    )
  }

  if (plan == null) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{title}</CardTitle>
          <CardDescription>{emptyDescription}</CardDescription>
        </CardHeader>
      </Card>
    )
  }

  const sortedItems = [...plan.items].sort((a, b) => a.position - b.position)

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <CardDescription>
          {sortedItems.length} block{sortedItems.length === 1 ? '' : 's'} scheduled
        </CardDescription>
      </CardHeader>
      <CardContent>
        {plan.warning != null && <PlanWarningAlert warning={plan.warning} />}
        <ol className="flex flex-col gap-3">
          {sortedItems.map((item) => (
            <li
              key={`${item.position}-${item.task.id}`}
              className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2 rounded-lg border border-border px-3 py-2"
            >
              <span className="min-w-0 font-medium">{item.task.title}</span>
              <div className="flex min-w-0 flex-wrap items-center justify-end gap-x-2 gap-y-1">
                <TaskMetaItem category="priority" icon={FlagIcon}>
                  <PriorityBadge priority={item.task.priority} />
                </TaskMetaItem>
                <TaskMetaItem category="status" icon={CircleDotIcon}>
                  <StatusBadge status={item.task.status} />
                </TaskMetaItem>
                <TaskMetaItem category="estimatedMinutes" icon={ClockIcon}>
                  {item.task.estimatedMinutes ?? 0} min
                </TaskMetaItem>
              </div>
            </li>
          ))}
        </ol>
      </CardContent>
    </Card>
  )
}
