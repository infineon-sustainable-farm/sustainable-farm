import { dashboardApi, aiApi, weatherApi, healthApi, irrigationApi } from '../api/watersupplyApi'
import { useModuleQuery } from './useModuleQuery'

/**
 * Donnees du tableau de bord, servies par React Query via {@link useModuleQuery} : les onze
 * hooks ci-dessous ne portent plus chacun leur `useEffect` + `useState`, la logique de
 * chargement n'existe qu'a un seul endroit.
 *
 * Le contrat rendu aux vues reste celui d'avant : `{ data, loading, error }` (et `refetch`
 * la ou une vue rafraichit apres une action).
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
 * Meteo courante. Les coordonnees sont optionnelles : sans elles, le backend applique celles
 * du site (voir app.weather.*), ce qui evite d'interroger un autre lieu par erreur.
 */
export function useWeather(latitude, longitude) {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'weather', latitude ?? 'site', longitude ?? 'site'],
    () => weatherApi.getCurrent(latitude, longitude),
  )
  return { data, loading, error }
}

/**
 * Sante du backend. Le contrat reste `{ healthy, loading }` : un backend injoignable n'est pas
 * une erreur a afficher mais l'indicateur « Backend deconnecte » du dashboard.
 */
export function useHealthCheck() {
  const { data, loading, error } = useModuleQuery(['watersupply', 'health'], healthApi.check)
  return { healthy: !loading && !error && Boolean(data), loading }
}

/** Serie cumulee de l'economie d'eau (fenetre de 1 a 60 jours). */
export function useSavingsSeries(days = 30) {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'savings-series', days],
    () => dashboardApi.getSavingsSeries(days),
  )
  return { data, loading, error }
}

/** Bilan hydrique : entrees (pluie recuperee) contre sorties (eau consommee). */
export function useWaterBalance(period = 'month') {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'water-balance', period],
    () => dashboardApi.getWaterBalance(period),
  )
  return { data, loading, error }
}

/** Anomalies de debit (fuites probables), calculees cote backend. */
export function useLeaks() {
  const { data, loading, error } = useModuleQuery(['watersupply', 'leaks'], dashboardApi.getLeaks)
  return { data, loading, error }
}

/** Suggestions de report d'irrigation fondees sur la pluie prevue : le backend propose, l'utilisateur decide. */
export function useIrrigationSuggestions() {
  const { data, loading, error } = useModuleQuery(
    ['watersupply', 'irrigation-suggestions'],
    irrigationApi.getSuggestions,
  )
  return { data, loading, error }
}
