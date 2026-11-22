// @vitest-environment jsdom
import { fireEvent, render, screen, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { collectModules } from './modules'
import { buildAppRoutes } from './routes'
import { useToast } from '../shared/ui'
import { fakeFiles, loadRealModuleFiles } from '../test/appFixtures'

const realFiles = await loadRealModuleFiles()

function renderAt(path, files = fakeFiles) {
  const router = createMemoryRouter(buildAppRoutes(collectModules(files)), { initialEntries: [path] })
  render(<RouterProvider router={router} />)
  return router
}

describe('application router', () => {
  // Generic on purpose: a module joins the launcher and the sidebar by exporting a `nav` from its
  // routes.jsx, without touching these tests.
  const realModules = collectModules(realFiles)
  const realNavs = realModules.map((module) => module.nav).filter(Boolean)

  it('shows the launcher at / with one link per integrated module', () => {
    renderAt('/', realFiles)
    const launcher = screen.getByRole('navigation', { name: 'Modules' })
    const hrefs = within(launcher)
      .getAllByRole('link')
      .map((link) => link.getAttribute('href'))
    expect(hrefs).toHaveLength(realNavs.length)
    expect(hrefs).toEqual(expect.arrayContaining(['/machinery', '/plants', '/watersupply']))
  })

  it('lists exactly the modules that export a nav, each once', () => {
    const ids = realNavs.map((nav) => nav.id)
    expect(new Set(ids).size).toBe(ids.length)
    expect(ids.length).toBeGreaterThanOrEqual(7)
  })

  it('opts every module with a nav into the shell', () => {
    expect(realModules.filter((module) => module.nav && !module.shell)).toEqual([])
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

  it('provides the kit toasts to every route, a module other than watersupply included', () => {
    function Notifier() {
      const toast = useToast()
      return (
        <button type="button" onClick={() => toast.success('Part saved')}>
          notify
        </button>
      )
    }
    const files = {
      ...fakeFiles,
      '../features/machinery/routes.jsx': {
        ...fakeFiles['../features/machinery/routes.jsx'],
        default: [{ path: 'machinery', element: <Notifier /> }],
      },
    }
    renderAt('/machinery', files)
    fireEvent.click(screen.getByRole('button', { name: 'notify' }))
    expect(screen.getByRole('region', { name: 'Notifications' })).toHaveTextContent('Part saved')
  })

  it('provides the kit toasts on the launcher', () => {
    renderAt('/')
    expect(screen.getByRole('region', { name: 'Notifications' })).toBeInTheDocument()
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
