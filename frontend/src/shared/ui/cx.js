/** Joins class names, skipping falsy values. */
export function cx(...classes) {
  return classes.filter(Boolean).join(' ')
}
