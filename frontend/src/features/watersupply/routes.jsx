import {
  Boxes,
  Bug,
  CalendarDays,
  CalendarX,
  ClipboardList,
  Droplets,
  FlaskConical,
  Fuel,
  Home,
  LayoutDashboard,
  Leaf,
  Package,
  Sprout,
  Users,
  Wrench,
} from 'lucide-react'
import WaterSupplyLayout from './components/WaterSupplyLayout'
import { VIEW_PATHS, WATERSUPPLY_BASE, viewUrl } from './paths'
import { VIEW_COMPONENTS, withModuleUi } from './ModuleScreen'

/**
 * Routes of the watersupply module, mounted under /watersupply inside the application shell
 * (app/AppLayout.jsx, see nav.shell below).
 *
 * The children are derived from VIEW_PATHS: the menu and the router share the same source of
 * truth. An unknown URL under /watersupply falls through to the shell's 404 page (app/routes.jsx),
 * with the water supply menu still open in the sidebar.
 *
 * This file only declares routing. The components and the UI adapter live in ModuleScreen.jsx:
 * the react-refresh/only-export-components rule requires a file to export only components, and
 * this one exports a route array.
 */
const viewRoutes = Object.entries(VIEW_PATHS).map(([viewId, relativePath]) => {
  const Screen = withModuleUi(VIEW_COMPONENTS[viewId])
  return relativePath === ''
    ? { index: true, element: <Screen /> }
    : { path: relativePath, element: <Screen /> }
})

export default [
  {
    path: 'watersupply',
    element: <WaterSupplyLayout />,
    children: viewRoutes,
  },
]

const NAV_ICONS = {
  dashboard: LayoutDashboard,
  farms: Home,
  sources: Boxes,
  irrigation: CalendarDays,
  consumption: Fuel,
  rainwater: Leaf,
  maintenance: Wrench,
  quality: FlaskConical,
  drought: Bug,
  outages: CalendarX,
  plants: Sprout,
  lots: Package,
  visitors: Users,
  notifications: ClipboardList,
}

// Section heading of each entry, as in the module's former sidebar (the dashboard has none).
const NAV_SECTIONS = {
  farms: 'dataManagement',
  sources: 'dataManagement',
  irrigation: 'waterPlanning',
  consumption: 'waterPlanning',
  rainwater: 'waterPlanning',
  maintenance: 'waterPlanning',
  quality: 'waterPlanning',
  drought: 'waterPlanning',
  outages: 'waterPlanning',
  plants: 'integrations',
  lots: 'integrations',
  visitors: 'integrations',
  notifications: 'operations',
}

/**
 * Navigation of the module, read by the application shell (app/Sidebar.jsx) and the launcher
 * page (app/LauncherPage.jsx). Same entries, order, targets and section headings as the module's
 * former sidebar.
 */
export const nav = {
  id: 'watersupply',
  basePath: WATERSUPPLY_BASE,
  icon: Droplets,
  nameKey: 'watersupply:name',
  shell: true,
  items: Object.keys(VIEW_PATHS).map((viewId) => ({
    to: viewUrl(viewId),
    labelKey: `watersupply:nav.${viewId}`,
    icon: NAV_ICONS[viewId],
    end: viewId === 'dashboard',
    sectionKey: NAV_SECTIONS[viewId] && `watersupply:navSections.${NAV_SECTIONS[viewId]}`,
  })),
}
