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
import { UnknownView } from './components/views/UnknownView'
import { VIEW_PATHS, WATERSUPPLY_BASE, viewUrl } from './paths'
import { VIEW_COMPONENTS, withModuleUi } from './ModuleScreen'

/**
 * Routes of the watersupply module.
 *
 * The module is mounted under /watersupply, like machinery (/machinery) and plants (/plants):
 * each module carries its own menu and its own screens, and the application discovers them
 * automatically (frontend/src/app/router.jsx reads every features/<module>/routes.jsx).
 *
 * The children are derived from VIEW_PATHS: the menu and the router therefore share the
 * same source of truth and can no longer go out of sync.
 *
 * This file only declares routing. The components and the UI adapter live in
 * ModuleScreen.jsx: the react-refresh/only-export-components rule requires a file to
 * export only components, and this one exports a route array. The file name and the
 * default export are imposed by the discovery in app/router.jsx.
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
    children: [...viewRoutes, { path: '*', element: <UnknownView /> }],
  },
]

/**
 * Navigation of the module, read by the application shell (app/Sidebar.jsx) and the launcher
 * page (app/LauncherPage.jsx). Same entries, order and targets as the module's own sidebar.
 */
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

export const nav = {
  id: 'watersupply',
  basePath: WATERSUPPLY_BASE,
  icon: Droplets,
  nameKey: 'watersupply:name',
  items: Object.keys(VIEW_PATHS).map((viewId) => ({
    to: viewUrl(viewId),
    labelKey: `watersupply:nav.${viewId}`,
    icon: NAV_ICONS[viewId],
    end: viewId === 'dashboard',
  })),
}
