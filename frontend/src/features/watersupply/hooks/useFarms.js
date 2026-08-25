import { useEffect, useState } from 'react'
import { farmApi, fieldApi, zoneApi } from '../api/watersupplyApi'

/**
 * Hook pour récupérer la liste des fermes.
 */
export function useFarms() {
  const [farms, setFarms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fetchFarms = () => {
    setLoading(true)
    farmApi
      .getFarms()
      .then((res) => {
        setFarms(res)
        setLoading(false)
      })
      .catch((err) => {
        setError(err)
        setLoading(false)
      })
  }

  useEffect(() => {
    fetchFarms()
  }, [])

  return { farms, loading, error, refetch: fetchFarms }
}

/**
 * Hook pour récupérer les champs d'une ferme.
 */
export function useFarmFields(farmId) {
  const [fields, setFields] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!farmId) return
    let cancelled = false
    setLoading(true)
    farmApi
      .getFarmFields(farmId)
      .then((res) => {
        if (!cancelled) {
          setFields(res)
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
  }, [farmId])

  return { fields, loading, error }
}

/**
 * Hook pour récupérer les zones d'un champ.
 */
export function useFieldZones(fieldId) {
  const [zones, setZones] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!fieldId) return
    let cancelled = false
    setLoading(true)
    fieldApi
      .getFieldZones(fieldId)
      .then((res) => {
        if (!cancelled) {
          setZones(res)
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
  }, [fieldId])

  return { zones, loading, error }
}

/**
 * Hook pour récupérer toutes les zones.
 */
export function useZones() {
  const [zones, setZones] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    zoneApi
      .getZones()
      .then((res) => {
        if (!cancelled) {
          setZones(res)
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

  return { zones, loading, error }
}
