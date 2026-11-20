import { useMemo, useState } from 'react'
import { ChevronDown, ChevronUp, ChevronsUpDown } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cx } from './cx'
import Skeleton from './Skeleton'
import { focusRing, transition } from './styles'

// Machinery tables: "w-full text-left border-collapse"; header row "border-b border-gray-200 text-sm
// text-gray-500"; body rows "border-b border-gray-100"; cells "py-2". The horizontal overflow
// scrolls inside the table wrapper ("overflow-x-auto", as SparePartTable) instead of the page.
const ALIGN = { left: 'text-left', right: 'text-right', center: 'text-center' }
const ARIA_SORT = { asc: 'ascending', desc: 'descending' }

function compareValues(a, b) {
  if (a == null && b == null) return 0
  if (a == null) return -1
  if (b == null) return 1
  if (typeof a === 'number' && typeof b === 'number') return a - b
  return String(a).localeCompare(String(b), undefined, { numeric: true, sensitivity: 'base' })
}

function cellValue(column, row) {
  return column.sortValue ? column.sortValue(row) : row[column.key]
}

/**
 * Data table of the kit.
 *
 * @param {object} props
 * @param {Array<{
 *   key: string,
 *   header: import('react').ReactNode,
 *   cell?: (row: object) => import('react').ReactNode,
 *   sortable?: boolean,
 *   sortValue?: (row: object) => unknown,
 *   align?: 'left'|'right'|'center',
 *   className?: string,
 * }>} props.columns - `cell` renders the cell (row[key] otherwise); the first column is bold.
 * @param {object[]} props.rows
 * @param {(row: object) => string|number} [props.getRowKey] - row.id by default.
 * @param {import('react').ReactNode} [props.caption] - accessible name of the table (visually hidden).
 * @param {(row: object) => import('react').ReactNode} [props.rowActions] - last column (IconButtons).
 * @param {string} [props.actionsHeader] - common:table.actions by default.
 * @param {boolean} [props.loading] - skeleton rows and aria-busy.
 * @param {number} [props.skeletonRows=5]
 * @param {import('react').ReactNode} [props.emptyMessage] - common:status.noResults by default.
 * @param {{ key: string, direction: 'asc'|'desc' }|null} [props.sort] - controlled sort (server side).
 * @param {(sort: { key: string, direction: 'asc'|'desc' }) => void} [props.onSortChange] - with
 *   `sort`, the rows are shown as given; without, the table sorts them itself.
 * @param {{ key: string, direction: 'asc'|'desc' }} [props.defaultSort] - initial local sort.
 */
function Table({
  columns,
  rows = [],
  getRowKey = (row) => row.id,
  caption,
  rowActions,
  actionsHeader,
  loading = false,
  skeletonRows = 5,
  emptyMessage,
  sort: controlledSort,
  onSortChange,
  defaultSort = null,
  className,
}) {
  const { t } = useTranslation('common')
  const [localSort, setLocalSort] = useState(defaultSort)
  const isControlled = controlledSort !== undefined
  const sort = isControlled ? controlledSort : localSort

  const sortedRows = useMemo(() => {
    if (isControlled || !sort) return rows
    const column = columns.find((col) => col.key === sort.key)
    if (!column) return rows
    const sorted = [...rows].sort((a, b) => compareValues(cellValue(column, a), cellValue(column, b)))
    return sort.direction === 'desc' ? sorted.reverse() : sorted
  }, [rows, columns, sort, isControlled])

  function toggleSort(key) {
    const next = sort?.key === key && sort.direction === 'asc' ? { key, direction: 'desc' } : { key, direction: 'asc' }
    if (!isControlled) setLocalSort(next)
    onSortChange?.(next)
  }

  const columnCount = columns.length + (rowActions ? 1 : 0)

  return (
    <div className={cx('w-full max-w-full overflow-x-auto', className)}>
      <table className="w-full border-collapse text-left" aria-busy={loading || undefined}>
        {caption && <caption className="sr-only">{caption}</caption>}
        <thead>
          <tr className="border-b border-border-divider text-sm text-text-muted">
            {columns.map((column) => {
              const direction = sort?.key === column.key ? sort.direction : null
              const SortIcon = direction === 'asc' ? ChevronUp : direction === 'desc' ? ChevronDown : ChevronsUpDown
              return (
                <th
                  key={column.key}
                  scope="col"
                  aria-sort={column.sortable ? (ARIA_SORT[direction] ?? 'none') : undefined}
                  className={cx('py-2 pr-4 whitespace-nowrap', ALIGN[column.align] ?? ALIGN.left)}
                >
                  {column.sortable ? (
                    <button
                      type="button"
                      onClick={() => toggleSort(column.key)}
                      className={cx(
                        '-mx-1 -my-1 inline-flex min-h-6 cursor-pointer items-center gap-1 rounded-control px-1 hover:text-text pointer-coarse:min-h-11',
                        focusRing,
                        transition,
                      )}
                    >
                      {column.header}
                      <SortIcon size={14} aria-hidden="true" />
                    </button>
                  ) : (
                    column.header
                  )}
                </th>
              )
            })}
            {rowActions && (
              <th scope="col" className="py-2 whitespace-nowrap">
                {actionsHeader ?? t('table.actions')}
              </th>
            )}
          </tr>
        </thead>
        <tbody>
          {loading &&
            Array.from({ length: skeletonRows }, (_, index) => (
              <tr key={`skeleton-${index}`} className="border-b border-border-subtle" aria-hidden="true">
                {Array.from({ length: columnCount }, (__, cellIndex) => (
                  <td key={cellIndex} className="py-2 pr-4">
                    <Skeleton className="h-5 w-full max-w-40" />
                  </td>
                ))}
              </tr>
            ))}

          {!loading && sortedRows.length === 0 && (
            <tr>
              <td colSpan={columnCount} className="py-8 text-center text-text-muted">
                {emptyMessage ?? t('status.noResults')}
              </td>
            </tr>
          )}

          {!loading &&
            sortedRows.map((row) => (
              <tr key={getRowKey(row)} className="border-b border-border-subtle">
                {columns.map((column, index) => (
                  <td
                    key={column.key}
                    className={cx('py-2 pr-4', index === 0 && 'font-medium', ALIGN[column.align] ?? ALIGN.left, column.className)}
                  >
                    {column.cell ? column.cell(row) : row[column.key]}
                  </td>
                ))}
                {rowActions && (
                  <td className="py-2 whitespace-nowrap">
                    <div className="-my-1 flex items-center gap-2">{rowActions(row)}</div>
                  </td>
                )}
              </tr>
            ))}
        </tbody>
      </table>
      {loading && (
        <p role="status" className="sr-only">
          {t('status.loading')}
        </p>
      )}
    </div>
  )
}

export default Table
