import { Loader2 } from 'lucide-react'

/**
 * Indicateur de chargement inspiré du design system ws-*
 * (couleurs --ws-*, Inter). Aucun texte obligatoire.
 */
export function Spinner({ label = 'Chargement…', size = 22, full = false }) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        justifyContent: 'center',
        ...(full ? { minHeight: '200px', width: '100%' } : {}),
        fontFamily: "'Inter', Arial, sans-serif",
        fontSize: '13px',
        color: 'var(--ws-muted, #6b7a78)',
      }}
      role="status"
      aria-live="polite"
    >
      <Loader2 size={size} style={{ color: 'var(--ws-primary, #0a8276)', animation: 'ws-spin 1s linear infinite' }} />
      <span>{label}</span>
      <style>{`@keyframes ws-spin{to{transform:rotate(360deg)}}`}</style>
    </div>
  )
}