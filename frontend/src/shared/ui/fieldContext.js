import { createContext, useContext } from 'react'

/** Ids and state that a Field gives to the control it wraps (Input, Select, Textarea). */
export const FieldContext = createContext(null)

/**
 * Props of the control inside a Field: id, aria-describedby, aria-invalid, required. Props passed
 * to the control directly win over the Field ones.
 */
export function useFieldControl(props) {
  const field = useContext(FieldContext)
  if (!field) return props
  const describedBy = [field.describedBy, props['aria-describedby']].filter(Boolean).join(' ') || undefined
  return {
    ...props,
    id: props.id ?? field.id,
    required: props.required ?? field.required,
    disabled: props.disabled ?? field.disabled,
    'aria-describedby': describedBy,
    'aria-invalid': props['aria-invalid'] ?? (field.invalid || undefined),
  }
}
