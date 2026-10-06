/**
 * Pagination of the watersupply module, driven by the values of useListControls
 * (page, pageCount, total) and styled with the module's ws-pagination classes.
 *
 * The application's shared component (shared/components/Pagination.jsx) expects a different
 * API (totalPages / onPageChange): the module therefore keeps its own, without touching the
 * shared one. It renders nothing as long as the list fits on a single page.
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
