import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

export function RegisterPage() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Register</CardTitle>
        <CardDescription>
          Create an account to use FocusFlow. Registration forms arrive in F7.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">Register placeholder</p>
      </CardContent>
    </Card>
  )
}
