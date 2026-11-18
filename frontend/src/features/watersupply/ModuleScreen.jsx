import { useOutletContext } from 'react-router-dom'
import { lazyScreen } from '../../shared/utils/lazyScreen'

// Screens load on first visit, one chunk each. The views are named exports.
const DashboardView = lazyScreen(() => import('./components/views/DashboardView'), 'DashboardView')
const FarmsView = lazyScreen(() => import('./components/views/FarmsView'), 'FarmsView')
const SourcesView = lazyScreen(() => import('./components/views/SourcesView'), 'SourcesView')
const IrrigationView = lazyScreen(() => import('./components/views/IrrigationView'), 'IrrigationView')
const ConsumptionView = lazyScreen(() => import('./components/views/ConsumptionView'), 'ConsumptionView')
const RainwaterView = lazyScreen(() => import('./components/views/RainwaterView'), 'RainwaterView')
const DripView = lazyScreen(() => import('./components/views/DripView'), 'DripView')
const QualityView = lazyScreen(() => import('./components/views/QualityView'), 'QualityView')
const DroughtView = lazyScreen(() => import('./components/views/DroughtView'), 'DroughtView')
const NotificationsView = lazyScreen(() => import('./components/views/NotificationsView'), 'NotificationsView')
const OutagesView = lazyScreen(() => import('./components/views/OutagesView'), 'OutagesView')
const PlantsBlocksView = lazyScreen(() => import('./components/views/PlantsBlocksView'), 'PlantsBlocksView')
const LotsView = lazyScreen(() => import('./components/views/LotsView'), 'LotsView')
const VisitorsView = lazyScreen(() => import('./components/views/VisitorsView'), 'VisitorsView')

/**
 * Maps a menu view -> screen component.
 *
 * The keys match the entries of VIEW_PATHS (paths.js), which remains the single source of
 * truth for paths: the router pairs the two by view id.
 */
export const VIEW_COMPONENTS = {
  dashboard: DashboardView,
  farms: FarmsView,
  sources: SourcesView,
  irrigation: IrrigationView,
  consumption: ConsumptionView,
  rainwater: RainwaterView,
  maintenance: DripView,
  quality: QualityView,
  drought: DroughtView,
  outages: OutagesView,
  plants: PlantsBlocksView,
  lots: LotsView,
  visitors: VisitorsView,
  notifications: NotificationsView,
}

/**
 * The views expect notify / onNavigate; the module route provides them through the Outlet context
 * (<Outlet context={...}> in WaterSupplyLayout). This adapter forwards them as props, which
 * avoids changing the views and their tests, which still receive them directly.
 *
 * It lives in its own file, not in routes.jsx: the react-refresh/only-export-components
 * rule does not accept a file declaring a component without exporting it, while routes.jsx
 * only exports an array of routes (consumed by app/router.jsx through import.meta.glob).
 * The internal name stays "ModuleScreen", asserted by routes.test.jsx.
 */
export const withModuleUi = (View) => {
  function ModuleScreen() {
    return <View {...useOutletContext()} />
  }
  return ModuleScreen
}
