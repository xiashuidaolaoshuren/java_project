import { cva } from 'class-variance-authority'

import { Badge } from '@/components/ui/badge'
import { STATUS_LABELS } from '@/features/tasks/labels'
import type { TaskStatus } from '@/types/api'

const statusBadgeVariants = cva('border', {
  variants: {
    status: {
      OPEN: 'border-border bg-status-open/15 text-status-open-foreground',
      IN_PROGRESS:
        'border-status-in-progress/30 bg-status-in-progress/15 text-status-in-progress-foreground',
      DONE: 'border-status-done/30 bg-status-done/15 text-status-done-foreground',
      CANCELLED:
        'border-border bg-status-cancelled/15 text-status-cancelled-foreground',
    },
  },
})

type StatusBadgeProps = {
  status: TaskStatus
}

export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <Badge
      variant="outline"
      className={statusBadgeVariants({ status })}
      data-status={status}
    >
      {STATUS_LABELS[status]}
    </Badge>
  )
}
