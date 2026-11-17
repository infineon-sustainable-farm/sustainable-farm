import { createContext, useContext } from 'react'

export const ToastContext = createContext(null)

/** Time a toast stays on screen, in milliseconds (behaviour, not a visual value). */
export const TOAST_DURATION = 5000

/**
 * Shows short notifications. Needs a ToastProvider above.
 *
 * @returns {{
 *   show: (toast: { message: import('react').ReactNode, tone?: 'success'|'error'|'warning'|'info',
 *     duration?: number }) => string,
 *   success: (message: import('react').ReactNode) => string,
 *   error: (message: import('react').ReactNode) => string,
 *   dismiss: (id: string) => void,
 * }}
 */
export function useToast() {
  const context = useContext(ToastContext)
  if (!context) throw new Error('useToast must be used inside a ToastProvider')
  return context
}
