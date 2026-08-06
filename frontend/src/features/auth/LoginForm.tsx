import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useLogin } from '@/features/auth/hooks'

export function LoginForm() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const { mutate, isPending, isError, error } = useLogin()
  const errorAlertRef = useRef<HTMLDivElement>(null)
  const hasLoginError = isError && error instanceof Error

  useEffect(() => {
    if (hasLoginError) {
      errorAlertRef.current?.focus()
    }
  }, [hasLoginError])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutate({ username, password })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Sign in</CardTitle>
        <CardDescription>
          Sign in to manage tasks and daily plans.
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit}>
        <CardContent className="flex flex-col gap-4">
          {hasLoginError ? (
            <Alert
              ref={errorAlertRef}
              variant="destructive"
              tabIndex={-1}
            >
              <AlertTitle>Sign-in failed</AlertTitle>
              <AlertDescription>{error.message}</AlertDescription>
            </Alert>
          ) : null}
          <div className="flex flex-col gap-2">
            <Label htmlFor="username">Username</Label>
            <Input
              id="username"
              name="username"
              autoComplete="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              aria-invalid={hasLoginError}
              required
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              aria-invalid={hasLoginError}
              required
            />
          </div>
        </CardContent>
        <CardFooter className="flex flex-col gap-4 border-t-0 bg-transparent p-4 pt-0">
          <Button type="submit" className="w-full" disabled={isPending}>
            Sign in
          </Button>
          <p className="text-center text-sm text-muted-foreground">
            Don&apos;t have an account?{' '}
            <Link to="/register" className="text-primary underline-offset-4 hover:underline">
              Register
            </Link>
          </p>
        </CardFooter>
      </form>
    </Card>
  )
}
