import { useCallback, useEffect, useState } from 'react'
import { waterQuotaApi } from '../api/watersupplyApi'

/**
 * Suivi des quotas mensuels d'eau (P8) : pour chaque quota du mois courant,
 * la consommation cumulée des capteurs, le pourcentage utilisé et le statut
 * (ok / warning à 80 % / exceeded à 100 %).
 */
export function useQuotaUsage() {
  const [usage, setUsage] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const refetch = useCallback(() => {
    setLoading(true)
    return waterQuotaApi
      .getUsage()
      .then((res) => {
        setUsage(Array.isArray(res) ? res : res?.content ?? [])
        setError(null)
        return res
      })
      .catch((err) => {
        setError(err)
        throw err
      })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    let cancelled = false
    waterQuotaApi
      .getUsage()
      .then((res) => {
        if (!cancelled) {
          setUsage(Array.isArray(res) ? res : res?.content ?? [])
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

  return { usage, loading, error, refetch }
}
