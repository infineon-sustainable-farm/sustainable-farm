import { Pagination } from '../../../../shared/ui'

/**
 * Kit pagination driven by useListControls (pages from 1, the kit counts from 0). Nothing is
 * rendered while the list fits on one page.
 */
export function ListPagination({ list }) {
  if (!list.pageCount || list.pageCount <= 1) return null
  return (
    <div className="mt-4">
      <Pagination page={list.page - 1} totalPages={list.pageCount} onPageChange={(page) => list.setPage(page + 1)} />
    </div>
  )
}
