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
import { useRegister } from '@/features/auth/hooks'
import { ApiError } from '@/lib/api'

function getFieldError(
  error: Error | null,
  fieldName: string,
): string | undefined {
  if (!(error instanceof ApiError) || !error.details) {
    return undefined
  }
  return error.details[fieldName]?.[0]
}

export function RegisterForm() {
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const { mutate, isPending, isError, error } = useRegister()
  const errorAlertRef = useRef<HTMLDivElement>(null)
  const hasRegisterError = isError && error instanceof Error

  useEffect(() => {
    if (hasRegisterError) {
      errorAlertRef.current?.focus()
    }
  }, [hasRegisterError])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutate({ email, username, password })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Create account</CardTitle>
        <CardDescription>
          Create an account to start planning your focus time.
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit}>
        <CardContent className="flex flex-col gap-4">
          {hasRegisterError ? (
            <Alert
              ref={errorAlertRef}
              variant="destructive"
              tabIndex={-1}
            >
              <AlertTitle>Registration failed</AlertTitle>
              <AlertDescription>{error.message}</AlertDescription>
            </Alert>
          ) : null}
          <div className="flex flex-col gap-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              aria-invalid={Boolean(getFieldError(error, 'email'))}
              aria-describedby={
                getFieldError(error, 'email') ? 'email-error' : undefined
              }
              required
            />
            {isError && getFieldError(error, 'email') ? (
              <p id="email-error" className="text-sm text-destructive">
                {getFieldError(error, 'email')}
              </p>
            ) : null}
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="username">Username</Label>
            <Input
              id="username"
              name="username"
              autoComplete="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              aria-invalid={Boolean(getFieldError(error, 'username'))}
              aria-describedby={
                getFieldError(error, 'username') ? 'username-error' : undefined
              }
              required
            />
            {isError && getFieldError(error, 'username') ? (
              <p id="username-error" className="text-sm text-destructive">
                {getFieldError(error, 'username')}
              </p>
            ) : null}
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              name="password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              aria-invalid={Boolean(getFieldError(error, 'password'))}
              aria-describedby={
                getFieldError(error, 'password') ? 'password-error' : undefined
              }
              required
            />
            {isError && getFieldError(error, 'password') ? (
              <p id="password-error" className="text-sm text-destructive">
                {getFieldError(error, 'password')}
              </p>
            ) : null}
          </div>
        </CardContent>
        <CardFooter className="flex flex-col gap-4 border-t-0 bg-transparent p-4 pt-0">
          <Button type="submit" className="w-full" disabled={isPending}>
            Create account
          </Button>
          <p className="text-center text-sm text-muted-foreground">
            Already have an account?{' '}
            <Link to="/login" className="text-primary underline-offset-4 hover:underline">
              Sign in
            </Link>
          </p>
        </CardFooter>
      </form>
    </Card>
  )
}
