// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { waterSourceApi, farmApi } from '../api/watersupplyApi'
import { useFarmFields, useFarms } from './useFarms'
import { useWaterSources } from './useWaterData'

vi.mock('../api/watersupplyApi', () => ({
  waterSourceApi: { getSources: vi.fn() },
  farmApi: { getFarms: vi.fn(), getFarmFields: vi.fn() },
}))

afterEach(cleanup)

beforeEach(() => {
  vi.clearAllMocks()
})

/**
 * Un client React Query neuf par test : le cache ne doit jamais masquer un appel API
 * que le test verifie.
 */
function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: 0, gcTime: 0 } },
  })
  return function Wrapper({ children }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
}

/**
 * Le contrat des hooks du module est celui attendu par les vues :
 * `{ donnee, loading, error, refetch }` — avec une liste vide au premier rendu.
 */
describe('hooks du module (React Query)', () => {
  it('expose une liste vide puis les donnees de l API', async () => {
    waterSourceApi.getSources.mockResolvedValue([{ id: 'src-1', name: 'Main borehole' }])

    const { result } = renderHook(() => useWaterSources(), { wrapper: createWrapper() })

    expect(result.current.loading).toBe(true)
    expect(result.current.sources).toEqual([])

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.sources).toHaveLength(1)
    expect(result.current.error).toBeNull()
  })

  it('ramene une reponse paginee a son tableau content', async () => {
    waterSourceApi.getSources.mockResolvedValue({
      content: [{ id: 'src-1', name: 'Main borehole' }],
      totalElements: 1,
    })

    const { result } = renderHook(() => useWaterSources(), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.sources).toHaveLength(1))
    expect(result.current.sources[0].id).toBe('src-1')
  })

  it('remonte l erreur de l API sans la masquer', async () => {
    waterSourceApi.getSources.mockRejectedValue(new Error('Backend unreachable'))

    const { result } = renderHook(() => useWaterSources(), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.error?.message).toBe('Backend unreachable')
    expect(result.current.sources).toEqual([])
  })

  it('recharge les donnees quand la vue appelle refetch', async () => {
    waterSourceApi.getSources.mockResolvedValue([{ id: 'src-1' }])

    const { result } = renderHook(() => useWaterSources(), { wrapper: createWrapper() })
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(waterSourceApi.getSources).toHaveBeenCalledTimes(1)

    await act(async () => {
      await result.current.refetch()
    })

    expect(waterSourceApi.getSources).toHaveBeenCalledTimes(2)
  })

  it('ne charge pas une liste dont l identifiant est absent', async () => {
    const { result } = renderHook(() => useFarmFields(null), { wrapper: createWrapper() })

    expect(farmApi.getFarmFields).not.toHaveBeenCalled()
    expect(result.current.loading).toBe(false)
    expect(result.current.fields).toEqual([])
  })

  it('expose les fermes sous le nom attendu par les vues', async () => {
    farmApi.getFarms.mockResolvedValue([{ id: 'farm-1', name: 'North Farm' }])

    const { result } = renderHook(() => useFarms(), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.farms).toHaveLength(1))
    expect(result.current.farms[0].name).toBe('North Farm')
  })
})
