import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'

/**
 * Renders a module view with a brand-new React Query client.
 *
 * Why a client per test: the application's shared client keeps its cache between tests
 * (same key = same data), which would hide the API calls the tests assert on
 * ("the list is reloaded after a creation"). Here every render starts from an empty cache.
 *
 * Tests import this function under the name `render`: `import { renderView as render }`.
 */
/**
 * Control (input / select / textarea) of the FormField whose label starts with `label`.
 * FormField renders its label next to the control without `htmlFor`, so testing-library's
 * getByLabelText cannot find it.
 */
export function controlByLabel(container, label) {
  const labelElement = [...container.querySelectorAll('label')].find((element) => element.textContent.trim().startsWith(label))
  if (!labelElement) throw new Error(`No field labelled "${label}"`)
  return labelElement.parentElement.querySelector('input, select, textarea')
}

export function renderView(ui) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: 0, gcTime: 0 } },
  })
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
}
