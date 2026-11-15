function Pagination({ page, totalPages, onPageChange }) {
  const isFirstPage = page === 0;
  const isLastPage = page >= totalPages - 1;

  return (
    <div className="flex items-center justify-end gap-3 mt-4 text-sm">
      <span className="text-gray-500">Page {page + 1} of {totalPages}</span>
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
  );
}
export default Pagination;
