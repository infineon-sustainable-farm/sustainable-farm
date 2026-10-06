import { waterQuotaApi } from '../api/watersupplyApi'
import { useModuleQuery } from './useModuleQuery'

/**
 * Tracking of the monthly water quotas (P8): for each quota of the current month,
 * the cumulative sensor consumption, the used percentage and the status
 * (ok / warning at 80% / exceeded at 100%).
 */
export function useQuotaUsage() {
  const { data, loading, error, refetch } = useModuleQuery(['watersupply', 'quota-usage'], waterQuotaApi.getUsage)
  const usage = Array.isArray(data) ? data : data?.content ?? []
  return { usage, loading, error, refetch }
}

