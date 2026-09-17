import { Inbox } from 'lucide-react'

/**
 * État vide (aucune donnée) conforme au design system.
 * @param {string} title - Message principal.
 * @param {string} [description] - sous-texte optionnel.
 * @param {ReactNode} [action] - bouton d'action optionnel (ex. "Créer une ferme").
 */
export function EmptyState({ title = 'Aucune donnée', description, action }) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '10px',
        minHeight: '180px',
        padding: '24px',
        border: '1px dashed var(--ws-line, #e2e9e7)',
        borderRadius: '12px',
        background: 'var(--ws-card, #fff)',
        textAlign: 'center',
      }}
    >
      <div
        style={{
          width: '52px',
          height: '52px',
          borderRadius: '50%',
          display: 'grid',
          placeItems: 'center',
          background: 'var(--ws-primary-tint, #e3f2ef)',
          color: 'var(--ws-primary, #0a8276)',
        }}
      >
        <Inbox size={24} />
      </div>
      <div style={{ fontFamily: "'Montserrat', Arial, sans-serif", fontWeight: 600, fontSize: '16px', color: 'var(--ws-ink, #1c2b29)' }}>
        {title}
      </div>
      {description && (
        <div style={{ fontFamily: "'Inter', Arial, sans-serif", fontSize: '13px', color: 'var(--ws-muted, #6b7a78)', lineHeight: 1.5 }}>
          {description}
        </div>
      )}
      {action}
    </div>
  )
}