import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

export function PlanHistoryPage() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Plan history</CardTitle>
        <CardDescription>
          Saved daily plans will be listed here in F13.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">Plan history placeholder</p>
      </CardContent>
    </Card>
  )
}
