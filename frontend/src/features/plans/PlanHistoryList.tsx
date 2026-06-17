import { CalendarDaysIcon } from 'lucide-react'
import { Link } from 'react-router-dom'

import {
  Alert,
  AlertAction,
  AlertDescription,
  AlertTitle,
} from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import type { DailyPlanResponse } from '@/types/api'

type PlanHistoryListProps = {
  plans: DailyPlanResponse[]
  isLoading: boolean
  isError: boolean
  onRetry?: () => void
}

function PlanHistoryListSkeleton() {
  return (
    <div
      role="status"
      aria-label="Loading plans"
      className="flex flex-col gap-3"
    >
      {Array.from({ length: 3 }).map((_, index) => (
        <Skeleton
          key={index}
          data-testid="plan-skeleton-row"
          className="h-16 w-full rounded-xl"
        />
      ))}
    </div>
  )
}

function PlanHistoryListEmpty() {
  return (
    <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-border px-6 py-10 text-center">
      <CalendarDaysIcon className="size-8 text-muted-foreground" aria-hidden />
      <div className="space-y-1">
        <p className="font-medium">No saved plans yet</p>
        <p className="text-sm text-muted-foreground">
          Generate a daily plan from the dashboard to see it here.
        </p>
      </div>
    </div>
  )
}

function formatItemCount(count: number): string {
  return `${count} block${count === 1 ? '' : 's'}`
}

export function PlanHistoryList({
  plans,
  isLoading,
  isError,
  onRetry,
}: PlanHistoryListProps) {
  if (isLoading) {
    return <PlanHistoryListSkeleton />
  }

  if (isError) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Could not load plans</AlertTitle>
        <AlertDescription>Something went wrong while loading your plans.</AlertDescription>
        <AlertAction>
          <Button type="button" size="sm" variant="outline" onClick={onRetry}>
            Retry
          </Button>
        </AlertAction>
      </Alert>
    )
  }

  if (plans.length === 0) {
    return <PlanHistoryListEmpty />
  }

  return (
    <ul className="flex flex-col gap-3">
      {plans.map((plan) => (
        <li key={plan.id}>
          <Link
            to={`/plans/${plan.id}`}
            className="flex items-center justify-between rounded-xl border border-border px-4 py-3 transition-colors hover:bg-muted/50"
          >
            <span className="font-medium">{plan.planDate}</span>
            <span className="text-sm text-muted-foreground">
              {formatItemCount(plan.items.length)}
            </span>
          </Link>
        </li>
      ))}
    </ul>
  )
}
