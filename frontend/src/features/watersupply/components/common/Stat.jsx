/** Small figure with its label (former metric pill): bordered tile, upper-case label, value. */
export function Stat({ label, value, hint }) {
  return (
    <div className="rounded-tile border border-border-subtle bg-surface-raised px-5 py-4">
      <p className="text-xs font-semibold tracking-wide text-text-muted uppercase">{label}</p>
      <p className="mt-1 text-lg font-semibold">{value}</p>
      {hint && <p className="mt-1 text-xs text-text-muted">{hint}</p>}
    </div>
  )
}

/** Grid of Stat tiles: one column on phones, up to four. */
export function StatGrid({ children }) {
  return <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{children}</div>
}
