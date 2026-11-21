import { useLocation } from 'react-router-dom'

/** Current URL of the test router (path and query string), for the tests of deep links. */
export function LocationProbe() {
  const location = useLocation()
  return <span data-testid="location">{location.pathname + location.search}</span>
}
