import { Search } from 'lucide-react'

/**
 * Reusable text search field (ws-* design system).
 * @param {string} value - valeur courante.
 * @param {(next: string) => void} onChange
 * @param {string} [placeholder]
 */
export function SearchInput({ value, onChange, placeholder = 'Search…' }) {
  return (
    <div className="ws-search">
      <Search size={14} />
      <input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} />
    </div>
  )
}