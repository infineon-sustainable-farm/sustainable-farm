// @vitest-environment jsdom
import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useOutletContext } from 'react-router-dom'
import WaterSupplyLayout from './WaterSupplyLayout'

afterEach(cleanup)

/**
 * Test screen: it consumes the context provided by the shell (notify / onNavigate),
 * exactly like the routes.jsx adapter does, and without depending on a real view
 * (hence no API call).
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
  it('injects notify and onNavigate into the active screen', () => {
    renderModule()

    expect(screen.getByTestId('probe-props').textContent)
      .toBe('notify:function|onNavigate:function')
  })

  it('shows the module menu and the active screen', () => {
    renderModule()

    expect(screen.getByRole('navigation', { name: 'Water supply module' })).toBeTruthy()
    expect(screen.getByText('probe screen')).toBeTruthy()
  })

  it('passes notify to the screens and shows the confirmation banner', () => {
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
