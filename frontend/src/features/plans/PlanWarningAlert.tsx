import {
  Alert,
  AlertDescription,
  AlertTitle,
} from '@/components/ui/alert'
import type { DailyPlanWarning } from '@/types/api'

type PlanWarningAlertProps = {
  warning: DailyPlanWarning
}

export function PlanWarningAlert({ warning }: PlanWarningAlertProps) {
  return (
    <Alert variant="default" className="mb-3">
      <AlertTitle>Plan needs more focus time</AlertTitle>
      <AlertDescription>
        <p>
          Must-include tasks need at least {warning.minimumAvailableMinutes} min
          of focus time, which exceeded the time available for this plan.
        </p>
        <ul className="mt-1 list-disc pl-4">
          {warning.estimatedTasks.map((task) => (
            <li key={task.taskId}>
              {task.title} — {task.estimatedMinutes} min
            </li>
          ))}
          {warning.unestimatedTasks.map((task) => (
            <li key={task.taskId}>{task.title} — unknown duration</li>
          ))}
        </ul>
      </AlertDescription>
    </Alert>
  )
}
