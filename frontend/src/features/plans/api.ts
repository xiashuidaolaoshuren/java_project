import { apiRequest } from '@/lib/api'
import type {
  DailyPlanResponse,
  DailyPlanSummaryResponse,
  GeneratePlanRequest,
  PageResponse,
} from '@/types/api'

function getTodayDateString(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export async function listPlans(
  page: number,
  size: number,
): Promise<PageResponse<DailyPlanSummaryResponse>> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  })
  return apiRequest<PageResponse<DailyPlanSummaryResponse>>(
    `/api/daily-plans?${params}`,
  )
}

export async function getPlanById(id: number): Promise<DailyPlanResponse> {
  return apiRequest<DailyPlanResponse>(`/api/daily-plans/${id}`)
}

export async function getTodayPlan(): Promise<DailyPlanResponse | null> {
  const planDate = getTodayDateString()
  const plan = await apiRequest<DailyPlanResponse | undefined>(
    `/api/daily-plans/latest?planDate=${encodeURIComponent(planDate)}`,
  )
  return plan ?? null
}

export async function generateDailyPlan(
  request: GeneratePlanRequest,
): Promise<DailyPlanResponse> {
  return apiRequest<DailyPlanResponse>('/api/daily-plans/generate', {
    method: 'POST',
    body: { planDate: getTodayDateString(), ...request },
  })
}

export async function deletePlan(id: number): Promise<void> {
  return apiRequest<void>(`/api/daily-plans/${id}`, {
    method: 'DELETE',
  })
}
