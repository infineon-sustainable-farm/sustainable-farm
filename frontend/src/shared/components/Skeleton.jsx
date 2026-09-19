/**
 * Squelette de chargement (skeleton) conforme au design system ws-*.
 * Utilisé pendant le chargement des listes/panneaux à la place du spinner quand possible.
 * @param {number} [rows] - nombre de lignes simulées.
 * @param {number} [height] - hauteur de chaque ligne (px).
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