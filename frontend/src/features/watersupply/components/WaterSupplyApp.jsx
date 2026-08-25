import { useState } from 'react'
import '../watersupply.css'
import { DashboardView } from './views/DashboardView'
import { IrrigationView } from './views/IrrigationView'
import { ConsumptionView } from './views/ConsumptionView'
import { RainwaterView } from './views/RainwaterView'
import { DripView } from './views/DripView'
import { QualityView } from './views/QualityView'
import { DroughtView } from './views/DroughtView'

const NAV_ITEMS = [
  { id: 'dashboard', label: 'Dashboard', section: null },
  { id: 'irrigation', label: 'Irrigation Scheduling', section: 'Water saving planning' },
  { id: 'consumption', label: 'Consumption Tracking', section: 'Water saving planning' },
  { id: 'rainwater', label: 'Rainwater Harvesting', section: 'Water saving planning' },
  { id: 'drip', label: 'Drip Irrigation', section: 'Water saving planning' },
  { id: 'quality', label: 'Water Quality', section: 'Water saving planning' },
  { id: 'drought', label: 'Drought Alerts', section: 'Water saving planning' },
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
  logout: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <path d="M16 17l5-5-5-5M21 12H9" />
    </svg>
  ),
}

export function WaterSupplyApp({ user, onLogout }) {
  const [activeView, setActiveView] = useState('dashboard')
  const [toast, setToast] = useState(null)
  const [toastTimer, setToastTimer] = useState(null)

  const notify = (message) => {
    setToast(message)
    clearTimeout(toastTimer)
    setToastTimer(setTimeout(() => setToast(null), 2400))
  }

  const showView = (id) => {
    setActiveView(id)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleLogout = () => {
    if (onLogout) onLogout()
    notify('Session closed.')
  }

  const userName = user?.firstName || user?.email || 'Yenouyaba'
  const initials = userName.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase() || 'YN'

  return (
    <div className="ws-app">
      <aside className="ws-sidebar">
        <div className="ws-sidebar-top">
          <div className="ws-app-logo" style={{ display: 'grid', placeItems: 'center', fontSize: '40px', fontWeight: 800, color: 'var(--ws-primary)' }}>
            💧
          </div>
          <div className="ws-module-name">Water Supply<br />Management</div>
        </div>
        <hr />
        <nav className="ws-main-nav">
          {NAV_ITEMS.map((item, index) => {
            const showSection = item.section && (index === 0 || NAV_ITEMS[index - 1].section !== item.section)
            return (
              <div key={item.id}>
                {showSection && <div className="ws-nav-section-label">{item.section}</div>}
                <button
                  className={`ws-nav-item ${activeView === item.id ? 'active' : ''}`}
                  onClick={() => showView(item.id)}
                >
                  {ICONS[item.id]}
                  {item.label}
                </button>
              </div>
            )
          })}
        </nav>
        <div className="ws-sidebar-bottom">
          <button className="ws-logout-item" onClick={handleLogout}>
            {ICONS.logout}
            Logout
          </button>
        </div>
      </aside>

      <main className="ws-main">
        <div className={`ws-view ${activeView === 'dashboard' ? 'active' : ''}`}>
          <DashboardView onNavigate={showView} notify={notify} userName={userName} initials={initials} />
        </div>
        <div className={`ws-view ${activeView === 'irrigation' ? 'active' : ''}`}>
          <IrrigationView notify={notify} userName={userName} initials={initials} />
        </div>
        <div className={`ws-view ${activeView === 'consumption' ? 'active' : ''}`}>
          <ConsumptionView notify={notify} userName={userName} initials={initials} />
        </div>
        <div className={`ws-view ${activeView === 'rainwater' ? 'active' : ''}`}>
          <RainwaterView notify={notify} userName={userName} initials={initials} />
        </div>
        <div className={`ws-view ${activeView === 'drip' ? 'active' : ''}`}>
          <DripView notify={notify} userName={userName} initials={initials} />
        </div>
        <div className={`ws-view ${activeView === 'quality' ? 'active' : ''}`}>
          <QualityView notify={notify} userName={userName} initials={initials} />
        </div>
        <div className={`ws-view ${activeView === 'drought' ? 'active' : ''}`}>
          <DroughtView notify={notify} userName={userName} initials={initials} />
        </div>
      </main>

      <div className={`ws-toast ${toast ? 'show' : ''}`} role="status" aria-live="polite">
        {toast}
      </div>
    </div>
  )
}