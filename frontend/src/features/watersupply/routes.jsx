import WaterSupplyLayout from './components/WaterSupplyLayout'
import { UnknownView } from './components/views/UnknownView'
import { VIEW_PATHS } from './paths'
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

