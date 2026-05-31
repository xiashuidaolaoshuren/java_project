import { useParams } from 'react-router-dom'

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

export function PlanDetailPage() {
  const { id } = useParams<{ id: string }>()

  return (
    <Card>
      <CardHeader>
        <CardTitle>Plan detail</CardTitle>
        <CardDescription>
          Viewing plan {id ?? 'unknown'}. Full detail view arrives in F14.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">Plan detail placeholder</p>
      </CardContent>
    </Card>
  )
}
