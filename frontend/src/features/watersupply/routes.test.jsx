import { describe, expect, it } from 'vitest'
import routes from './routes'
import { VIEW_PATHS, WATERSUPPLY_BASE } from './paths'

/**
 * The wiring of the module into the application is asserted here: this is exactly what
 * was missing (an empty routes.jsx), so the module was absent from the production bundle.
 */
describe('watersupply module routes', () => {
  const [moduleRoute] = routes

  it('mounts the module under /watersupply, like machinery and plants', () => {
    expect(routes).toHaveLength(1)
    expect(WATERSUPPLY_BASE).toBe('/watersupply')
    expect(moduleRoute.path).toBe('watersupply')
  })

  it('declares one screen per menu entry, with a single default route', () => {
    const declared = moduleRoute.children.filter((child) => child.path !== '*')
    const indexRoutes = declared.filter((child) => child.index === true)
    const paths = declared
      .filter((child) => child.path)
      .map((child) => child.path)
      .sort()
    const menuPaths = Object.values(VIEW_PATHS)
      .filter((relativePath) => relativePath !== '')
      .sort()

    expect(declared).toHaveLength(Object.keys(VIEW_PATHS).length)
    expect(indexRoutes).toHaveLength(1)
    expect(paths).toEqual(menuPaths)
  })

  it('wraps every screen to pass it notify and onNavigate', () => {
    // The views expect these two props: they come from the module's Outlet context,
    // passed by the adapter (see withModuleUi in routes.jsx).
    const declared = moduleRoute.children.filter((child) => child.path !== '*')

    declared.forEach((child) => {
      expect(typeof child.element.type).toBe('function')
      expect(child.element.type.name).toBe('ModuleScreen')
    })
  })

  it('shows a "not found" screen on an unknown URL, without silent redirection', () => {
    const catchAll = moduleRoute.children.at(-1)

    expect(catchAll.path).toBe('*')
    expect(catchAll.element).toBeTruthy()
  })
})
