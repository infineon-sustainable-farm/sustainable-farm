/**
 * Reusable pagination - shared across modules.
 * Watersupply API: 1-based page, pageCount, total, unit.
 * Machinery API (alias): 0-based page via totalPages + onPageChange.
 * @param {number} page - page courante (1-based, ou 0-based si totalPages est fourni).
 * @param {number} [pageCount] - nombre total de pages (1-based).
 * @param {(next: number) => void} [onPage] - changement de page (1-based).
 * @param {number} [total] - nombre total d'éléments.
 * @param {string} [unit] - libellé du compteur.
 * @param {number} [totalPages] - alias machinery : nombre total de pages (page 0-based).
 * @param {(next: number) => void} [onPageChange] - alias machinery : changement de page (0-based).
 */
export function Pagination({ page, pageCount, onPage, total, unit = 'item', totalPages, onPageChange }) {
  // Machinery mode: 0-based page, totalPages provided, no item counter.
  if (totalPages !== undefined) {
    const isFirstPage = page === 0
    const isLastPage = page >= totalPages - 1
    return (
      <div className="flex items-center justify-end gap-3 mt-4 text-sm">
        <span className="text-gray-500">
          Page {page + 1} of {totalPages}
        </span>
        <button
          type="button"
          disabled={isFirstPage}
          onClick={() => onPageChange(page - 1)}
          className="px-3 py-1 rounded-lg border border-gray-200 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-gray-50"
        >
          Previous
        </button>
        <button
          type="button"
          disabled={isLastPage}
          onClick={() => onPageChange(page + 1)}
          className="px-3 py-1 rounded-lg border border-gray-200 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-gray-50"
        >
          Next
        </button>
      </div>
    )
  }

  // Watersupply mode: 1-based page with item counter.
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

export default Pagination
