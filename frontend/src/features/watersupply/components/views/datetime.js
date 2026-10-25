/**
 * Date helpers of the integration screens.
 *
 * A `datetime-local` input holds a LOCAL time without zone while the API speaks UTC instants:
 * converting through `Date` in both directions keeps an edited period identical to the one
 * that was saved (slicing the ISO string would shift it by the browser's offset).
 */
const pad = (value) => String(value).padStart(2, '0')

/** ISO instant -> value of a `datetime-local` input, in local time. */
export function toLocalInput(iso) {
  if (!iso) return ''
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** Value of a `datetime-local` input -> ISO instant, or null when empty. */
export function fromLocalInput(value) {
  return value ? new Date(value).toISOString() : null
}

/** Short display of an instant: "2026-10-20 08:00" (UTC, like the rest of the module). */
export function formatInstant(iso) {
  return iso ? String(iso).replace('T', ' ').slice(0, 16) : '—'
}

/** Day in yyyy-MM-dd, UTC (the API's format for day parameters). */
export function isoDay(date = new Date()) {
  return date.toISOString().slice(0, 10)
}

/** First day of the current month, UTC. */
export function monthStartDay(date = new Date()) {
  return `${date.toISOString().slice(0, 7)}-01`
}
