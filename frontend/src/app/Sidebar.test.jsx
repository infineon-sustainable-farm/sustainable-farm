// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { Cog } from 'lucide-react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import i18n from '../shared/i18n/i18n'
import Sidebar from './Sidebar'
import { otherModuleNav, shellModuleNav } from '../test/appFixtures'

function renderSidebar(path = '/machinery/equipment-registry', navs = [shellModuleNav, otherModuleNav]) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <Sidebar navs={navs} />
    </MemoryRouter>,
  )
  return screen.getByRole('navigation', { name: 'Modules' })
}

function MachineryFooter() {
  return <button type="button">machinery footer</button>
}

function WaterFooter() {
  return <p>water footer</p>
}

const navsWithFooters = [
  { ...shellModuleNav, sidebarFooter: MachineryFooter },
  { ...otherModuleNav, sidebarFooter: WaterFooter },
]

describe('Sidebar', () => {
  it('is the sidebar of the current module only, with a way back to all modules', () => {
    const nav = renderSidebar()
    // The logo shows the module name and leads back to the launcher.
    expect(within(nav).getByRole('link', { name: 'Machinery' })).toHaveAttribute('href', '/')
    expect(within(nav).getByRole('link', { name: 'Equipment Registry' })).toHaveAttribute('aria-current', 'page')
    expect(within(nav).getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('aria-current')
    expect(within(nav).getByRole('link', { name: 'All modules' })).toHaveAttribute('href', '/')
    // No link to the other modules nor to their pages.
    expect(within(nav).queryByRole('link', { name: 'Water supply' })).not.toBeInTheDocument()
    expect(within(nav).queryByRole('link', { name: 'Farms & Fields' })).not.toBeInTheDocument()
  })

  it('collapses every module outside a module and links the logo to the launcher', () => {
    const nav = renderSidebar('/unknown')
    expect(within(nav).queryByRole('link', { name: 'Equipment Registry' })).not.toBeInTheDocument()
    expect(within(nav).getByRole('link', { name: 'Sustainable Farm' })).toHaveAttribute('href', '/')
  })

  it('opens the drawer with the menu button and closes it on Escape, focus back on the button', () => {
    const nav = renderSidebar()
    const toggle = screen.getByRole('button', { name: 'Open navigation' })
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    expect(nav).toHaveAttribute('inert')

    fireEvent.click(toggle)
    expect(toggle).toHaveAttribute('aria-expanded', 'true')
    expect(toggle).toHaveAccessibleName('Close navigation')
    expect(nav).not.toHaveAttribute('inert')
    expect(nav.contains(document.activeElement)).toBe(true)
    expect(screen.getByTestId('sidebar-overlay')).toBeInTheDocument()

    fireEvent.keyDown(document, { key: 'Escape' })
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    expect(screen.queryByTestId('sidebar-overlay')).not.toBeInTheDocument()
    expect(document.activeElement).toBe(toggle)
  })

  it('closes the drawer on the overlay and when a link is followed', () => {
    renderSidebar()
    const toggle = screen.getByRole('button', { name: 'Open navigation' })
    fireEvent.click(toggle)
    fireEvent.click(screen.getByTestId('sidebar-overlay'))
    expect(toggle).toHaveAttribute('aria-expanded', 'false')

    fireEvent.click(toggle)
    fireEvent.click(screen.getByRole('link', { name: 'Spare Parts' }))
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    expect(screen.getByRole('link', { name: 'Spare Parts' })).toHaveAttribute('aria-current', 'page')
  })

  it('keeps Tab inside the open drawer', () => {
    renderSidebar()
    const toggle = screen.getByRole('button', { name: 'Open navigation' })
    fireEvent.click(toggle)
    const last = screen.getByRole('button', { name: 'Français' })
    last.focus()
    fireEvent.keyDown(document, { key: 'Tab' })
    expect(document.activeElement).toBe(toggle)
    fireEvent.keyDown(document, { key: 'Tab', shiftKey: true })
    expect(document.activeElement).toBe(last)
  })

  it('switches the interface language and <html lang>', async () => {
    const nav = renderSidebar()
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Deutsch' }))
    })
    // German and French load on demand.
    expect(await within(nav).findByRole('link', { name: 'Geräteverzeichnis' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Module' })).toBe(nav)
    expect(document.documentElement.lang).toBe('de')
    expect(i18n.resolvedLanguage).toBe('de')

    await act(async () => {
      fireEvent.click(within(screen.getByRole('group', { name: 'Sprache' })).getByRole('button', { name: 'Français' }))
    })
    expect(await within(nav).findByRole('link', { name: 'Tous les modules' })).toBeInTheDocument()
    expect(document.documentElement.lang).toBe('fr')
  })

  describe('sidebarFooter of the nav contract', () => {
    it('renders the footer of the current module only, inside the navigation above the language', () => {
      const nav = renderSidebar('/machinery/spare-parts', navsWithFooters)
      const footer = within(nav).getByRole('button', { name: 'machinery footer' })
      expect(within(nav).queryByText('water footer')).not.toBeInTheDocument()
      const language = within(nav).getByRole('group', { name: 'Language' })
      expect(footer.compareDocumentPosition(language) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    })

    it('switches the footer with the current module', () => {
      const nav = renderSidebar('/watersupply/farms', navsWithFooters)
      expect(within(nav).getByText('water footer')).toBeInTheDocument()
      expect(within(nav).queryByRole('button', { name: 'machinery footer' })).not.toBeInTheDocument()
    })

    it('renders no footer outside a module or for a module without one', () => {
      renderSidebar('/unknown', navsWithFooters)
      expect(screen.queryByTestId('sidebar-module-footer')).not.toBeInTheDocument()
      cleanup()
      renderSidebar('/machinery')
      expect(screen.queryByTestId('sidebar-module-footer')).not.toBeInTheDocument()
    })

    it('puts the footer in the drawer: inert while closed, reachable once opened', () => {
      const nav = renderSidebar('/machinery', navsWithFooters)
      const footerButton = within(nav).getByRole('button', { name: 'machinery footer' })
      expect(nav).toHaveAttribute('inert')
      fireEvent.click(screen.getByRole('button', { name: 'Open navigation' }))
      expect(nav).not.toHaveAttribute('inert')
      footerButton.focus()
      expect(document.activeElement).toBe(footerButton)
    })
  })

  describe('sections of the nav contract', () => {
    const sectionedNav = {
      ...otherModuleNav,
      items: [
        { to: '/watersupply', labelKey: 'watersupply:nav.dashboard', icon: Cog, end: true },
        { to: '/watersupply/farms', labelKey: 'watersupply:nav.farms', icon: Cog, sectionKey: 'watersupply:navSections.dataManagement' },
        { to: '/watersupply/sources', labelKey: 'watersupply:nav.sources', icon: Cog, sectionKey: 'watersupply:navSections.dataManagement' },
        { to: '/watersupply/irrigation', labelKey: 'watersupply:nav.irrigation', icon: Cog, sectionKey: 'watersupply:navSections.waterPlanning' },
      ],
    }
    const navs = [shellModuleNav, sectionedNav]

    it('groups the items of the current module under their headings, in order', () => {
      const nav = renderSidebar('/watersupply/farms', navs)
      const data = within(nav).getByRole('group', { name: 'Data management' })
      expect(within(data).getAllByRole('link').map((link) => link.textContent)).toEqual(['Farms & Fields', 'Water sources'])
      const planning = within(nav).getByRole('group', { name: 'Water planning' })
      expect(within(planning).getAllByRole('link').map((link) => link.textContent)).toEqual(['Irrigation planning'])
      const dashboard = within(nav).getByRole('link', { name: 'Dashboard' })
      expect(data.contains(dashboard)).toBe(false)
      expect(dashboard.compareDocumentPosition(data) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    })

    it('shows no heading for another module, and a module without sections stays flat', () => {
      const nav = renderSidebar('/machinery', navs)
      expect(within(nav).queryByText('Data management')).not.toBeInTheDocument()
      // The only group left is the language switcher: no section group.
      expect(within(nav).getAllByRole('group').map((group) => group.getAttribute('aria-label'))).toEqual(['Language'])
      expect(within(nav).getByRole('link', { name: 'Equipment Registry' })).toBeInTheDocument()
    })

    it('translates the headings', async () => {
      const nav = renderSidebar('/watersupply', navs)
      await act(async () => {
        await i18n.changeLanguage('de')
      })
      expect(within(nav).getByRole('group', { name: 'Datenverwaltung' })).toBeInTheDocument()
      await act(async () => {
        await i18n.changeLanguage('fr')
      })
      expect(within(nav).getByRole('group', { name: 'Planification de l’eau' })).toBeInTheDocument()
      await act(async () => {
        await i18n.changeLanguage('en')
      })
    })
  })
})
