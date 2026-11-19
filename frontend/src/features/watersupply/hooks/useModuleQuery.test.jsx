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
 * A brand-new React Query client per test: the cache must never hide an API call
 * the test asserts on.
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
 * The contract of the module's hooks is what the views expect:
 * `{ data, loading, error, refetch }` — with an empty list on the first render.
 */
describe('module hooks (React Query)', () => {
  it('exposes an empty list first, then the API data', async () => {
    waterSourceApi.getSources.mockResolvedValue([{ id: 'src-1', name: 'Main borehole' }])

    const { result } = renderHook(() => useWaterSources(), { wrapper: createWrapper() })

    expect(result.current.loading).toBe(true)
    expect(result.current.sources).toEqual([])

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.sources).toHaveLength(1)
    expect(result.current.error).toBeNull()
  })

  it('reduces a paginated response to its content array', async () => {
    waterSourceApi.getSources.mockResolvedValue({
      content: [{ id: 'src-1', name: 'Main borehole' }],
      totalElements: 1,
    })

    const { result } = renderHook(() => useWaterSources(), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.sources).toHaveLength(1))
    expect(result.current.sources[0].id).toBe('src-1')
  })

  it('surfaces the API error without hiding it', async () => {
    waterSourceApi.getSources.mockRejectedValue(new Error('Backend unreachable'))

    const { result } = renderHook(() => useWaterSources(), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.error?.message).toBe('Backend unreachable')
    expect(result.current.sources).toEqual([])
  })

  it('reloads the data when the view calls refetch', async () => {
    waterSourceApi.getSources.mockResolvedValue([{ id: 'src-1' }])

    const { result } = renderHook(() => useWaterSources(), { wrapper: createWrapper() })
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(waterSourceApi.getSources).toHaveBeenCalledTimes(1)

    await act(async () => {
      await result.current.refetch()
    })

    expect(waterSourceApi.getSources).toHaveBeenCalledTimes(2)
  })

  it('does not load a list whose id is missing', async () => {
    const { result } = renderHook(() => useFarmFields(null), { wrapper: createWrapper() })

    expect(farmApi.getFarmFields).not.toHaveBeenCalled()
    expect(result.current.loading).toBe(false)
    expect(result.current.fields).toEqual([])
  })

  it('exposes farms under the name the views expect', async () => {
    farmApi.getFarms.mockResolvedValue([{ id: 'farm-1', name: 'North Farm' }])

    const { result } = renderHook(() => useFarms(), { wrapper: createWrapper() })

    await waitFor(() => expect(result.current.farms).toHaveLength(1))
    expect(result.current.farms[0].name).toBe('North Farm')
  })
})
