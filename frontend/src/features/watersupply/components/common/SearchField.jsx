import { Search } from 'lucide-react'
import { Input } from '../../../../shared/ui'

/** Search box of a list: kit input with a search icon, named by its placeholder. */
export function SearchField({ value, onChange, placeholder }) {
  return (
    <div className="relative min-w-48 flex-1">
      <Search size={16} aria-hidden="true" className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-text-muted" />
      <Input
        type="search"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        aria-label={placeholder}
        className="pl-9"
      />
    </div>
  )
}
