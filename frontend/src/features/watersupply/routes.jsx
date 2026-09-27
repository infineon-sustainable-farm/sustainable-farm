import { useOutletContext } from 'react-router-dom'
import WaterSupplyLayout from './components/WaterSupplyLayout'
import { UnknownView } from './components/views/UnknownView'
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
import { VIEW_PATHS } from './paths'

/**
 * Routes du module watersupply.
 *
 * Le module est monte sous /watersupply, comme machinery (/machinery) et plants (/plants) :
 * chaque module porte son propre menu et ses propres ecrans, et l'application les decouvre
 * automatiquement (frontend/src/app/router.jsx lit tous les features/<module>/routes.jsx).
 *
 * Les enfants sont derives de VIEW_PATHS : le menu et le routeur partagent donc la meme
 * source de verite et ne peuvent plus se desynchroniser.
 */
const VIEW_COMPONENTS = {
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
 * Les vues attendent notify / onNavigate ; la coque les fournit par le contexte de l'Outlet
 * (<Outlet context={...}> dans WaterSupplyLayout). Cet adaptateur les transmet en proprietes,
 * ce qui evite de modifier les vues et leurs tests, qui les recoivent toujours directement.
 */
const withModuleUi = (View) => {
  function ModuleScreen() {
    return <View {...useOutletContext()} />
  }
  return ModuleScreen
}

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

