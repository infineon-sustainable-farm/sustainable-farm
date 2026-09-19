import { useCallback, useState } from 'react'
import { apiClient, getToken, setToken } from '../api/client.js'

function readUserFromToken() {
  const token = getToken()
  if (!token) return null

  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    // Expiration JWT : un token expiré est supprimé et l'utilisateur considéré déconnecté.
    if (payload.exp && payload.exp * 1000 < Date.now()) {
      setToken(null)
      return null
    }
    return {
      id: payload.userId,
      email: payload.sub,
    }
  } catch {
    setToken(null)
    return null
  }
}

export function useAuth() {
  const [user, setUser] = useState(() => readUserFromToken())
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

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
      return await apiClient.post('/auth/register', { firstName, lastName, email, password })
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

  return {
    user,
    loading,
    error,
    login,
    register,
    logout,
    isAuthenticated: !!user,
  }
}
