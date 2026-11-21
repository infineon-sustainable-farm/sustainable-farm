import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'

/** Field coordinates are stored as text by the API: "lat, lng". */
export function formatFieldCoordinates(latitude, longitude) {
  return `${latitude}, ${longitude}`
}

export function parseFieldCoordinates(text) {
  const match = /^\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*$/.exec(text || '')
  return match ? { latitude: match[1], longitude: match[2] } : null
}

// The geolocation hook and coordinatesError (hooks/useGeolocation.js) return English sentences:
// they are matched here to their translation key, any other text (a browser message) is shown as is.
const GEO_MESSAGES = {
  'Location access refused (or page not served over HTTPS): allow it in the browser, or type the coordinates.': 'refused',
  'Position unavailable on this device: type the coordinates.': 'unavailable',
  'Location took too long: try again or type the coordinates.': 'timeout',
  'This browser cannot give a location: type the coordinates.': 'unsupported',
  'Location failed.': 'failed',
  'Give both latitude and longitude, or neither.': 'bothOrNeither',
  'The latitude is between -90 and 90.': 'latitudeRange',
  'The longitude is between -180 and 180.': 'longitudeRange',
}

/** Translates a message of the geolocation hook or of coordinatesError. */
export function useGeoMessage() {
  const { t } = useTranslation('watersupply')
  return useCallback(
    (message) => (message && GEO_MESSAGES[message] ? t(`farms.geo.${GEO_MESSAGES[message]}`) : message),
    [t],
  )
}
