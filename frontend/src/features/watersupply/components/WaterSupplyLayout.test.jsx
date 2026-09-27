// @vitest-environment jsdom
import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useOutletContext } from 'react-router-dom'
import WaterSupplyLayout from './WaterSupplyLayout'

afterEach(cleanup)

/**
 * Ecran de test : il consomme le contexte fourni par la coque (notify / onNavigate),
 * exactement comme le fait l'adaptateur de routes.jsx, et sans dependre d'une vraie vue
 * (donc sans appel API).
 */
function ProbeScreen() {
  const { notify, onNavigate } = useOutletContext()
  return (
    <>
      <p data-testid="probe-props">{`notify:${typeof notify}|onNavigate:${typeof onNavigate}`}</p>
      <button type="button" onClick={() => notify('Source saved')}>call-notify</button>
      <button type="button" onClick={() => onNavigate('sources')}>go-to-sources</button>
      <p>probe screen</p>
    </>
  )
}

function renderModule() {
  return render(
    <MemoryRouter initialEntries={['/watersupply']}>
      <Routes>
        <Route path="watersupply" element={<WaterSupplyLayout />}>
          <Route index element={<ProbeScreen />} />
          <Route path="sources" element={<ProbeScreen />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('WaterSupplyLayout', () => {
  it('injecte notify et onNavigate dans l ecran actif', () => {
    renderModule()

    expect(screen.getByTestId('probe-props').textContent)
      .toBe('notify:function|onNavigate:function')
  })

  it('affiche le menu du module et l ecran actif', () => {
    renderModule()

    expect(screen.getByRole('navigation', { name: 'Water supply module' })).toBeTruthy()
    expect(screen.getByText('probe screen')).toBeTruthy()
  })

  it('transmet notify aux ecrans et affiche le bandeau de confirmation', () => {
    renderModule()

    fireEvent.click(screen.getByText('call-notify'))

    expect(screen.getByRole('status').textContent).toBe('Source saved')
    expect(screen.getByRole('status').className).toContain('show')
  })

  it('transmet onNavigate aux ecrans (raccourcis du dashboard)', () => {
    renderModule()

    fireEvent.click(screen.getByText('go-to-sources'))

    expect(screen.getByRole('link', { name: /Water sources/ }).className).toContain('active')
  })
})
