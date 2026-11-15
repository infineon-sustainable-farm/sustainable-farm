import { useCallback, useEffect, useMemo, useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import '../watersupply.css'
import WaterSupplySidebar from './WaterSupplySidebar'
import { viewUrl } from '../paths'

/** How long the confirmation banner stays on screen (ms). */
const TOAST_DURATION_MS = 2400

/**
 * Shell of the watersupply module: menu on the left, active screen on the right,
 * notifications at the bottom.
 *
 * Same structure as MachineryLayout / PlantsLayout (sidebar + content), except that screens
 * render through <Outlet />: so the URLs are real (/watersupply, /watersupply/farms, ...)
 * and no longer internal component state.
 *
 * The views expect two props (`notify` for the confirmation banner, `onNavigate`
 * for the dashboard shortcuts): they are provided by the Outlet context and
 * passed by the routes.jsx adapter. The views and their tests therefore stay unchanged.
 */
export default function WaterSupplyLayout() {
  const navigate = useNavigate()
  const [toast, setToast] = useState(null)

  const notify = useCallback((message) => setToast(message), [])
  const goToView = useCallback((viewId) => navigate(viewUrl(viewId)), [navigate])
  const moduleUi = useMemo(() => ({ notify, onNavigate: goToView }), [notify, goToView])

  // The banner fades away on its own; the cleanup avoids leaving a timer behind
  // when the user leaves the module or triggers a new confirmation.
  useEffect(() => {
    if (!toast) return undefined
    const timer = setTimeout(() => setToast(null), TOAST_DURATION_MS)
    return () => clearTimeout(timer)
  }, [toast])

  return (
    <div className="ws-app">
      <WaterSupplySidebar />

      <main className="ws-main">
        <div className="ws-view active">
          {/* The active screen comes from the URL (see routes.jsx), which passes it the module context. */}
          <Outlet context={moduleUi} />
        </div>
      </main>

      <div className={`ws-toast ${toast ? 'show' : ''}`} role="status" aria-live="polite">
        {toast}
      </div>
    </div>
  )
}
