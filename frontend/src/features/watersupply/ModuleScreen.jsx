import { useOutletContext } from 'react-router-dom'
import { DashboardView } from './components/views/DashboardView'
import { FarmsView } from './components/views/FarmsView'
import { SourcesView } from './components/views/SourcesView'
import { IrrigationView } from './components/views/IrrigationView'
import { ConsumptionView } from './components/views/ConsumptionView'
import { RainwaterView } from './components/views/RainwaterView'
import { DripView } from './components/views/DripView'
import { QualityView } from './components/views/QualityView'
import { DroughtView } from './components/views/DroughtView'
import { NotificationsView } from './components/views/NotificationsView'

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
  notifications: NotificationsView,
}

/**
 * The views expect notify / onNavigate; the shell provides them through the Outlet context
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
