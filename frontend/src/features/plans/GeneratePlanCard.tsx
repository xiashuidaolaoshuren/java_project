import { useState, type FormEvent } from 'react'
import { toast } from 'sonner'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useGeneratePlan } from '@/features/plans/hooks'
import { ApiError } from '@/lib/api'

export function GeneratePlanCard() {
  const [availableMinutes, setAvailableMinutes] = useState('60')
  const { mutate, isPending, isError, error } = useGeneratePlan()

  const showProviderError =
    isError && error instanceof ApiError && error.status === 502

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutate(
      { availableMinutes: Number.parseInt(availableMinutes, 10) },
      {
        onSuccess: () => {
          toast.success("Today's plan generated")
        },
      },
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Generate Today&apos;s Plan</CardTitle>
        <CardDescription>
          Enter how many minutes you have available for focused work today.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          {showProviderError && (
            <Alert variant="destructive">
              <AlertDescription>{error.message}</AlertDescription>
            </Alert>
          )}
          <div className="flex flex-col gap-2">
            <Label htmlFor="available-minutes">Available focus time (minutes)</Label>
            <Input
              id="available-minutes"
              type="number"
              min={1}
              required
              value={availableMinutes}
              onChange={(event) => setAvailableMinutes(event.target.value)}
            />
          </div>
          <Button type="submit" disabled={isPending}>
            {isPending ? 'Generating...' : "Generate today's plan"}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
