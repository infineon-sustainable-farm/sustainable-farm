/**
 * Generic form field matching the design system.
 * Labels in Inter 12px/Medium (>= 12px), input 14px.
 * Compatible with react-hook-form (spread {...register(...)} on `inputProps`).
 *
 * @param {object} props
 * @param {string} props.label - field label.
 * @param {ReactNode} props.children - the child input/select/textarea.
 * @param {string} [props.error] - optional error message.
 * @param {boolean} [props.hint] - optional helper text.
 * @param {boolean} [props.required] - shows an asterisk.
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