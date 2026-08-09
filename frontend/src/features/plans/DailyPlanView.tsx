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
        <ol className="flex flex-col gap-3">
          {sortedItems.map((item) => (
            <li
              key={`${item.position}-${item.task.id}`}
              className="flex items-center justify-between rounded-lg border border-border px-3 py-2"
            >
              <span className="font-medium">{item.task.title}</span>
              <span className="text-sm text-muted-foreground">
                {item.task.estimatedMinutes ?? 0} min
              </span>
            </li>
          ))}
        </ol>
      </CardContent>
    </Card>
  )
}
