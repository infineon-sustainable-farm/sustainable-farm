// @vitest-environment jsdom
import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import WaterSupplySidebar from './WaterSupplySidebar'
import { VIEW_PATHS, viewUrl } from '../paths'

afterEach(cleanup)

function renderAt(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <WaterSupplySidebar />
    </MemoryRouter>,
  )
}

describe('WaterSupplySidebar', () => {
  it('declare un lien pour chaque ecran du module, sous /watersupply', () => {
    renderAt('/watersupply')

    const hrefs = screen.getAllByRole('link').map((link) => link.getAttribute('href'))
    expect(hrefs).toEqual(Object.keys(VIEW_PATHS).map((viewId) => viewUrl(viewId)))
  })

  it('pointe le dashboard sur la racine du module', () => {
    renderAt('/watersupply')

    expect(screen.getByRole('link', { name: 'Dashboard' }).getAttribute('href')).toBe('/watersupply')
    expect(screen.getByRole('link', { name: /Farms & Fields/ }).getAttribute('href'))
      .toBe('/watersupply/farms')
  })

  it('marque l ecran courant comme actif', () => {
    renderAt('/watersupply/farms')

    expect(screen.getByRole('link', { name: /Farms & Fields/ }).className).toContain('active')
    expect(screen.getByRole('link', { name: 'Dashboard' }).className).not.toContain('active')
  })

  it('ne marque pas le dashboard comme actif quand un ecran enfant est ouvert', () => {
    renderAt('/watersupply/sources')

    expect(screen.getByRole('link', { name: 'Dashboard' }).className).not.toContain('active')
    expect(screen.getByRole('link', { name: /Water sources/ }).className).toContain('active')
  })
})
