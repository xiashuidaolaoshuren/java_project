import { useState } from 'react'

import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet'
import { TaskForm } from '@/features/tasks/TaskForm'
import { TaskList } from '@/features/tasks/TaskList'

export function DashboardPage() {
  const [isCreateSheetOpen, setIsCreateSheetOpen] = useState(false)

  return (
    <div className="grid gap-6 md:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
      <section className="flex flex-col gap-4">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Tasks</h1>
            <p className="text-sm text-muted-foreground">
              Review your tasks and prepare for daily planning.
            </p>
          </div>
          <Button type="button" onClick={() => setIsCreateSheetOpen(true)}>
            New Task
          </Button>
        </div>
        <TaskList />
      </section>

      <Sheet open={isCreateSheetOpen} onOpenChange={setIsCreateSheetOpen}>
        <SheetContent>
          <SheetHeader>
            <SheetTitle>New task</SheetTitle>
            <SheetDescription>
              Add a task to your list and prepare it for planning.
            </SheetDescription>
          </SheetHeader>
          <TaskForm onSuccess={() => setIsCreateSheetOpen(false)} />
        </SheetContent>
      </Sheet>

      <aside>
        <Card>
          <CardHeader>
            <CardTitle>Plan generation</CardTitle>
            <CardDescription>
              Daily plan generation arrives in a later milestone.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">
              Generate today&apos;s plan from your open tasks once planning is
              available.
            </p>
          </CardContent>
        </Card>
      </aside>
    </div>
  )
}
