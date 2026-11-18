// @vitest-environment jsdom
import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation, useOutletContext } from 'react-router-dom'
import { ToastProvider } from '../../../shared/ui'
import WaterSupplyLayout from './WaterSupplyLayout'

/**
 * Test screen: it consumes the context provided by the module route (notify / onNavigate),
 * exactly like the withModuleUi adapter does, without depending on a real view (no API call).
 */
function ProbeScreen() {
  const { notify, onNavigate } = useOutletContext()
  const { pathname } = useLocation()
  return (
    <>
      <p data-testid="probe-props">{`notify:${typeof notify}|onNavigate:${typeof onNavigate}`}</p>
      <p data-testid="probe-path">{pathname}</p>
      <button type="button" onClick={() => notify('Source saved')}>call-notify</button>
      <button type="button" onClick={() => onNavigate('sources')}>go-to-sources</button>
    </>
  )
}

// The toasts come from the application root (app/RootLayout), provided here the same way.
function renderModule() {
  return render(
    <ToastProvider>
      <MemoryRouter initialEntries={['/watersupply']}>
        <Routes>
          <Route path="watersupply" element={<WaterSupplyLayout />}>
            <Route index element={<ProbeScreen />} />
            <Route path="sources" element={<ProbeScreen />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </ToastProvider>,
  )
}

describe('WaterSupplyLayout', () => {
  it('injects notify and onNavigate into the active screen', () => {
    renderModule()

    expect(screen.getByTestId('probe-props').textContent).toBe('notify:function|onNavigate:function')
  })

  it('renders no menu of its own: the application shell carries the navigation', () => {
    renderModule()

    expect(screen.queryByRole('navigation')).toBeNull()
  })

  it('shows the confirmation passed to notify as a kit toast', () => {
    renderModule()

    fireEvent.click(screen.getByText('call-notify'))

    expect(screen.getByRole('region', { name: 'Notifications' })).toHaveTextContent('Source saved')
  })

  it('opens another screen of the module with onNavigate (dashboard shortcuts)', () => {
    renderModule()

    fireEvent.click(screen.getByText('go-to-sources'))

    expect(screen.getByTestId('probe-path').textContent).toBe('/watersupply/sources')
  })
})
