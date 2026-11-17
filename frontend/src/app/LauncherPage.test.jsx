// @vitest-environment jsdom
import { act, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import i18n from '../shared/i18n/i18n'
import LauncherPage from './LauncherPage'
import { collectModules } from './modules'
import { loadRealModuleFiles } from '../test/appFixtures'

const navs = collectModules(await loadRealModuleFiles())
  .map((module) => module.nav)
  .filter(Boolean)

function renderLauncher() {
  render(
    <MemoryRouter>
      <LauncherPage navs={navs} />
    </MemoryRouter>,
  )
  return within(screen.getByRole('navigation', { name: i18n.t('launcher.modules') }))
}

describe('LauncherPage', () => {
  it('shows the app name, the language switcher and one tile per module, with no sidebar', () => {
    const tiles = renderLauncher()
    expect(screen.getByRole('heading', { level: 1, name: 'Sustainable Farm' })).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Language' })).toBeInTheDocument()
    expect(tiles.getAllByRole('link')).toHaveLength(7)
    expect(tiles.getByRole('link', { name: 'Machinery' })).toHaveAttribute('href', '/machinery')
    expect(screen.queryByRole('button', { name: 'Open navigation' })).not.toBeInTheDocument()
  })

  it('names the tiles in German', async () => {
    await act(() => i18n.changeLanguage('de'))
    const tiles = renderLauncher()
    expect(tiles.getByRole('link', { name: 'Maschinen' })).toHaveAttribute('href', '/machinery')
    expect(tiles.getByRole('link', { name: 'Wasserversorgung' })).toHaveAttribute('href', '/watersupply')
    expect(screen.getByRole('combobox', { name: 'Sprache' })).toHaveValue('de')
  })

  it('names the tiles in French', async () => {
    await act(() => i18n.changeLanguage('fr'))
    const tiles = renderLauncher()
    expect(tiles.getByRole('link', { name: 'Transformation des produits' })).toHaveAttribute(
      'href',
      '/product-transformation',
    )
    expect(tiles.getByRole('link', { name: 'Gestion des visiteurs' })).toHaveAttribute('href', '/visitormanagement')
  })
})
