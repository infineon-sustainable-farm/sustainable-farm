const FOCUSABLE = [
  'a[href]',
  'area[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  'iframe',
  'audio[controls]',
  'video[controls]',
  '[contenteditable]:not([contenteditable="false"])',
  '[tabindex]:not([tabindex="-1"])',
].join(',')

/** Elements of `container` that Tab can reach, in document order. */
export function getFocusable(container) {
  if (!container) return []
  return [...container.querySelectorAll(FOCUSABLE)].filter((element) => !element.closest('[inert]'))
}
