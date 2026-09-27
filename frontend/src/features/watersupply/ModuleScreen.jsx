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
 * Association vue du menu -> composant d'ecran.
 *
 * Les cles correspondent aux entrees de VIEW_PATHS (paths.js), qui reste la source de
 * verite des chemins : le routeur apparie les deux par identifiant de vue.
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
 * Les vues attendent notify / onNavigate ; la coque les fournit par le contexte de l'Outlet
 * (<Outlet context={...}> dans WaterSupplyLayout). Cet adaptateur les transmet en proprietes,
 * ce qui evite de modifier les vues et leurs tests, qui les recoivent toujours directement.
 *
 * Il vit dans son propre fichier, et non dans routes.jsx : la regle
 * react-refresh/only-export-components n'accepte pas qu'un fichier declare un composant
 * sans l'exporter, or routes.jsx n'exporte qu'un tableau de routes (consomme par
 * app/router.jsx via import.meta.glob). Le nom interne reste "ModuleScreen", verifie
 * par routes.test.jsx.
 */
export const withModuleUi = (View) => {
  function ModuleScreen() {
    return <View {...useOutletContext()} />
  }
  return ModuleScreen
}
