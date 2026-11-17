import AppLayout from './AppLayout'
import LauncherPage from './LauncherPage'
import NotFoundPage from './NotFoundPage'
import RootLayout from './RootLayout'
import RouteError from './RouteError'

/**
 * Route tree of the application, built from the modules (see modules.js):
 *
 * - `/`: the launcher, full screen, one tile per integrated module;
 * - modules that opted into the shell: under AppLayout (global sidebar);
 * - the other modules: as before, each with its own layout;
 * - any unknown URL: the 404 page inside the shell.
 *
 * @param {ReturnType<import('./modules').collectModules>} modules
 */
export function buildAppRoutes(modules) {
  const navs = modules.map((module) => module.nav).filter(Boolean)
  const shellRoutes = modules.filter((module) => module.shell).flatMap((module) => module.routes)
  const ownLayoutRoutes = modules.filter((module) => !module.shell).flatMap((module) => module.routes)

  return [
    {
      path: '/',
      element: <RootLayout />,
      errorElement: <RouteError fullScreen />,
      children: [
        { index: true, element: <LauncherPage navs={navs} /> },
        ...ownLayoutRoutes,
        {
          element: <AppLayout navs={navs} />,
          children: [
            {
              errorElement: <RouteError />,
              children: [...shellRoutes, { path: '*', element: <NotFoundPage /> }],
            },
          ],
        },
      ],
    },
  ]
}
