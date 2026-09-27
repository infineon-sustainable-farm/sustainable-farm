import { farmApi, fieldApi, zoneApi } from '../api/watersupplyApi'
import { useModuleQuery } from './useModuleQuery'

/**
 * Fermes, champs et zones : memes hooks qu'auparavant (contrat `{ donnee, loading, error }`),
 * servis par React Query. Les listes dependant d'un identifiant ne sont chargees que lorsque
 * cet identifiant est fourni (`enabled`).
 */
export function useFarms() {
  const { data, loading, error, refetch } = useModuleQuery(['watersupply', 'farms'], farmApi.getFarms)
  return { farms: Array.isArray(data) ? data : data?.content ?? [], loading, error, refetch }
}

export function useFarmFields(farmId) {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'farm-fields', farmId ?? 'none'],
    () => farmApi.getFarmFields(farmId),
    { enabled: Boolean(farmId) },
  )
  return { fields: Array.isArray(data) ? data : data?.content ?? [], loading, error }
}

export function useFieldZones(fieldId) {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'field-zones', fieldId ?? 'none'],
    () => fieldApi.getFieldZones(fieldId),
    { enabled: Boolean(fieldId) },
  )
  return { zones: Array.isArray(data) ? data : data?.content ?? [], loading, error }
}

export function useZones() {
  const { data, loading, error } = useModuleQuery(['watersupply', 'zones'], zoneApi.getZones)
  return { zones: Array.isArray(data) ? data : data?.content ?? [], loading, error }
}

