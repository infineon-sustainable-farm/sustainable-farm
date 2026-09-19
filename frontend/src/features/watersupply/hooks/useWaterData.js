import { useCallback, useEffect, useState } from 'react'
import {
  waterSourceApi,
  waterConsumptionApi,
  waterQualityApi,
  irrigationApi,
  notificationApi,
  rainwaterHarvestApi,
  dripMaintenanceApi,
} from '../api/watersupplyApi'

function useApiList(fetcher, dataKey) {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const normalize = useCallback((res) => (Array.isArray(res) ? res : res?.content ?? []), [])

  const refetch = useCallback(() => {
    setLoading(true)
    setError(null)
    return fetcher()
      .then((res) => {
        setData(normalize(res))
        return res
      })
      .catch((err) => {
        setError(err)
        throw err
      })
      .finally(() => setLoading(false))
  }, [fetcher, normalize])

  useEffect(() => {
    let cancelled = false
    fetcher()
      .then((res) => {
        if (!cancelled) {
          setData(normalize(res))
          setLoading(false)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err)
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [fetcher, normalize])

  return { [dataKey]: data, loading, error, refetch }
}

export function useWaterSources() {
  const fetcher = useCallback(() => waterSourceApi.getSources(), [])
  return useApiList(fetcher, 'sources')
}

export function useWaterConsumptions() {
  const fetcher = useCallback(() => waterConsumptionApi.getConsumptions(), [])
  return useApiList(fetcher, 'consumptions')
}

export function useWaterQualityTests() {
  const fetcher = useCallback(() => waterQualityApi.getTests(), [])
  return useApiList(fetcher, 'tests')
}

export function useIrrigationSchedules() {
  const fetcher = useCallback(() => irrigationApi.getSchedules(), [])
  return useApiList(fetcher, 'schedules')
}

export function useIrrigationLogs() {
  const fetcher = useCallback(() => irrigationApi.getLogs(), [])
  return useApiList(fetcher, 'logs')
}

export function useNotifications() {
  const fetcher = useCallback(() => notificationApi.getNotifications(), [])
  return useApiList(fetcher, 'notifications')
}

export function useRainwaterHarvests() {
  const fetcher = useCallback(() => rainwaterHarvestApi.getHarvests(), [])
  return useApiList(fetcher, 'harvests')
}

export function useDripMaintenanceLogs() {
  const fetcher = useCallback(() => dripMaintenanceApi.getLogs(), [])
  return useApiList(fetcher, 'logs')
}
