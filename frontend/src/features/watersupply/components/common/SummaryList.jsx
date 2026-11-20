// Dot colours of the summary rows, from the tone tokens. The dot is decoration: the row text
// (or a badge) carries the state.
const DOTS = {
  success: 'bg-success-fg',
  warning: 'bg-warning-fg',
  error: 'bg-error-fg',
  info: 'bg-primary',
  neutral: 'bg-neutral-fg',
}

/** Stack of summary rows (alerts, activity, recommendations, balance lines). */
export function SummaryList({ children, label }) {
  return (
    <ul className="flex flex-col gap-3" aria-label={label}>
      {children}
    </ul>
  )
}

/**
 * One summary row: a coloured dot, a bold title, a muted detail under it and an optional element
 * on the right.
 */
export function SummaryItem({ tone = 'info', title, detail, aside }) {
  return (
    <li className="flex items-center gap-3 rounded-tile border border-border-subtle px-4 py-3">
      <span aria-hidden="true" className={`size-2.5 shrink-0 rounded-pill max-sm:hidden ${DOTS[tone] ?? DOTS.info}`} />
      <div className="min-w-0 flex-1 text-sm font-semibold">
        {title}
        {detail !== undefined && detail !== null && <p className="mt-1 font-medium text-text-muted">{detail}</p>}
      </div>
      {aside}
    </li>
  )
}
