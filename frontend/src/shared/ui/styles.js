// Class strings shared by the components of the kit. Every value comes from the
// tokens of src/index.css (core section).

/** Keyboard focus mark: 2 px primary outline, shown on :focus-visible only (never on click). */
export const focusRing =
  'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring'

/** Colour transitions of interactive elements; off under prefers-reduced-motion. */
export const transition = 'transition duration-(--duration-fast) ease-standard motion-reduce:transition-none'

/**
 * Text field, select and textarea, as machinery: gray-300 border, rounded-lg, 12 x 8 px padding.
 * The border-input token (gray-300) is 1.5:1 on white, below the 3:1 asked for field borders: kept to
 * keep the machinery rendering, the owner decides at the demo (T50).
 */
export const controlBase = [
  'block w-full rounded-control border border-border-input bg-surface-raised px-3 py-2 text-text',
  'placeholder:text-text-muted',
  'aria-invalid:border-error-fg',
  'disabled:cursor-not-allowed disabled:bg-surface-muted disabled:text-text-muted',
  focusRing,
  transition,
].join(' ')
