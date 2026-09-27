import {
  waterSourceApi,
  waterConsumptionApi,
  waterQualityApi,
  irrigationApi,
  notificationApi,
  rainwaterHarvestApi,
  dripMaintenanceApi,
} from '../api/watersupplyApi'
import { useModuleQuery } from './useModuleQuery'

/**
 * Listes du module, servies par React Query via {@link useModuleQuery}.
 * Une reponse paginee est ramenee a son tableau `content` : les vues consomment une liste.
 */
function useListQuery(key, fetcher) {
  const { data, loading, error, refetch } = useModuleQuery(['watersupply', key], fetcher)
  const list = Array.isArray(data) ? data : data?.content ?? []
  return { list, loading, error, refetch }
}

export function useWaterSources() {
  const { list, loading, error, refetch } = useListQuery('sources', waterSourceApi.getSources)
  return { sources: list, loading, error, refetch }
}

export function useWaterConsumptions() {
  const { list, loading, error, refetch } = useListQuery('consumptions', waterConsumptionApi.getConsumptions)
  return { consumptions: list, loading, error, refetch }
}

export function useWaterQualityTests() {
  const { list, loading, error, refetch } = useListQuery('quality-tests', waterQualityApi.getTests)
  return { tests: list, loading, error, refetch }
}

export function useIrrigationSchedules() {
  const { list, loading, error, refetch } = useListQuery('irrigation-schedules', irrigationApi.getSchedules)
  return { schedules: list, loading, error, refetch }
}

export function useIrrigationLogs() {
  const { list, loading, error, refetch } = useListQuery('irrigation-logs', irrigationApi.getLogs)
  return { logs: list, loading, error, refetch }
}

export function useNotifications() {
  const { list, loading, error, refetch } = useListQuery('notifications', notificationApi.getNotifications)
  return { notifications: list, loading, error, refetch }
}

export function useRainwaterHarvests() {
  const { list, loading, error, refetch } = useListQuery('rainwater-harvests', rainwaterHarvestApi.getHarvests)
  return { harvests: list, loading, error, refetch }
}

export function useDripMaintenanceLogs() {
  const { list, loading, error, refetch } = useListQuery('drip-maintenance-logs', dripMaintenanceApi.getLogs)
  return { logs: list, loading, error, refetch }
}

