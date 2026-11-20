import { CircleAlert } from 'lucide-react'

/** Error of a form or of a request, as the machinery server errors: red tint with an icon. */
export function FormAlert({ children }) {
  if (!children) return null
  return (
    <p role="alert" className="mb-4 flex items-start gap-2 rounded-control bg-error-tint px-3 py-2 text-sm text-error-fg">
      <CircleAlert size={16} aria-hidden="true" className="mt-0.5 shrink-0" />
      <span>{children}</span>
    </p>
  )
}
