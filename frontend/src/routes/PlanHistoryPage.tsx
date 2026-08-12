import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { PlanHistoryList } from '@/features/plans/PlanHistoryList'
import { usePlans } from '@/features/plans/hooks'

export function PlanHistoryPage() {
  const { plans, isPending, isError, refetch } = usePlans()

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Plan history</h1>
        <p className="text-sm text-muted-foreground">
          Review your saved daily plans and open any plan for details.
        </p>
      </div>
      <Card>
        <CardHeader>
          <CardTitle>Saved plans</CardTitle>
          <CardDescription>
            Each entry links to the full plan timeline for that day.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <PlanHistoryList
            plans={plans}
            isLoading={isPending}
            isError={isError}
            onRetry={refetch}
          />
        </CardContent>
      </Card>
    </div>
  )
}
