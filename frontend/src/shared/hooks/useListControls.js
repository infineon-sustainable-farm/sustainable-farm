import { useMemo, useState } from 'react'

const compareValues = (a, b) => {
  if (a == null && b == null) return 0
  if (a == null) return -1
  if (b == null) return 1
  if (typeof a === 'number' && typeof b === 'number') return a - b
  return String(a).localeCompare(String(b), undefined, { numeric: true })
}

/**
 * Recherche + tri + pagination côté client pour les listes du frontend.
 * @param {Array} items - données brutes (déjà filtrées métier par l'appelant).
 * @param {Array} [searchFields] - champs texte parcourus par la recherche.
 * @param {{ key: string, dir: 'asc'|'desc' }} [defaultSort] - tri initial.
 * @param {number} [pageSize] - taille de page.
 * @returns {{ items: Array, query: string, setQuery: Function, sort: Object,
 *             toggleSort: Function, page: number, setPage: Function,
 *             pageCount: number, total: number }}
 */
export function useListControls(items, { searchFields = [], defaultSort = null, pageSize = 8 } = {}) {
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState(defaultSort)
  const [rawPage, setRawPage] = useState(1)

  const searched = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return items
    return items.filter((item) => searchFields.some((field) => String(item[field] ?? '').toLowerCase().includes(q)))
  }, [items, query, searchFields])

  const sorted = useMemo(() => {
    if (!sort?.key) return searched
    return [...searched].sort((a, b) => {
      const cmp = compareValues(a[sort.key], b[sort.key])
      return sort.dir === 'asc' ? cmp : -cmp
    })
  }, [searched, sort])

  const pageCount = Math.max(1, Math.ceil(sorted.length / pageSize))
  const page = Math.min(rawPage, pageCount)
  const total = sorted.length

  const items2 = useMemo(
    () => (total <= pageSize ? sorted : sorted.slice((page - 1) * pageSize, page * pageSize)),
    [sorted, total, page, pageSize],
  )

  const toggleSort = (key) =>
    setSort((current) => (current?.key === key ? { key, dir: current.dir === 'asc' ? 'desc' : 'asc' } : { key, dir: 'asc' }))
  const setPage = (next) => setRawPage(Math.max(1, Math.min(next, pageCount)))

  return { items: items2, query, setQuery, sort, toggleSort, page, setPage, pageCount, total }
}