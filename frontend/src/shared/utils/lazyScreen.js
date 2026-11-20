import { lazy } from 'react'

/**
 * Screen loaded on its first visit (React.lazy), in a chunk of its own. The Suspense boundaries
 * are in app/RootLayout.jsx and app/AppLayout.jsx.
 *
 * Module routes.jsx files use it instead of a bare lazy(): they export route arrays, and
 * react-refresh/only-export-components refuses a component declared next to them. The real
 * component keeps its own file, so fast refresh still works on it.
 *
 * @param {() => Promise<object>} load - `() => import('./components/Screen')`
 * @param {string} [exportName] - named export of the screen, the default export otherwise
 */
export function lazyScreen(load, exportName = 'default') {
  return lazy(() => load().then((module) => ({ default: module[exportName] })))
}
