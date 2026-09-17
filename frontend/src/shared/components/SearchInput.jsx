import { Search } from 'lucide-react'

/**
 * Champ de recherche texte réutilisable (design system ws-*).
 * @param {string} value - valeur courante.
 * @param {(next: string) => void} onChange
 * @param {string} [placeholder]
 */
export function SearchInput({ value, onChange, placeholder = 'Rechercher…' }) {
  return (
    <div className="ws-search">
      <Search size={14} />
      <input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} />
    </div>
  )
}