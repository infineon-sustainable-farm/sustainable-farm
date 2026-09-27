import { waterQuotaApi } from '../api/watersupplyApi'
import { useModuleQuery } from './useModuleQuery'

/**
 * Suivi des quotas mensuels d'eau (P8) : pour chaque quota du mois courant,
 * la consommation cumulee des capteurs, le pourcentage utilise et le statut
 * (ok / warning a 80 % / exceeded a 100 %).
 */
export function useQuotaUsage() {
  const { data, loading, error, refetch } = useModuleQuery(['watersupply', 'quota-usage'], waterQuotaApi.getUsage)
  const usage = Array.isArray(data) ? data : data?.content ?? []
  return { usage, loading, error, refetch }
}

