import { useState } from 'react'
import { ChevronUp, ChevronDown } from 'lucide-react'

/**
 * Reusable data table matching the design system (13px table, Inter).
 * Supports sorting by key and an actions column.
 *
 * @param {Array} columns - [{ key, label, sortable?, render? }]
 * @param {Array} rows - the raw data.
 * @param {string | null} keyField - unique key name (defaults to 'id').
 * @param {ReactNode} [actions] - render prop (row) => ReactNode, shown as last column.
 */
export function DataTable({ columns, rows = [], keyField = 'id', actions }) {
  const [sort, setSort] = useState({ key: null, dir: 'asc' })

  const sortable = sort.key
  const data = sortable
    ? [...rows].sort((a, b) => {
        const av = a[sortable]
        const bv = b[sortable]
        const cmp = av == null ? -1 : bv == null ? 1 : String(av).localeCompare(String(bv), undefined, { numeric: true })
        return sort.dir === 'asc' ? cmp : -cmp
      })
    : rows

  const toggleSort = (key) => {
    setSort((prev) => (prev.key === key ? { key, dir: prev.dir === 'asc' ? 'desc' : 'asc' } : { key, dir: 'asc' }))
  }

  return (
    <div style={{ overflow: 'auto' }}>
      <table className="ws-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            {columns.map((col) => (
              <th
                key={col.key}
                style={colStyle}
                onClick={col.sortable ? () => toggleSort(col.key) : undefined}
              >
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', cursor: col.sortable ? 'pointer' : 'default' }}>
                  {col.label}
                  {col.sortable && sort.key === col.key && (sort.dir === 'asc' ? <ChevronUp size={13} /> : <ChevronDown size={13} />)}
                </span>
              </th>
            ))}
            {actions && <th style={colStyle}></th>}
          </tr>
        </thead>
        <tbody>
          {data.map((row) => (
            <tr key={row[keyField] ?? JSON.stringify(row)}>
              {columns.map((col) => (
                <td key={col.key} style={cellStyle}>
                  {col.render ? col.render(row) : (row[col.key] ?? '—')}
                </td>
              ))}
              {actions && <td style={{ ...cellStyle, textAlign: 'right' }}>{actions(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

const colStyle = {
  textAlign: 'left',
  padding: '9px 12px',
  fontSize: '12px',
  fontWeight: 600,
  letterSpacing: '.3px',
  textTransform: 'uppercase',
  color: 'var(--ws-muted, #6b7a78)',
  borderBottom: '1px solid var(--ws-line, #e2e9e7)',
  whiteSpace: 'nowrap',
}

const cellStyle = {
  padding: '9px 12px',
  fontSize: '13px',
  color: 'var(--ws-ink, #1c2b29)',
  borderBottom: '1px solid var(--ws-line, #e2e9e7)',
}