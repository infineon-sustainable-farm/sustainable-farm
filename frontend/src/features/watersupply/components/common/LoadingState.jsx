import { LoaderCircle } from 'lucide-react'

/**
 * Loading message of a panel: spinner and text, announced politely. `compact` for a block
 * inside a card, the full height (64 px above and below, as EmptyState) otherwise.
 */
export function LoadingState({ label, compact = false }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={`flex items-center justify-center gap-2 text-sm text-text-muted ${compact ? 'py-4' : 'py-16'}`}
    >
      <LoaderCircle size={18} aria-hidden="true" className="animate-spin motion-reduce:animate-none" />
      <span>{label}</span>
    </div>
  )
}
