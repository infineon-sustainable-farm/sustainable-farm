/**
 * Champ de formulaire générique conforme au design system.
 * Labels en Inter 12px/Medium (>= 12px), saisie 14px.
 * Compatible react-hook-form (spread {...register(...)} sur `inputProps`).
 *
 * @param {object} props
 * @param {string} props.label - libellé du champ.
 * @param {ReactNode} props.children - l'input/select/textarea enfant.
 * @param {string} [props.error] - message d'erreur éventuel.
 * @param {boolean} [props.hint] - texte d'aide optionnel.
 * @param {boolean} [props.required] - affiche un astérisque.
 */
export function FormField({ label, children, error, hint, required }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
      <label
        style={{
          display: 'flex',
          gap: '4px',
          fontFamily: "'Inter', Arial, sans-serif",
          fontSize: '12px',
          fontWeight: 600,
          letterSpacing: '.3px',
          color: 'var(--ws-ink, #1c2b29)',
        }}
      >
        {label}
        {required && <span style={{ color: 'var(--ws-red, #c62828)' }}>*</span>}
      </label>
      {children}
      {hint && !error && (
        <span style={{ fontFamily: "'Inter', Arial, sans-serif", fontSize: '12px', color: 'var(--ws-muted, #6b7a78)' }}>
          {hint}
        </span>
      )}
      {error && (
        <span style={{ fontFamily: "'Inter', Arial, sans-serif", fontSize: '12px', color: 'var(--ws-red, #c62828)' }}>
          {error}
        </span>
      )}
    </div>
  )
}