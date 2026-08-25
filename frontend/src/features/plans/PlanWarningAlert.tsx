import {
  Alert,
  AlertDescription,
  AlertTitle,
} from '@/components/ui/alert'
import type { DailyPlanWarning } from '@/types/api'

type PlanWarningAlertProps = {
  warning: DailyPlanWarning
  availableMinutes: number | null
}

function buildLeadSentence(
  warning: DailyPlanWarning,
  availableMinutes: number | null,
): string {
  const hasUnestimated = warning.unestimatedTasks.length > 0
  const timeExceeded =
    availableMinutes != null &&
    warning.minimumAvailableMinutes > availableMinutes

  if (timeExceeded && hasUnestimated) {
    return `Must-include tasks need at least ${warning.minimumAvailableMinutes} min of focus time, which exceeded the time available, and some have unknown duration.`
  }
  if (timeExceeded) {
    return `Must-include tasks need at least ${warning.minimumAvailableMinutes} min of focus time, which exceeded the time available for this plan.`
  }
  if (hasUnestimated) {
    return 'Some must-include tasks have unknown duration, so the total focus time needed is uncertain.'
  }
  return `Must-include tasks need at least ${warning.minimumAvailableMinutes} min of focus time, which exceeded the time available for this plan.`
}

export function PlanWarningAlert({
  warning,
  availableMinutes,
}: PlanWarningAlertProps) {
  return (
    <Alert variant="default" className="mb-3">
      <AlertTitle>Plan needs more focus time</AlertTitle>
      <AlertDescription>
        <p>{buildLeadSentence(warning, availableMinutes)}</p>
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
