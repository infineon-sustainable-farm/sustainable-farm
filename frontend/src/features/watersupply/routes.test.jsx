import { describe, expect, it } from 'vitest'
import { matchRoutes } from 'react-router-dom'
import routes, * as routesFile from './routes'
import { nav } from './routes'
import { VIEW_PATHS, WATERSUPPLY_BASE, viewUrl } from './paths'
import { collectModules } from '../../app/modules'
import { buildAppRoutes } from '../../app/routes'
import NotFoundPage from '../../app/NotFoundPage'

const appRoutes = buildAppRoutes(collectModules({ '../features/watersupply/routes.jsx': routesFile }))

/** Element of the deepest route matched by a URL in the application route tree. */
function screenAt(url) {
  const matches = matchRoutes(appRoutes, url)
  return matches?.at(-1).route.element
}

describe('watersupply module routes', () => {
  const [moduleRoute] = routes

  it('mounts the module under /watersupply, inside the application shell', () => {
    expect(routes).toHaveLength(1)
    expect(WATERSUPPLY_BASE).toBe('/watersupply')
    expect(moduleRoute.path).toBe('watersupply')
    expect(nav.shell).toBe(true)
  })

  it('declares one screen per menu entry, with a single default route', () => {
    const indexRoutes = moduleRoute.children.filter((child) => child.index === true)
    const paths = moduleRoute.children
      .filter((child) => child.path)
      .map((child) => child.path)
      .sort()
    const menuPaths = Object.values(VIEW_PATHS)
      .filter((relativePath) => relativePath !== '')
      .sort()

    expect(moduleRoute.children).toHaveLength(Object.keys(VIEW_PATHS).length)
    expect(indexRoutes).toHaveLength(1)
    expect(paths).toEqual(menuPaths)
  })

  it('reaches every screen by its URL, wrapped to receive notify and onNavigate', () => {
    Object.keys(VIEW_PATHS).forEach((viewId) => {
      const element = screenAt(viewUrl(viewId))
      expect(element.type.name).toBe('ModuleScreen')
    })
  })

  it('lists the screens in the sidebar in the order of the former module menu', () => {
    expect(nav.items.map((item) => item.to)).toEqual(Object.keys(VIEW_PATHS).map(viewUrl))
    expect(nav.items.map((item) => item.labelKey)).toEqual(Object.keys(VIEW_PATHS).map((id) => `watersupply:nav.${id}`))
    expect(nav.items.filter((item) => item.end).map((item) => item.to)).toEqual([WATERSUPPLY_BASE])
  })

  it('groups the entries under the four headings of the former module menu', () => {
    const sections = nav.items.map((item) => [item.to.replace(WATERSUPPLY_BASE, ''), item.sectionKey?.replace('watersupply:navSections.', '')])
    expect(sections).toEqual([
      ['', undefined],
      ['/farms', 'dataManagement'],
      ['/sources', 'dataManagement'],
      ['/irrigation', 'waterPlanning'],
      ['/consumption', 'waterPlanning'],
      ['/rainwater', 'waterPlanning'],
      ['/maintenance', 'waterPlanning'],
      ['/quality', 'waterPlanning'],
      ['/drought', 'waterPlanning'],
      ['/outages', 'waterPlanning'],
      ['/plants-blocks', 'integrations'],
      ['/lots', 'integrations'],
      ['/visitors', 'integrations'],
      ['/notifications', 'operations'],
    ])
  })

  it('shows the shell "not found" page on an unknown URL, without silent redirection', () => {
    expect(screenAt('/watersupply/no-such-screen').type).toBe(NotFoundPage)
  })
})
