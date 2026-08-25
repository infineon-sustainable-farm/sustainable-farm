import { useCallback, useEffect, useState } from 'react'
import { apiClient, setToken, getToken } from '../api/client.js'

/**
 * Hook d'authentification - gère le login, logout et l'état de l'utilisateur connecté.
 */
export function useAuth() {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  // Vérifie au montage si un token existe déjà
  useEffect(() => {
    const token = getToken()
    if (token) {
      // Décoder le token pour récupérer les infos utilisateur de base
      try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        setUser({
          id: payload.userId,
          email: payload.sub,
        })
      } catch {
        // Token invalide, on le supprime
        setToken(null)
      }
    }
  }, [])

  const login = useCallback(async (email, password) => {
    setLoading(true)
    setError(null)
    try {
      const response = await apiClient.post('/auth/login', { email, password })
      setToken(response.access_token)
      setUser(response.user)
      return response
    } catch (err) {
      setError(err)
      throw err
    } finally {
      setLoading(false)
    }
  }, [])

  const register = useCallback(async (firstName, lastName, email, password) => {
    setLoading(true)
    setError(null)
    try {
      const response = await apiClient.post('/auth/register', { firstName, lastName, email, password })
      return response
    } catch (err) {
      setError(err)
      throw err
    } finally {
      setLoading(false)
    }
  }, [])

  const logout = useCallback(() => {
    setToken(null)
    setUser(null)
  }, [])

  const isAuthenticated = !!user

  return {
    user,
    loading,
    error,
    login,
    register,
    logout,
    isAuthenticated,
  }
}
