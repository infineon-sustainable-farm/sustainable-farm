import { AlertTriangle } from 'lucide-react'

/**
 * Modale de confirmation avant suppression (design system ws-*).
 * @param {object} props
 * @param {string} [props.title] - titre de la boîte de dialogue.
 * @param {string} props.message - texte d'avertissement/conséquence.
 * @param {string} [props.confirmLabel] - libellé du bouton de confirmation.
 * @param {() => void} props.onConfirm
 * @param {() => void} props.onCancel
 * @param {boolean} [props.busy] - désactive les boutons pendant l'appel API.
 */
export function ConfirmDialog({ title = 'Confirmer la suppression', message, confirmLabel = 'Supprimer', onConfirm, onCancel, busy = false }) {
  return (
    <div
      role="dialog"
      aria-modal="true"
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 40,
        display: 'grid',
        placeItems: 'center',
        background: 'rgba(16,47,43,.4)',
        padding: '20px',
        fontFamily: "'Inter', Arial, sans-serif",
      }}
      onClick={busy ? undefined : onCancel}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '420px',
          background: 'var(--ws-card, #fff)',
          border: '1px solid var(--ws-line, #e2e9e7)',
          borderRadius: '12px',
          padding: '24px',
          boxShadow: '0 16px 40px rgba(0,0,0,.18)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div
          style={{
            width: '44px',
            height: '44px',
            borderRadius: '50%',
            display: 'grid',
            placeItems: 'center',
            background: 'var(--ws-red-tint, #fbeaea)',
            color: 'var(--ws-red, #c62828)',
            marginBottom: '14px',
          }}
        >
          <AlertTriangle size={22} />
        </div>
        <h3 style={{ margin: '0 0 6px', fontFamily: "'Montserrat', Arial, sans-serif", fontWeight: 600, fontSize: '17px', color: 'var(--ws-ink, #1c2b29)' }}>
          {title}
        </h3>
        <p style={{ margin: '0 0 20px', fontSize: '14px', color: 'var(--ws-muted, #6b7a78)', lineHeight: 1.5 }}>
          {message}
        </p>
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
          <button
            type="button"
            disabled={busy}
            onClick={onCancel}
            style={buttonStyle(false)}
          >
            Annuler
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={onConfirm}
            style={buttonStyle(true)}
          >
            {busy ? 'Suppression…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}

function buttonStyle(danger) {
  return {
    padding: '10px 16px',
    border: 'none',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: 600,
    fontFamily: "'Inter', Arial, sans-serif",
    cursor: 'pointer',
    color: danger ? '#fff' : 'var(--ws-ink, #1c2b29)',
    background: danger ? 'var(--ws-red, #c62828)' : 'var(--ws-line, #e2e9e7)',
  }
}