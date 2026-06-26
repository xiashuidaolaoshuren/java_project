import { Link, useParams } from 'react-router-dom'

import {
  Alert,
  AlertAction,
  AlertDescription,
  AlertTitle,
} from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { DailyPlanView } from '@/features/plans/DailyPlanView'
import { usePlan } from '@/features/plans/hooks'
import { ApiError } from '@/lib/api'

function parsePlanId(idParam: string | undefined): number {
  if (idParam == null) {
    return Number.NaN
  }

  const parsed = Number.parseInt(idParam, 10)
  return Number.isNaN(parsed) ? Number.NaN : parsed
}

function PlanDetailSkeleton() {
  return (
    <div
      role="status"
      aria-label="Loading plan"
      className="flex flex-col gap-4"
    >
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-40 w-full rounded-xl" />
    </div>
  )
}

function PlanNotFoundState() {
  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Plan not found</h1>
        <p className="text-sm text-muted-foreground">
          This saved plan does not exist or is no longer available.
        </p>
      </div>
      <Link
        to="/plans"
        className="inline-flex h-8 w-fit items-center justify-center rounded-lg border border-border bg-background px-2.5 text-sm font-medium hover:bg-muted"
      >
        Back to plan history
      </Link>
    </div>
  )
}

export function PlanDetailPage() {
  const { id: idParam } = useParams<{ id: string }>()
  const planId = parsePlanId(idParam)
  const { plan, isPending, isError, error, refetch } = usePlan(planId)

  if (!Number.isInteger(planId) || planId <= 0) {
    return <PlanNotFoundState />
  }

  if (isPending) {
    return <PlanDetailSkeleton />
  }

  if (isError) {
    if (error instanceof ApiError && error.status === 404) {
      return <PlanNotFoundState />
    }

    return (
      <Alert variant="destructive">
        <AlertTitle>Could not load plan</AlertTitle>
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

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Plan detail</h1>
        <p className="text-sm text-muted-foreground">
          Saved plan for {plan?.planDate ?? 'unknown date'}.
        </p>
      </div>
      <DailyPlanView plan={plan} title={`Plan for ${plan?.planDate ?? ''}`} />
      <Link
        to="/plans"
        className="inline-flex h-8 w-fit items-center justify-center rounded-lg border border-border bg-background px-2.5 text-sm font-medium hover:bg-muted"
      >
        Back to plan history
      </Link>
    </div>
  )
}
