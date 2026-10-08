import { dashboardApi, aiApi, weatherApi, healthApi, irrigationApi } from '../api/watersupplyApi'
import { useModuleQuery } from './useModuleQuery'

/**
 * Dashboard data, served by React Query through {@link useModuleQuery}: the eleven
 * hooks below no longer each carry their own `useEffect` + `useState`, the loading
 * logic exists in exactly one place.
 *
 * The contract returned to the views is unchanged: `{ data, loading, error }` (plus
 * `refetch` where a view refreshes after an action).
 */

export function useKpis() {
  const { data, loading, error, refetch } = useModuleQuery(['watersupply', 'kpis'], dashboardApi.getKpis)
  return { data, loading, error, refetch }
}

export function useAlerts() {
  const { data, loading, error } = useModuleQuery(['watersupply', 'alerts'], dashboardApi.getAlerts)
  return { data: data ?? [], loading, error }
}

export function useActivities() {
  const { data, loading, error } = useModuleQuery(['watersupply', 'activities'], dashboardApi.getActivities)
  return { data: data ?? [], loading, error }
}

export function useRecommendations() {
  const { data, loading, error } = useModuleQuery(['watersupply', 'recommendations'], aiApi.getRecommendations)
  return { data: data ?? [], loading, error }
}

export function useDroughtPrediction() {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'drought-prediction'],
    aiApi.getDroughtPrediction,
  )
  return { data, loading, error }
}

/**
 * Current weather. Coordinates are optional: without them the backend applies the
 * site ones (see app.weather.*), which avoids querying another location by mistake.
 */
export function useWeather(latitude, longitude) {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'weather', latitude ?? 'site', longitude ?? 'site'],
    () => weatherApi.getCurrent(latitude, longitude),
  )
  return { data, loading, error }
}

/**
 * Backend health. The contract stays `{ healthy, loading }`: an unreachable backend
 * is not an error to display but the dashboard's "Backend disconnected" indicator.
 */
export function useHealthCheck() {
  const { data, loading, error } = useModuleQuery(['watersupply', 'health'], healthApi.check)
  return { healthy: !loading && !error && Boolean(data), loading }
}

/** Cumulative water-savings series (1 to 60 day window). */
export function useSavingsSeries(days = 30) {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'savings-series', days],
    () => dashboardApi.getSavingsSeries(days),
  )
  return { data, loading, error }
}

/** Water balance: inputs (harvested rain) against outputs (consumed water). */
export function useWaterBalance(period = 'month') {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'water-balance', period],
    () => dashboardApi.getWaterBalance(period),
  )
  return { data, loading, error }
}

/** Flow anomalies (probable leaks), computed backend side. */
export function useLeaks() {
  const { data, loading, error } = useModuleQuery(['watersupply', 'leaks'], dashboardApi.getLeaks)
  return { data, loading, error }
}

/** Irrigation postponement suggestions based on expected rain: the backend proposes, the user decides. */
export function useIrrigationSuggestions() {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'irrigation-suggestions'],
    irrigationApi.getSuggestions,
  )
  return { data, loading, error }
}
