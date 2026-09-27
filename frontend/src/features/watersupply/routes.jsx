import WaterSupplyLayout from './components/WaterSupplyLayout'
import { UnknownView } from './components/views/UnknownView'
import { VIEW_PATHS } from './paths'
import { VIEW_COMPONENTS, withModuleUi } from './ModuleScreen'

/**
 * Routes du module watersupply.
 *
 * Le module est monte sous /watersupply, comme machinery (/machinery) et plants (/plants) :
 * chaque module porte son propre menu et ses propres ecrans, et l'application les decouvre
 * automatiquement (frontend/src/app/router.jsx lit tous les features/<module>/routes.jsx).
 *
 * Les enfants sont derives de VIEW_PATHS : le menu et le routeur partagent donc la meme
 * source de verite et ne peuvent plus se desynchroniser.
 *
 * Ce fichier ne declare que du routage. Les composants et l'adaptateur d'interface sont
 * dans ModuleScreen.jsx : la regle react-refresh/only-export-components exige qu'un fichier
 * n'exporte que des composants, et celui-ci n'exporte qu'un tableau de routes. Le nom du
 * fichier et l'export par defaut sont imposes par la decouverte de app/router.jsx.
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

