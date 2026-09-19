import { describe, expect, it, vi, beforeEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { useAuth } from './useAuth'
import { apiClient, getToken, setToken } from '../api/client.js'

vi.mock('../api/client.js', () => {
  let token = null
  return {
    apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn(), patch: vi.fn() },
    getToken: vi.fn(() => token),
    setToken: vi.fn((value) => {
      token = value
    }),
  }
})

function makeJwt(payload) {
  return `header.${btoa(JSON.stringify(payload))}.signature`
}

beforeEach(() => {
  setToken(null)
  vi.mocked(apiClient.post).mockReset()
})

describe('useAuth', () => {
  it('connecte l’utilisateur avec un token valide', async () => {
    const user = { id: 'u1', email: 'farmer@farm.io' }
    vi.mocked(apiClient.post).mockResolvedValue({ access_token: 'token-123', user })

    const { result } = renderHook(() => useAuth())
    let response
    await act(async () => {
      response = await result.current.login('farmer@farm.io', 'secret')
    })

    expect(apiClient.post).toHaveBeenCalledWith('/auth/login', { email: 'farmer@farm.io', password: 'secret' })
    expect(response.user).toEqual(user)
    expect(getToken()).toBe('token-123')
    expect(result.current.user).toEqual(user)
    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.loading).toBe(false)
  })

  it('expose l’erreur quand le backend refuse la connexion', async () => {
    vi.mocked(apiClient.post).mockRejectedValue(new Error('Identifiants invalides'))

    const { result } = renderHook(() => useAuth())
    await act(async () => {
      await expect(result.current.login('farmer@farm.io', 'wrong')).rejects.toThrow('Identifiants invalides')
    })

    expect(result.current.error).toBeInstanceOf(Error)
    expect(result.current.isAuthenticated).toBe(false)
    expect(getToken()).toBeNull()
  })

  it('déconnecte l’utilisateur et efface le token', async () => {
    setToken(makeJwt({ sub: 'farmer@farm.io', userId: 'u1' }))
    const { result } = renderHook(() => useAuth())
    expect(result.current.isAuthenticated).toBe(true)

    act(() => {
      result.current.logout()
    })

    expect(getToken()).toBeNull()
    expect(result.current.user).toBeNull()
    expect(result.current.isAuthenticated).toBe(false)
  })

  it('rejette un token JWT expiré', () => {
    setToken(makeJwt({ sub: 'farmer@farm.io', userId: 'u1', exp: Math.floor(Date.now() / 1000) - 3600 }))

    const { result } = renderHook(() => useAuth())

    expect(getToken()).toBeNull()
    expect(result.current.user).toBeNull()
    expect(result.current.isAuthenticated).toBe(false)
  })

  it('accepte un token JWT non expiré', () => {
    setToken(makeJwt({ sub: 'farmer@farm.io', userId: 'u1', exp: Math.floor(Date.now() / 1000) + 3600 }))

    const { result } = renderHook(() => useAuth())

    expect(result.current.user).toEqual({ id: 'u1', email: 'farmer@farm.io' })
    expect(result.current.isAuthenticated).toBe(true)
  })

  it('gère un token JWT malformé', () => {
    setToken('not-a-jwt')
    const { result } = renderHook(() => useAuth())
    expect(getToken()).toBeNull()
    expect(result.current.user).toBeNull()
  })
})

describe('waitFor integration', () => {
  it('termine le chargement après le login', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ access_token: 't', user: { email: 'a@b.c' } })
    const { result } = renderHook(() => useAuth())
    await act(async () => {
      await result.current.login('a@b.c', 'x')
    })
    await waitFor(() => expect(result.current.loading).toBe(false))
  })
})