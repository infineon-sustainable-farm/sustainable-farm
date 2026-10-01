import {
  Boxes,
  Bug,
  CalendarDays,
  ClipboardList,
  FlaskConical,
  Fuel,
  Home,
  LayoutDashboard,
  Leaf,
  Wrench,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { viewUrl } from '../paths'

/**
 * Menu du module, dans l'ordre des sections de l'interface.
 * Les icones viennent de lucide-react (deja utilise par les modules machinery et plants)
 * au lieu des SVG inline, et les entrees sont de vrais liens : l'etat actif est fourni par
 * le routeur, les URL sont partageables et la navigation est accessible au clavier.
 */
const NAV_ITEMS = [
  { id: 'dashboard', label: 'Dashboard', section: null, icon: LayoutDashboard },
  { id: 'farms', label: 'Farms & Fields', section: 'Data management', icon: Home },
  { id: 'sources', label: 'Water sources', section: 'Data management', icon: Boxes },
  { id: 'irrigation', label: 'Irrigation planning', section: 'Water planning', icon: CalendarDays },
  { id: 'consumption', label: 'Consumption tracking', section: 'Water planning', icon: Fuel },
  { id: 'rainwater', label: 'Rainwater harvesting', section: 'Water planning', icon: Leaf },
  { id: 'maintenance', label: 'Maintenance', section: 'Water planning', icon: Wrench },
  { id: 'quality', label: 'Water quality', section: 'Water planning', icon: FlaskConical },
  { id: 'drought', label: 'Drought alerts', section: 'Water planning', icon: Bug },
  { id: 'notifications', label: 'Notifications', section: 'Operations', icon: ClipboardList },
]

/** Classe du lien : la classe `active` attendue par watersupply.css est posee explicitement. */
const linkClassName = ({ isActive }) => (isActive ? 'ws-nav-item active' : 'ws-nav-item')

export default function WaterSupplySidebar() {
  return (
    <aside className="ws-sidebar">
      <div className="ws-sidebar-top">
        <img src="/logo.webp" alt="Sustainable Farm" className="ws-app-logo" />
        <div className="ws-module-name">Sustainable Farm</div>
      </div>

      <hr />

      <nav className="ws-main-nav" aria-label="Water supply module">
        {NAV_ITEMS.map((item, index) => {
          const showSection = item.section && (index === 0 || NAV_ITEMS[index - 1].section !== item.section)
          const Icon = item.icon
          return (
            <div key={item.id}>
              {showSection && <div className="ws-nav-section-label">{item.section}</div>}
              <NavLink
                to={viewUrl(item.id)}
                className={linkClassName}
                end={item.id === 'dashboard'}
              >
                <Icon aria-hidden="true" />
                {item.label}
              </NavLink>
            </div>
          )
        })}
      </nav>
    </aside>
  )
}
