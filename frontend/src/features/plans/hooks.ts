import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { generateDailyPlan, getPlanById, getTodayPlan, listPlans } from '@/features/plans/api'
import type { GeneratePlanRequest } from '@/types/api'

export const todayPlanQueryKey = ['plans', 'today'] as const
export const plansQueryKey = ['plans', 'list'] as const

export function planDetailQueryKey(id: number) {
  return ['plans', 'detail', id] as const
}

function isValidPlanId(id: number): boolean {
  return Number.isInteger(id) && id > 0
}

export function useTodayPlan() {
  const query = useQuery({
    queryKey: todayPlanQueryKey,
    queryFn: getTodayPlan,
  })

  return {
    ...query,
    plan: query.data ?? null,
    hasPlan: query.data != null,
  }
}

export function usePlans() {
  const query = useQuery({
    queryKey: plansQueryKey,
    queryFn: listPlans,
  })

  return {
    ...query,
    plans: query.data ?? [],
    isEmpty: !query.isPending && (query.data?.length ?? 0) === 0,
  }
}

export function usePlan(id: number) {
  const query = useQuery({
    queryKey: planDetailQueryKey(id),
    queryFn: () => getPlanById(id),
    enabled: isValidPlanId(id),
  })

  return {
    ...query,
    plan: query.data ?? null,
  }
}

export function useGeneratePlan() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: GeneratePlanRequest) => generateDailyPlan(request),
    onSuccess: async (plan) => {
      queryClient.setQueryData(todayPlanQueryKey, plan)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: todayPlanQueryKey }),
        queryClient.invalidateQueries({ queryKey: plansQueryKey }),
      ])
    },
  })
}
