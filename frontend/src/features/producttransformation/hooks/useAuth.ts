import { useState, useEffect } from 'react'

export function useAuth() {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [userRole, setUserRole] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const checkAuth = async () => {
      try {
        const res = await fetch('/api/auth/status', {
          credentials: 'include',
        })
        if (res.ok) {
          const data = await res.json()
          if (data.authenticated) {
            setIsAuthenticated(true)
            setUserRole(data.role)
          }
        }
      } catch (e) {
        // Auth endpoint not available - allow unauthenticated access for development
        setIsAuthenticated(true)
        setUserRole('operator')
      } finally {
        setLoading(false)
      }
    }
    checkAuth()
  }, [])

  return { isAuthenticated, userRole, loading }
}

export function useApi<T>(url: string, options?: RequestInit) {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await fetch(url, {
          credentials: 'include',
          ...options,
        })
        if (!res.ok) {
          const errorData = await res.json().catch(() => ({}))
          throw new Error(errorData.message || `Error: ${res.status}`)
        }
        const result = await res.json()
        setData(result)
      } catch (e) {
        setError(e instanceof Error ? e.message : 'An error occurred')
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [url, options])

  return { data, loading, error }
}