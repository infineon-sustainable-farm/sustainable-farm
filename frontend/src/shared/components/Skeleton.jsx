/**
 * Loading skeleton conforming to the ws-* design system.
 * Used while lists/panels load, in place of the spinner when possible.
 * @param {number} [rows] - number of simulated rows.
 * @param {number} [height] - height of each row (px).
 */
export function Skeleton({ rows = 3, height = 44 }) {
  return (
    <div aria-hidden="true" style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="ws-skeleton" style={{ height, borderRadius: '8px' }} />
      ))}
      <style>{`.ws-skeleton{background:linear-gradient(90deg,var(--ws-line,#e2e9e7) 25%,rgba(226,233,231,.45) 50%,var(--ws-line,#e2e9e7) 75%);background-size:200% 100%;animation:ws-shimmer 1.3s infinite}@keyframes ws-shimmer{to{background-position:-200% 0}}`}</style>
    </div>
  )
}