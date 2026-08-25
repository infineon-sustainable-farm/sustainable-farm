import { useEffect, useState } from 'react'
import {
  waterSourceApi,
  waterConsumptionApi,
  waterQualityApi,
  irrigationApi,
  notificationApi,
  rainwaterHarvestApi,
  dripMaintenanceApi,
} from '../api/watersupplyApi'

/**
 * Hook pour récupérer les sources d'eau.
 */
export function useWaterSources() {
  const [sources, setSources] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    waterSourceApi
      .getSources()
      .then((res) => {
        if (!cancelled) {
          setSources(res)
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
  }, [])

  return { sources, loading, error }
}

/**
 * Hook pour récupérer les consommations d'eau.
 */
export function useWaterConsumptions() {
  const [consumptions, setConsumptions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    waterConsumptionApi
      .getConsumptions()
      .then((res) => {
        if (!cancelled) {
          setConsumptions(res)
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
  }, [])

  return { consumptions, loading, error }
}

/**
 * Hook pour récupérer les tests de qualité de l'eau.
 */
export function useWaterQualityTests() {
  const [tests, setTests] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    waterQualityApi
      .getTests()
      .then((res) => {
        if (!cancelled) {
          setTests(res)
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
  }, [])

  return { tests, loading, error }
}

/**
 * Hook pour récupérer les plannings d'irrigation.
 */
export function useIrrigationSchedules() {
  const [schedules, setSchedules] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    irrigationApi
      .getSchedules()
      .then((res) => {
        if (!cancelled) {
          setSchedules(res)
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
  }, [])

  return { schedules, loading, error }
}

/**
 * Hook pour récupérer les logs d'irrigation.
 */
export function useIrrigationLogs() {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    irrigationApi
      .getLogs()
      .then((res) => {
        if (!cancelled) {
          setLogs(res)
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
  }, [])

  return { logs, loading, error }
}

/**
 * Hook pour récupérer les notifications.
 */
export function useNotifications() {
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fetchNotifications = () => {
    setLoading(true)
    notificationApi
      .getNotifications()
      .then((res) => {
        setNotifications(res)
        setLoading(false)
      })
      .catch((err) => {
        setError(err)
        setLoading(false)
      })
  }

  useEffect(() => {
    fetchNotifications()
  }, [])

  return { notifications, loading, error, refetch: fetchNotifications }
}

/**
 * Hook pour récupérer les récoltes d'eau de pluie.
 */
export function useRainwaterHarvests() {
  const [harvests, setHarvests] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    rainwaterHarvestApi
      .getHarvests()
      .then((res) => {
        if (!cancelled) {
          setHarvests(res)
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
  }, [])

  return { harvests, loading, error }
}

/**
 * Hook pour récupérer les logs de maintenance goutte-à-goutte.
 */
export function useDripMaintenanceLogs() {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    dripMaintenanceApi
      .getLogs()
      .then((res) => {
        if (!cancelled) {
          setLogs(res)
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
  }, [])

  return { logs, loading, error }
}
