/**
 * Pagination réutilisable (design system ws-*).
 * @param {number} page - page courante (1-based).
 * @param {number} pageCount - nombre total de pages.
 * @param {(next: number) => void} onPage - changement de page.
 * @param {number} total - nombre total d'éléments filtrés.
 * @param {string} [unit] - libellé du compteur.
 */
export function Pagination({ page, pageCount, onPage, total, unit = 'élément' }) {
  if (total === 0) return null
  return (
    <div className="ws-pagination">
      <span>
        {total} {unit}
        {total > 1 ? 's' : ''}
      </span>
      {pageCount > 1 && (
        <div className="ws-pagination-nav">
          <button type="button" disabled={page <= 1} onClick={() => onPage(page - 1)}>‹</button>
          <span>
            Page {page} / {pageCount}
          </span>
          <button type="button" disabled={page >= pageCount} onClick={() => onPage(page + 1)}>›</button>
        </div>
      )}
    </div>
  )
}