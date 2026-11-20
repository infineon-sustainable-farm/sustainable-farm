import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { CircleAlert, CircleCheck, Info, TriangleAlert, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cx } from './cx'
import IconButton from './IconButton'
import { TOAST_DURATION, ToastContext } from './toastContext'

// Machinery has no toast. Built as a machinery card (white, border gray-100, rounded-xl, shadow-md
// as a popover) with the icon of its tone, so the state is not carried by colour only.
const TONES = {
  success: { icon: CircleCheck, className: 'text-success-fg' },
  error: { icon: CircleAlert, className: 'text-error-fg' },
  warning: { icon: TriangleAlert, className: 'text-warning-fg' },
  info: { icon: Info, className: 'text-info-fg' },
}

let nextId = 0

function ToastItem({ toast, onDismiss }) {
  const { t } = useTranslation('common')
  const { icon: Icon, className } = TONES[toast.tone] ?? TONES.info
  const timer = useRef(null)

  const start = useCallback(() => {
    if (!toast.duration) return
    timer.current = setTimeout(() => onDismiss(toast.id), toast.duration)
  }, [toast.id, toast.duration, onDismiss])

  const stop = useCallback(() => clearTimeout(timer.current), [])

  useEffect(() => {
    start()
    return stop
  }, [start, stop])

  return (
    <li
      onMouseEnter={stop}
      onMouseLeave={start}
      onFocus={stop}
      onBlur={start}
      className={cx(
        'flex items-start gap-3 rounded-tile border border-border-subtle bg-surface-raised p-4 text-sm text-text shadow-overlay',
        'transition duration-(--duration-base) ease-enter starting:translate-y-2 starting:opacity-0 motion-reduce:transition-none',
      )}
    >
      <Icon size={20} aria-hidden="true" className={cx('shrink-0', className)} />
      <p className="min-w-0 flex-1 self-center">{toast.message}</p>
      <IconButton
        icon={X}
        label={t('actions.dismiss')}
        tone="neutral"
        onClick={() => onDismiss(toast.id)}
        className="-my-2 -mr-2"
      />
    </li>
  )
}

/**
 * Holds the toasts of the application and renders them bottom right, in a polite live region.
 * Each toast closes after `duration` ms (5 s by default, 0 keeps it), pauses while hovered or
 * focused, and has a close button.
 */
export function ToastProvider({ children }) {
  const { t } = useTranslation('common')
  const [toasts, setToasts] = useState([])

  const dismiss = useCallback((id) => setToasts((current) => current.filter((toast) => toast.id !== id)), [])

  const show = useCallback(({ message, tone = 'info', duration = TOAST_DURATION }) => {
    nextId += 1
    const id = `toast-${nextId}`
    setToasts((current) => [...current, { id, message, tone, duration }])
    return id
  }, [])

  const api = useMemo(
    () => ({
      show,
      dismiss,
      success: (message) => show({ message, tone: 'success' }),
      error: (message) => show({ message, tone: 'error' }),
    }),
    [show, dismiss],
  )

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div
        role="region"
        aria-label={t('toast.region')}
        className="pointer-events-none fixed right-4 bottom-4 left-4 z-(--z-toast) sm:left-auto sm:w-96"
      >
        <ol aria-live="polite" aria-relevant="additions text" className="pointer-events-auto flex flex-col gap-2">
          {toasts.map((toast) => (
            <ToastItem key={toast.id} toast={toast} onDismiss={dismiss} />
          ))}
        </ol>
      </div>
    </ToastContext.Provider>
  )
}

export default ToastProvider
