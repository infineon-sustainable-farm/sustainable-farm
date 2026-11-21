// @vitest-environment jsdom
import { act, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import i18n from '../shared/i18n/i18n'
import { HOME_PHOTO_SIZES, HOME_PHOTO_SRCSET } from './homePhoto'
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
  it('shows the app name, the headline, the language switcher and one card per module, with no sidebar', () => {
    const tiles = renderLauncher()
    expect(screen.getByText('Sustainable Farm')).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { level: 1, name: 'From orchard to export, one farm platform.' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('group', { name: 'Language' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'English' })).toHaveAttribute('aria-pressed', 'true')
    expect(tiles.getAllByRole('link')).toHaveLength(8)
    expect(tiles.getByRole('link', { name: 'Machinery' })).toHaveAttribute('href', '/machinery')
    expect(screen.queryByRole('button', { name: 'Open navigation' })).not.toBeInTheDocument()
  })

  it('serves the landscape photo as a decorative, responsive LCP image', () => {
    const { container } = render(
      <MemoryRouter>
        <LauncherPage navs={navs} />
      </MemoryRouter>,
    )
    const photo = container.querySelector('img[srcset]')
    expect(photo).toHaveAttribute('alt', '')
    expect(photo).toHaveAttribute('fetchpriority', 'high')
    expect(photo).toHaveAttribute('width', '1440')
    expect(photo).toHaveAttribute('height', '900')
    expect(photo).toHaveAttribute('srcset', HOME_PHOTO_SRCSET)
    expect(photo).toHaveAttribute('sizes', HOME_PHOTO_SIZES)
    // decorative: not exposed to assistive technologies
    expect(screen.queryAllByRole('img')).toHaveLength(0)
  })

  it('preloads the same photo candidates from index.html', () => {
    const html = readFileSync(resolve(import.meta.dirname, '../../index.html'), 'utf8')
    expect(html).toContain(`'${HOME_PHOTO_SRCSET}'`)
    expect(html).toContain(`'${HOME_PHOTO_SIZES}'`)
  })

  it('names the tiles in German', async () => {
    await act(() => i18n.changeLanguage('de'))
    const tiles = renderLauncher()
    expect(tiles.getByRole('link', { name: 'Maschinen' })).toHaveAttribute('href', '/machinery')
    expect(tiles.getByRole('link', { name: 'Wasserversorgung' })).toHaveAttribute('href', '/watersupply')
    expect(screen.getByRole('button', { name: 'Deutsch' })).toHaveAttribute('aria-pressed', 'true')
    expect(
      screen.getByRole('heading', { level: 1, name: 'Von der Plantage bis zum Export, eine Plattform.' }),
    ).toBeInTheDocument()
  })

  it('names the tiles in French', async () => {
    await act(() => i18n.changeLanguage('fr'))
    const tiles = renderLauncher()
    expect(tiles.getByRole('link', { name: 'Transformation des produits' })).toHaveAttribute(
      'href',
      '/product-transformation',
    )
    expect(tiles.getByRole('link', { name: 'Gestion des visiteurs' })).toHaveAttribute('href', '/visitormanagement')
    expect(screen.getByRole('group', { name: 'Langue' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Du verger à l’export, une seule plateforme.' })).toBeInTheDocument()
  })

  it('switches the language from the switcher', async () => {
    await act(() => i18n.changeLanguage('en'))
    const tiles = renderLauncher()
    await act(() => screen.getByRole('button', { name: 'Français' }).click())
    expect(screen.getByRole('button', { name: 'Français' })).toHaveAttribute('aria-pressed', 'true')
    expect(tiles.getByRole('link', { name: 'Machines' })).toHaveAttribute('href', '/machinery')
    await act(() => i18n.changeLanguage('en'))
  })
})
