import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { generateDailyPlan, getTodayPlan, listPlans } from '@/features/plans/api'
import type { GeneratePlanRequest } from '@/types/api'

export const todayPlanQueryKey = ['plans', 'today'] as const
export const plansQueryKey = ['plans', 'list'] as const

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

export function useGeneratePlan() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: GeneratePlanRequest) => generateDailyPlan(request),
    onSuccess: async (plan) => {
      queryClient.setQueryData(todayPlanQueryKey, plan)
      await queryClient.invalidateQueries({ queryKey: todayPlanQueryKey })
    },
  })
}
