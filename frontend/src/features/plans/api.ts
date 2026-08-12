import { apiRequest } from '@/lib/api'
import type { DailyPlanResponse, GeneratePlanRequest } from '@/types/api'

function getTodayDateString(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export async function listPlans(): Promise<DailyPlanResponse[]> {
  return apiRequest<DailyPlanResponse[]>('/api/daily-plans')
}

export async function getPlanById(id: number): Promise<DailyPlanResponse> {
  return apiRequest<DailyPlanResponse>(`/api/daily-plans/${id}`)
}

export async function getTodayPlan(): Promise<DailyPlanResponse | null> {
  const planDate = getTodayDateString()
  const plans = await apiRequest<DailyPlanResponse[]>(
    `/api/daily-plans?planDate=${encodeURIComponent(planDate)}`,
  )
  return plans[0] ?? null
}

export async function generateDailyPlan(
  request: GeneratePlanRequest,
): Promise<DailyPlanResponse> {
  return apiRequest<DailyPlanResponse>('/api/daily-plans/generate', {
    method: 'POST',
    body: { planDate: getTodayDateString(), ...request },
  })
}
