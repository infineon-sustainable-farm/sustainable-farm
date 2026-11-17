// @vitest-environment jsdom
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import i18n from '../shared/i18n/i18n'
import Sidebar from './Sidebar'
import { otherModuleNav, shellModuleNav } from '../test/appFixtures'

function renderSidebar(path = '/machinery/equipment-registry') {
  render(
    <MemoryRouter initialEntries={[path]}>
      <Sidebar navs={[shellModuleNav, otherModuleNav]} />
    </MemoryRouter>,
  )
  return screen.getByRole('navigation', { name: 'Modules' })
}

describe('Sidebar', () => {
  it('shows one group per module, the current one expanded with its links', () => {
    const nav = renderSidebar()
    expect(within(nav).getByRole('link', { name: 'Machinery' })).toHaveAttribute('href', '/machinery')
    expect(within(nav).getByRole('link', { name: 'Water supply' })).toHaveAttribute('href', '/watersupply')
    expect(within(nav).getByRole('link', { name: 'Equipment Registry' })).toHaveAttribute('aria-current', 'page')
    expect(within(nav).getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('aria-current')
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
    const select = screen.getByRole('combobox', { name: 'Language' })
    select.focus()
    fireEvent.keyDown(document, { key: 'Tab' })
    expect(document.activeElement).toBe(toggle)
    fireEvent.keyDown(document, { key: 'Tab', shiftKey: true })
    expect(document.activeElement).toBe(select)
  })

  it('switches the interface language and <html lang>', async () => {
    const nav = renderSidebar()
    await act(async () => {
      fireEvent.change(screen.getByRole('combobox', { name: 'Language' }), { target: { value: 'de' } })
    })
    expect(within(nav).getByRole('link', { name: 'Geräteverzeichnis' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Module' })).toBe(nav)
    expect(document.documentElement.lang).toBe('de')
    expect(i18n.resolvedLanguage).toBe('de')

    await act(async () => {
      fireEvent.change(screen.getByRole('combobox', { name: 'Sprache' }), { target: { value: 'fr' } })
    })
    expect(within(nav).getByRole('link', { name: 'Approvisionnement en eau' })).toBeInTheDocument()
    expect(document.documentElement.lang).toBe('fr')
  })
})
