// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { CREATE_PARAM, ownerCreatePath, useCreateParam } from './useCreateParam'

describe('ownerCreatePath', () => {
  it('adds the create parameter to the screen path', () => {
    expect(ownerCreatePath('/machinery/equipment-registry')).toBe('/machinery/equipment-registry?create=1')
    expect(ownerCreatePath('/watersupply/farms', 'zone')).toBe('/watersupply/farms?create=zone')
  })

  it('keeps the parameters already in the path', () => {
    expect(ownerCreatePath('/plants/harvest-maturity?block=A')).toBe('/plants/harvest-maturity?block=A&create=1')
  })
})

function Probe({ value }) {
  const [open, clear] = useCreateParam(value)
  const location = useLocation()
  return (
    <>
      <span data-testid="open">{String(open)}</span>
      <button type="button" onClick={clear}>
        clear
      </button>
      <span data-testid="location">{location.pathname + location.search}</span>
    </>
  )
}

const isOpen = () => screen.getByTestId('open').textContent === 'true'

function renderAt(url, value) {
  return render(
    <MemoryRouter initialEntries={[url]}>
      <Probe value={value} />
    </MemoryRouter>,
  )
}

describe('useCreateParam', () => {
  it('is open when the parameter has the default value', () => {
    renderAt('/screen?create=1')
    expect(isOpen()).toBe(true)
  })

  it('is closed without the parameter or with the value of another dialog', () => {
    renderAt('/screen')
    expect(isOpen()).toBe(false)
    cleanup()
    renderAt('/screen?create=zone')
    expect(isOpen()).toBe(false)
  })

  it('reads the value of its own dialog', () => {
    renderAt('/screen?create=field', 'field')
    expect(isOpen()).toBe(true)
  })

  it('clears the parameter and keeps the others', () => {
    renderAt('/screen?block=A&create=1')
    fireEvent.click(screen.getByRole('button', { name: 'clear' }))
    expect(isOpen()).toBe(false)
    expect(screen.getByTestId('location')).toHaveTextContent('/screen?block=A')
    expect(screen.getByTestId('location').textContent).not.toContain(CREATE_PARAM)
  })
})
