// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { coordinatesError, mapUrl, useGeolocation } from './useGeolocation'

const original = Object.getOwnPropertyDescriptor(globalThis.navigator, 'geolocation')

function mockGeolocation(getCurrentPosition) {
  Object.defineProperty(globalThis.navigator, 'geolocation', { value: { getCurrentPosition }, configurable: true })
}

afterEach(() => {
  if (original) Object.defineProperty(globalThis.navigator, 'geolocation', original)
  else delete globalThis.navigator.geolocation
})

describe('useGeolocation', () => {
  it('returns the device position rounded to 6 decimals, with its accuracy', async () => {
    mockGeolocation(vi.fn((success) => success({ coords: { latitude: 10.63312345, longitude: -4.76123412, accuracy: 24.6 } })))
    const { result } = renderHook(() => useGeolocation())

    let position
    await act(async () => { position = await result.current.locate() })

    expect(position).toEqual({ latitude: 10.633123, longitude: -4.761234 })
    expect(result.current.accuracy).toBe(25)
    expect(result.current.error).toBeNull()
    expect(result.current.locating).toBe(false)
  })

  it('resolves null with a readable message when the user refuses', async () => {
    mockGeolocation(vi.fn((_success, failure) => failure({ code: 1, message: 'User denied Geolocation' })))
    const { result } = renderHook(() => useGeolocation())

    let position
    await act(async () => { position = await result.current.locate() })

    expect(position).toBeNull()
    expect(result.current.error).toMatch(/Location access refused/)
  })

  it('works without geolocation support', async () => {
    Object.defineProperty(globalThis.navigator, 'geolocation', { value: undefined, configurable: true })
    const { result } = renderHook(() => useGeolocation())

    await act(async () => { await result.current.locate() })

    expect(result.current.error).toMatch(/cannot give a location/)
  })
})

describe('coordinatesError', () => {
  it('accepts both empty or a valid pair', () => {
    expect(coordinatesError('', '')).toBeNull()
    expect(coordinatesError('10.63', '-4.77')).toBeNull()
  })

  it('refuses half a position and out-of-range values', () => {
    expect(coordinatesError('10.63', '')).toMatch(/both latitude and longitude/)
    expect(coordinatesError('95', '1')).toMatch(/latitude/)
    expect(coordinatesError('1', '-190')).toMatch(/longitude/)
  })

  it('builds an OpenStreetMap link', () => {
    expect(mapUrl(10.63, -4.77)).toBe('https://www.openstreetmap.org/?mlat=10.63&mlon=-4.77#map=16/10.63/-4.77')
  })
})
