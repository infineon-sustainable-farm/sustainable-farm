import { cloneElement, useState } from 'react'
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import '../watersupply.css'
import { DashboardView } from './views/DashboardView'
import { IrrigationView } from './views/IrrigationView'
import { ConsumptionView } from './views/ConsumptionView'
import { RainwaterView } from './views/RainwaterView'
import { DripView } from './views/DripView'
import { QualityView } from './views/QualityView'
import { DroughtView } from './views/DroughtView'
import { FarmsView } from './views/FarmsView'
import { SourcesView } from './views/SourcesView'
import { NotificationsView } from './views/NotificationsView'

// Chaque vue possède une URL directe (ex. /watersupply/consommation) — voir App.jsx.
const NAV_ITEMS = [
  { id: 'dashboard', path: '', label: 'Tableau de bord', section: null, element: <DashboardView /> },
  { id: 'farms', path: 'fermes', label: 'Fermes & Champs', section: 'Gestion des données', element: <FarmsView /> },
  { id: 'sources', path: 'sources', label: "Sources d'eau", section: 'Gestion des données', element: <SourcesView /> },
  { id: 'irrigation', path: 'irrigation', label: "Planification d'irrigation", section: 'Planification de l’eau', element: <IrrigationView /> },
  { id: 'consumption', path: 'consommation', label: 'Suivi consommation', section: 'Planification de l’eau', element: <ConsumptionView /> },
  { id: 'rainwater', path: 'pluvial', label: 'Récupération pluviale', section: 'Planification de l’eau', element: <RainwaterView /> },
  { id: 'drip', path: 'goutte-a-goutte', label: 'Maintenance', section: 'Planification de l’eau', element: <DripView /> },
  { id: 'quality', path: 'qualite', label: 'Qualité de l’eau', section: 'Planification de l’eau', element: <QualityView /> },
  { id: 'drought', path: 'secheresse', label: 'Alertes sécheresse', section: 'Planification de l’eau', element: <DroughtView /> },
  { id: 'notifications', path: 'notifications', label: 'Notifications', section: 'Opérations', element: <NotificationsView /> },
]

const ICONS = {
  dashboard: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <rect width="7" height="9" x="3" y="3" rx="1" />
      <rect width="7" height="5" x="14" y="3" rx="1" />
      <rect width="7" height="9" x="14" y="12" rx="1" />
      <rect width="7" height="5" x="3" y="16" rx="1" />
    </svg>
  ),
  farms: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18Z" />
      <path d="M6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2" />
      <path d="M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2" />
      <path d="M10 6h4M10 10h4M10 14h4" />
    </svg>
  ),
  irrigation: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M21 7.5V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h3.5" />
      <path d="M16 2v4M8 2v4M3 10h5" />
      <path d="M17.5 17.5 16 16.3V14" />
      <circle cx="16" cy="16" r="6" />
    </svg>
  ),
  consumption: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M3 3v16a2 2 0 0 0 2 2h16" />
      <path d="M18 17V9M13 17V5M8 17v-3" />
    </svg>
  ),
  rainwater: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242" />
      <path d="M16 14v6M8 14v6M12 16v6" />
    </svg>
  ),
  drip: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <rect x="16" y="16" width="6" height="6" rx="1" />
      <rect x="2" y="16" width="6" height="6" rx="1" />
      <rect x="9" y="2" width="6" height="6" rx="1" />
      <path d="M5 16v-3a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v3M12 12V8" />
    </svg>
  ),
  quality: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M10 2v7.527a2 2 0 0 1-.211.896L4.72 20.55a1 1 0 0 0 .9 1.45h12.76a1 1 0 0 0 .9-1.45l-5.069-10.127A2 2 0 0 1 14 9.527V2" />
      <path d="M8.5 2h7M7 16h10" />
    </svg>
  ),
  drought: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3" />
      <path d="M12 9v4M12 17h.01" />
    </svg>
  ),
  notifications: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M10.268 21a2 2 0 0 0 3.464 0" />
      <path d="M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8a6 6 0 0 0-12 0c0 4.499-1.411 5.956-2.738 7.326" />
    </svg>
  ),
  }

export function WaterSupplyApp() {
  const location = useLocation()
  const navigate = useNavigate()
  const [toast, setToast] = useState(null)
  const [toastTimer, setToastTimer] = useState(null)

  const notify = (message) => {
    setToast(message)
    clearTimeout(toastTimer)
    setToastTimer(setTimeout(() => setToast(null), 2400))
  }

  const activeItem = NAV_ITEMS.find((item) => (item.path === '' ? location.pathname === '/' : location.pathname === `/${item.path}`))
  const showView = (id) => {
    const item = NAV_ITEMS.find((nav) => nav.id === id)
    if (item) navigate(item.path === '' ? '/' : `/${item.path}`)
  }


  const renderView = (item) =>
    cloneElement(item.element, {
      notify,
      onNavigate: item.id === 'dashboard' ? showView : undefined,
    })

  return (
    <div className="ws-app">
      <aside className="ws-sidebar">
        <div className="ws-sidebar-top">
          <img src="/logo.webp" alt="Sustainable Farm" className="ws-app-logo" />
          <div className="ws-module-name">Sustainable Farm</div>
        </div>
        <hr />
        <nav className="ws-main-nav">
          {NAV_ITEMS.map((item, index) => {
            const showSection = item.section && (index === 0 || NAV_ITEMS[index - 1].section !== item.section)
            return (
              <div key={item.id}>
                {showSection && <div className="ws-nav-section-label">{item.section}</div>}
                <button
                  className={`ws-nav-item ${activeItem?.id === item.id ? 'active' : ''}`}
                  onClick={() => showView(item.id)}
                >
                  {ICONS[item.id]}
                  {item.label}
                </button>
              </div>
            )
          })}
        </nav>
      </aside>

      <main className="ws-main">
        <div className="ws-view active">
          <Routes>
            {NAV_ITEMS.map((item) => (
              <Route key={item.id} path={item.path === '' ? '/' : item.path} element={renderView(item)} />
            ))}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </main>

      <div className={`ws-toast ${toast ? 'show' : ''}`} role="status" aria-live="polite">
        {toast}
      </div>
    </div>
  )
}
