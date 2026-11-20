import { useId } from 'react'
import { CircleAlert } from 'lucide-react'
import { FieldContext } from './fieldContext'

/**
 * Label, control, hint and error of a form field, as in the machinery forms: label
 * "text-sm font-medium", control 4 px under it, error "text-xs" in red. The error also carries an
 * icon (never colour only) and is linked to the control with aria-describedby and aria-invalid.
 *
 * Wrap one Input, Select or Textarea of the kit: it receives the ids on its own.
 *
 * @param {object} props
 * @param {import('react').ReactNode} props.label
 * @param {import('react').ReactNode} [props.hint] - help text, shown when there is no error.
 * @param {import('react').ReactNode} [props.error] - error message; marks the control invalid.
 * @param {string} [props.id] - id of the control (generated otherwise).
 * @param {boolean} [props.required]
 * @param {boolean} [props.disabled]
 */
function Field({ label, hint, error, id, required, disabled, className, children }) {
  const generatedId = useId()
  const controlId = id ?? `field-${generatedId}`
  const hintId = `${controlId}-hint`
  const errorId = `${controlId}-error`
  const describedBy = error ? errorId : hint ? hintId : undefined

  return (
    <div className={className}>
      <label htmlFor={controlId} className="text-sm font-medium">
        {label}
      </label>
      <FieldContext.Provider value={{ id: controlId, describedBy, invalid: Boolean(error), required, disabled }}>
        <div className="mt-1">{children}</div>
      </FieldContext.Provider>
      {error ? (
        <p id={errorId} className="mt-1 flex items-center gap-1 text-xs text-error-fg">
          <CircleAlert size={14} aria-hidden="true" className="shrink-0" />
          <span>{error}</span>
        </p>
      ) : (
        hint && (
          <p id={hintId} className="mt-1 text-xs text-text-muted">
            {hint}
          </p>
        )
      )}
    </div>
  )
}

export default Field
