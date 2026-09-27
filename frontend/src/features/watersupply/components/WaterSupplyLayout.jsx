import { useCallback, useEffect, useMemo, useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import '../watersupply.css'
import WaterSupplySidebar from './WaterSupplySidebar'
import { viewUrl } from '../paths'

/** Duree d'affichage du bandeau de confirmation (ms). */
const TOAST_DURATION_MS = 2400

/**
 * Coque du module watersupply : menu a gauche, ecran actif a droite, notifications en bas.
 *
 * Meme structure que MachineryLayout / PlantsLayout (sidebar + contenu), sauf que les ecrans
 * s'affichent via <Outlet /> : les URL sont donc reelles (/watersupply, /watersupply/farms, ...)
 * et non plus un etat interne au composant.
 *
 * Les vues attendent deux proprietes (`notify` pour le bandeau de confirmation, `onNavigate`
 * pour les raccourcis du dashboard) : elles sont fournies par le contexte de l'Outlet et
 * transmises par l'adaptateur de routes.jsx. Les vues et leurs tests restent donc inchanges.
 */
export default function WaterSupplyLayout() {
  const navigate = useNavigate()
  const [toast, setToast] = useState(null)

  const notify = useCallback((message) => setToast(message), [])
  const goToView = useCallback((viewId) => navigate(viewUrl(viewId)), [navigate])
  const moduleUi = useMemo(() => ({ notify, onNavigate: goToView }), [notify, goToView])

  // Le bandeau disparait tout seul ; le nettoyage evite de laisser un minuteur derriere soi
  // quand l'utilisateur quitte le module ou declenche une nouvelle confirmation.
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
          {/* L'ecran actif vient de l'URL (voir routes.jsx), qui lui transmet le contexte du module. */}
          <Outlet context={moduleUi} />
        </div>
      </main>

      <div className={`ws-toast ${toast ? 'show' : ''}`} role="status" aria-live="polite">
        {toast}
      </div>
    </div>
  )
}
