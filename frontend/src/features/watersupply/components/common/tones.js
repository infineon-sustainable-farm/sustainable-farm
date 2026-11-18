/**
 * Colours of the former ws-tag / ws-summary-dot classes, as kit tones (Badge, StatusBadge).
 * green -> success, orange -> warning, red -> error, primary and blue -> info, gray -> neutral.
 */
export const TONES = {
  green: 'success',
  orange: 'warning',
  red: 'error',
  primary: 'info',
  blue: 'info',
  gray: 'neutral',
}

/** Kit tone of a former colour name, neutral when unknown. */
export function toneOf(color) {
  return TONES[color] ?? 'neutral'
}
