import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import type { DailyPlanResponse } from '@/types/api'

type DailyPlanViewProps = {
  plan: DailyPlanResponse | null
}

export function DailyPlanView({ plan }: DailyPlanViewProps) {
  if (plan == null) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Today&apos;s plan</CardTitle>
          <CardDescription>No plan for today yet.</CardDescription>
        </CardHeader>
      </Card>
    )
  }

  const sortedItems = [...plan.items].sort((a, b) => a.position - b.position)

  return (
    <Card>
      <CardHeader>
        <CardTitle>Today&apos;s plan</CardTitle>
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
