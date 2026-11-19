import { useCallback, useEffect, useState } from 'react'
import { fetchPersons } from '../api/persons'

/**
 * Loads the active people of the directory (GET /api/staff/persons) for a person picker. Plain
 * state and effect, so a screen works with or without a React Query provider.
 *
 * @returns {{ data: object[] | undefined, isPending: boolean, isError: boolean, refetch: () => void }}
 */
export function usePersons() {
  const [state, setState] = useState({ data: undefined, isPending: true, isError: false })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    fetchPersons()
      .then((data) => {
        if (!cancelled) setState({ data: Array.isArray(data) ? data : [], isPending: false, isError: false })
      })
      .catch(() => {
        if (!cancelled) setState({ data: undefined, isPending: false, isError: true })
      })
    return () => {
      cancelled = true
    }
  }, [attempt])

  const refetch = useCallback(() => {
    setState((current) => ({ ...current, isPending: true, isError: false }))
    setAttempt((current) => current + 1)
  }, [])

  return { ...state, refetch }
}
