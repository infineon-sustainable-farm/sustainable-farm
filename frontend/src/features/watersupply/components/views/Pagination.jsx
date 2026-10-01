/**
 * Pagination du module watersupply, pilotee par les valeurs de useListControls
 * (page, pageCount, total) et habillee par les classes ws-pagination du module.
 *
 * Le composant partage de l'application (shared/components/Pagination.jsx) attend une autre
 * API (totalPages / onPageChange) : le module garde donc la sienne, sans modifier le partage.
 * Elle ne s'affiche pas tant que la liste tient sur une seule page.
 */
export function Pagination({ page, pageCount, onPage, total = 0, unit = 'item' }) {
  if (!pageCount || pageCount <= 1) {
    return null
  }

  const plural = total > 1 ? 's' : ''

  return (
    <div className="ws-pagination">
      <span>
        {total} {unit}
        {plural} — page {page} / {pageCount}
      </span>
      <div className="ws-pagination-nav">
        <button
          type="button"
          aria-label="Previous page"
          disabled={page <= 1}
          onClick={() => onPage(page - 1)}
        >
          ‹
        </button>
        <button
          type="button"
          aria-label="Next page"
          disabled={page >= pageCount}
          onClick={() => onPage(page + 1)}
        >
          ›
        </button>
      </div>
    </div>
  )
}
