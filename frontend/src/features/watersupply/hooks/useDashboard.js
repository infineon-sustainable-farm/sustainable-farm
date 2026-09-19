import { useEffect, useState } from 'react'
import { dashboardApi, aiApi, weatherApi, healthApi, irrigationApi } from '../api/watersupplyApi'

/**
 * Hook pour récupérer les KPIs du dashboard.
 */
export function useKpis() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    dashboardApi
      .getKpis()
      .then((res) => {
        if (!cancelled) {
          setData(res)
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

  return { data, loading, error }
}

/**
 * Hook pour récupérer les alertes du dashboard.
 */
export function useAlerts() {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    dashboardApi
      .getAlerts()
      .then((res) => {
        if (!cancelled) {
          setData(res)
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

  return { data, loading, error }
}

/**
 * Hook pour récupérer les activités du dashboard.
 */
export function useActivities() {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    dashboardApi
      .getActivities()
      .then((res) => {
        if (!cancelled) {
          setData(res)
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

  return { data, loading, error }
}

/**
 * Hook pour récupérer les recommandations IA.
 */
export function useRecommendations() {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    aiApi
      .getRecommendations()
      .then((res) => {
        if (!cancelled) {
          setData(res)
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

  return { data, loading, error }
}

/**
 * Hook pour récupérer la prédiction de sécheresse.
 */
export function useDroughtPrediction() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    aiApi
      .getDroughtPrediction()
      .then((res) => {
        if (!cancelled) {
          setData(res)
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

  return { data, loading, error }
}

/**
 * Hook pour récupérer la météo actuelle.
 */
export function useWeather(latitude, longitude) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    weatherApi
      .getCurrent(latitude, longitude)
      .then((res) => {
        if (!cancelled) {
          setData(res)
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
  }, [latitude, longitude])

  return { data, loading, error }
}

/**
 * Hook pour vérifier la santé du backend.
 */
export function useHealthCheck() {
  const [healthy, setHealthy] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    healthApi
      .check()
      .then(() => {
        if (!cancelled) {
          setHealthy(true)
          setLoading(false)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setHealthy(false)
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  return { healthy, loading }
}

/**
 * Hook pour la série cumulée de l'économie d'eau : c'est la courbe de progression
 * (besoin cumulé des cultures vs consommation cumulée mesurée par les capteurs).
 * @param {number} days - taille de la fenêtre (1 à 60 jours).
 */
export function useSavingsSeries(days = 30) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    dashboardApi
      .getSavingsSeries(days)
      .then((res) => {
        if (!cancelled) {
          setData(res)
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
  }, [days])

  return { data, loading, error }
}

/**
 * Hook pour le bilan hydrique : entrées (pluie récupérée) contre sorties (eau consommée),
 * comparées au niveau des réservoirs. Rend visibles les pertes.
 */
export function useWaterBalance(period = 'month') {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    dashboardApi
      .getWaterBalance(period)
      .then((res) => {
        if (!cancelled) {
          setData(res)
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
  }, [period])

  return { data, loading, error }
}

/**
 * Hook pour les anomalies de débit (fuites probables). Diagnostic calculé côté backend
 * sur les mesures de consommation.
 */
export function useLeaks() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    dashboardApi
      .getLeaks()
      .then((res) => {
        if (!cancelled) {
          setData(res)
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

  return { data, loading, error }
}

/**
 * Hook pour les suggestions de report d'irrigation fondées sur la pluie prévue.
 * Le backend propose, l'utilisateur décide via irrigationApi.postpone().
 */
export function useIrrigationSuggestions() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    irrigationApi
      .getSuggestions()
      .then((res) => {
        if (!cancelled) {
          setData(res)
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

  return { data, loading, error }
}
