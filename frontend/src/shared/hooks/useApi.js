import { useCallback, useState } from 'react'
import { apiClient } from '../api/client.js'

/**
 * Hook générique pour appeler l'API avec gestion d'état de chargement et d'erreur.
 * @param {string} method - GET, POST, PUT, DELETE, PATCH
 * @returns {{ loading: boolean, error: Error|null, execute: Function }}
 */
export function useApi(method) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const execute = useCallback(
    async (path, body = null) => {
      setLoading(true)
      setError(null)
      try {
        const result = await apiClient[method.toLowerCase()](path, body)
        return result
      } catch (err) {
        setError(err)
        throw err
      } finally {
        setLoading(false)
      }
    },
    [method],
  )

  return { loading, error, execute }
}
