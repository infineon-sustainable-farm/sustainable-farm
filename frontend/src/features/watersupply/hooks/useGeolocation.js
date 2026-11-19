import { useCallback, useState } from 'react'

/** GeolocationPositionError codes -> what the user can do about it. */
const GEOLOCATION_ERRORS = {
  1: 'Location access refused (or page not served over HTTPS): allow it in the browser, or type the coordinates.',
  2: 'Position unavailable on this device: type the coordinates.',
  3: 'Location took too long: try again or type the coordinates.',
}

/** 6 decimals = about 10 cm: more would only be noise from the device. */
const round6 = (value) => Math.round(value * 1e6) / 1e6

/**
 * Current position of the device, through the browser Geolocation API.
 *
 * Used to fill in where a farm or a field is when it is created: the person is expected to be on
 * site. The accuracy depends on the device (GPS on a phone, a few kilometres from Wi-Fi/IP on a
 * desktop), so it is returned to be shown next to the coordinates. `locate()` never rejects: it
 * resolves `null` and exposes a readable `error` when no position can be obtained.
 *
 * TO BE REVIEWED: first version (device position only); a map picker and field boundaries are
 * expected to replace or complement it.
 */
export function useGeolocation() {
  const [locating, setLocating] = useState(false)
  const [error, setError] = useState(null)
  const [accuracy, setAccuracy] = useState(null)

  const locate = useCallback(() => {
    setError(null)
    setAccuracy(null)
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      setError('This browser cannot give a location: type the coordinates.')
      return Promise.resolve(null)
    }
    setLocating(true)
    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setLocating(false)
          setAccuracy(Math.round(position.coords.accuracy))
          resolve({ latitude: round6(position.coords.latitude), longitude: round6(position.coords.longitude) })
        },
        (positionError) => {
          setLocating(false)
          setError(GEOLOCATION_ERRORS[positionError.code] || positionError.message || 'Location failed.')
          resolve(null)
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 },
      )
    })
  }, [])

  return { locate, locating, error, accuracy }
}

/** Latitude / longitude typed by hand: null when valid, otherwise the message to show. */
export function coordinatesError(latitude, longitude) {
  if (latitude === '' && longitude === '') return null
  const lat = Number(latitude)
  const lng = Number(longitude)
  if (latitude === '' || longitude === '' || Number.isNaN(lat) || Number.isNaN(lng)) {
    return 'Give both latitude and longitude, or neither.'
  }
  if (lat < -90 || lat > 90) return 'The latitude is between -90 and 90.'
  if (lng < -180 || lng > 180) return 'The longitude is between -180 and 180.'
  return null
}

/** OpenStreetMap link to check a position on a map. */
export function mapUrl(latitude, longitude) {
  return `https://www.openstreetmap.org/?mlat=${latitude}&mlon=${longitude}#map=16/${latitude}/${longitude}`
}
