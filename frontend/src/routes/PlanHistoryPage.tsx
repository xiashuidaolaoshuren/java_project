import { useState } from 'react'

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { PlanHistoryList } from '@/features/plans/PlanHistoryList'
import { usePlans } from '@/features/plans/hooks'

const PAGE_SIZE = 20

export function PlanHistoryPage() {
  const [page, setPage] = useState(0)
  const { plans, page: pageEnvelope, isPending, isError, refetch } = usePlans(
    page,
    PAGE_SIZE,
  )

  const totalPages = pageEnvelope?.totalPages ?? 0
  const totalElements = pageEnvelope?.totalElements ?? 0
  const hasPrevious = page > 0
  const hasNext = page + 1 < totalPages

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
            page={page}
            size={PAGE_SIZE}
            totalPages={totalPages}
            totalElements={totalElements}
            hasPrevious={hasPrevious}
            hasNext={hasNext}
            onPageChange={setPage}
            isLoading={isPending}
            isError={isError}
            onRetry={refetch}
          />
        </CardContent>
      </Card>
    </div>
  )
}
