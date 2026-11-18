// @vitest-environment jsdom
import { render, screen, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { collectModules } from './modules'
import { buildAppRoutes } from './routes'
import { fakeFiles, loadRealModuleFiles } from '../test/appFixtures'

const realFiles = await loadRealModuleFiles()

function renderAt(path, files = fakeFiles) {
  const router = createMemoryRouter(buildAppRoutes(collectModules(files)), { initialEntries: [path] })
  render(<RouterProvider router={router} />)
  return router
}

describe('application router', () => {
  it('shows the launcher at / with one link per integrated module', () => {
    renderAt('/', realFiles)
    const launcher = screen.getByRole('navigation', { name: 'Modules' })
    const links = within(launcher).getAllByRole('link')
    const targets = Object.fromEntries(links.map((link) => [link.textContent, link.getAttribute('href')]))
    expect(targets).toEqual({
      'Energy supply': '/energy-supply',
      Machinery: '/machinery',
      Plants: '/plants',
      'Product transformation': '/product-transformation',
      'Sales & marketing': '/salesmarketing',
      'Visitor management': '/visitormanagement',
      'Water supply': '/watersupply',
    })
  })

  it('keeps sitesecurity and cropstorage out of the navigation', () => {
    const navs = collectModules(realFiles).map((module) => module.nav?.id).filter(Boolean)
    expect(navs).not.toContain('sitesecurity')
    expect(navs).not.toContain('cropstorage')
    expect(navs).toHaveLength(7)
  })

  it('opts the migrated modules into the shell, the others not yet', () => {
    const shellIds = collectModules(realFiles)
      .filter((module) => module.shell)
      .map((module) => module.nav?.id)
    expect(shellIds).toEqual(['energysupply', 'machinery', 'plants', 'producttransformation', 'visitormanagement'])
  })

  it('renders the sidebar footer of the current shell module', () => {
    function Footer() {
      return <p>module footer</p>
    }
    const files = {
      ...fakeFiles,
      '../features/machinery/routes.jsx': {
        ...fakeFiles['../features/machinery/routes.jsx'],
        nav: { ...fakeFiles['../features/machinery/routes.jsx'].nav, sidebarFooter: Footer },
      },
    }
    renderAt('/machinery/spare-parts', files)
    const sidebar = screen.getByRole('navigation', { name: 'Modules' })
    expect(within(sidebar).getByText('module footer')).toBeInTheDocument()
  })

  it('shows the 404 page inside the shell for an unknown URL', () => {
    renderAt('/no-such-page')
    expect(screen.getByRole('heading', { level: 1, name: 'Page not found' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to modules' })).toHaveAttribute('href', '/')
    expect(screen.getByRole('navigation', { name: 'Modules' })).toBeInTheDocument()
  })

  it('renders an opted-in module inside AppLayout', () => {
    renderAt('/machinery/equipment-registry')
    expect(screen.getByText('equipment screen').closest('main')).toBeInTheDocument()
    const sidebar = screen.getByRole('navigation', { name: 'Modules' })
    expect(within(sidebar).getByRole('link', { name: 'Equipment Registry' })).toHaveAttribute('aria-current', 'page')
  })

  it('leaves a module that did not opt in on its own layout, without the shell', () => {
    renderAt('/watersupply')
    expect(screen.getByTestId('own-layout')).toHaveTextContent('water home')
    expect(screen.queryByRole('navigation', { name: 'Modules' })).not.toBeInTheDocument()
  })

  it('routes modules without nav as before', () => {
    renderAt('/sitesecurity')
    expect(screen.getByText('security app')).toBeInTheDocument()
  })

  it('shows the error state with a way back when a screen throws', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    function Broken() {
      throw new Error('boom')
    }
    const files = {
      ...fakeFiles,
      '../features/machinery/routes.jsx': {
        ...fakeFiles['../features/machinery/routes.jsx'],
        default: [{ path: 'machinery', element: <Broken /> }],
      },
    }
    renderAt('/machinery', files)
    expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong')
    expect(screen.getByRole('link', { name: 'Back to modules' })).toHaveAttribute('href', '/')
    // The error renders inside the shell: the sidebar stays.
    expect(screen.getByRole('navigation', { name: 'Modules' })).toBeInTheDocument()
  })
})
