import { cva } from 'class-variance-authority'

import { Badge } from '@/components/ui/badge'
import { PRIORITY_LABELS } from '@/features/tasks/labels'
import type { TaskPriority } from '@/types/api'

const priorityBadgeVariants = cva('border', {
  variants: {
    priority: {
      LOW: 'border-border bg-priority-low/15 text-priority-low-foreground',
      MEDIUM:
        'border-priority-medium/30 bg-priority-medium/15 text-priority-medium-foreground',
      HIGH: 'border-destructive/30 bg-destructive/10 text-destructive',
    },
  },
})

type PriorityBadgeProps = {
  priority: TaskPriority
}

export function PriorityBadge({ priority }: PriorityBadgeProps) {
  return (
    <Badge
      variant="outline"
      className={priorityBadgeVariants({ priority })}
      data-priority={priority}
    >
      {PRIORITY_LABELS[priority]}
    </Badge>
  )
}
