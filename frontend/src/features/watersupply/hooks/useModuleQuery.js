import { useQuery } from '@tanstack/react-query'

/**
 * React Query wrapper for the module data.
 *
 * Why: every view used to run its own `useEffect` + `useState` loading block,
 * with the same logic copied a dozen times. React Query is already the project's
 * data library (machinery, plants): it brings request deduplication, keyed caching
 * and background refresh, without reinventing a local state.
 *
 * The contract exposed to the views deliberately stays the same as before —
 * `{ data, loading, error, refetch }` — so that neither the views nor their tests
 * change. Data is considered fresh (`staleTime: 0`) and failures are not retried
 * automatically: a supervision screen must show the error, not hide a backend outage.
 */
export function useModuleQuery(queryKey, queryFn, { enabled = true } = {}) {
  const { data, isPending, error, refetch } = useQuery({
    queryKey,
    queryFn: () => queryFn(),
    enabled,
    retry: false,
    refetchOnWindowFocus: false,
    staleTime: 0,
  })

  return {
    data: data ?? null,
    // A disabled request (missing identifier) is not "pending": same behaviour as
    // the previous hooks, which left `loading` at false in that case.
    loading: enabled ? isPending : false,
    error: error ?? null,
    refetch,
  }
}
