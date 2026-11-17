// Class strings of the application shell. Every value comes from the tokens of src/index.css.

/** Colour transitions, off under prefers-reduced-motion (same as the kit). */
export const shellTransition = 'transition duration-(--duration-fast) ease-standard motion-reduce:transition-none'

/** Keyboard focus on the primary sidebar: a white outline inside the element, on :focus-visible only. */
export const focusOnPrimary =
  'focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-on-primary'

/** Keyboard focus on the page background: the kit's 2 px primary outline. */
export const focusOnSurface =
  'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring'

/** A link that looks like the kit's primary button (navigation, so not a <button>). */
export const primaryLinkButton = [
  'inline-flex min-h-10 items-center justify-center gap-2 rounded-control bg-primary px-4 py-2 text-on-primary',
  'pointer-coarse:min-h-11 active:scale-98 active:bg-primary-pressed motion-reduce:active:scale-100',
  focusOnSurface,
  shellTransition,
].join(' ')
