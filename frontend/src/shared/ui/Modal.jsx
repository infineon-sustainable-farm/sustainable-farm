import { useEffect, useId, useRef } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cx } from './cx'
import { getFocusable } from './focusable'
import IconButton from './IconButton'

// Machinery modal (former shared/components/Modal): black 50 % backdrop, white panel "rounded-lg p-6
// w-full max-w-md", title "text-lg font-bold mb-4". The kit adds the dialog semantics, Escape, the
// focus trap, focus return, a close button, and a 300 ms entrance (off under reduced motion).
const SIZES = { sm: 'max-w-sm', md: 'max-w-md', lg: 'max-w-lg', xl: 'max-w-2xl' }

const entrance =
  'transition duration-(--duration-slow) ease-enter starting:opacity-0 motion-reduce:transition-none'

/**
 * Accessible dialog.
 *
 * @param {object} props
 * @param {boolean} props.open
 * @param {() => void} props.onClose - called on Escape, backdrop click and the close button.
 * @param {import('react').ReactNode} props.title - dialog title (names the dialog).
 * @param {import('react').ReactNode} [props.description] - text under the title (describes the dialog).
 * @param {import('react').ReactNode} [props.footer] - action buttons, aligned right.
 * @param {'sm'|'md'|'lg'|'xl'} [props.size='md']
 * @param {import('react').RefObject<HTMLElement>} [props.initialFocusRef] - element focused on open
 *   (first focusable field otherwise).
 * @param {boolean} [props.dismissible=true] - false blocks Escape and the backdrop (a request runs).
 * @param {string} [props.closeLabel] - accessible name of the close button (common:actions.close).
 */
function Modal({
  open,
  onClose,
  title,
  description,
  footer,
  size = 'md',
  initialFocusRef,
  dismissible = true,
  closeLabel,
  className,
  children,
}) {
  const { t } = useTranslation('common')
  const panelRef = useRef(null)
  const onCloseRef = useRef(onClose)
  const dismissibleRef = useRef(dismissible)
  const baseId = useId()
  const titleId = `${baseId}-title`
  const descriptionId = `${baseId}-description`

  useEffect(() => {
    onCloseRef.current = onClose
    dismissibleRef.current = dismissible
  })

  useEffect(() => {
    if (!open) return undefined
    const previouslyFocused = document.activeElement
    const panel = panelRef.current
    const target = initialFocusRef?.current ?? getFocusable(panel).find((el) => !el.dataset.modalClose) ?? panel
    target?.focus()

    const { overflow } = document.body.style
    document.body.style.overflow = 'hidden'

    function onKeyDown(event) {
      if (event.key === 'Escape') {
        if (!dismissibleRef.current) return
        event.stopPropagation()
        onCloseRef.current?.()
        return
      }
      if (event.key !== 'Tab') return
      const focusable = getFocusable(panel)
      if (focusable.length === 0) {
        event.preventDefault()
        panel.focus()
        return
      }
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      const active = document.activeElement
      if (event.shiftKey && (active === first || !panel.contains(active))) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && (active === last || !panel.contains(active))) {
        event.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.body.style.overflow = overflow
      if (previouslyFocused && typeof previouslyFocused.focus === 'function') previouslyFocused.focus()
    }
  }, [open, initialFocusRef])

  if (!open) return null

  function onBackdropMouseDown(event) {
    if (event.target === event.currentTarget && dismissible) onClose?.()
  }

  return createPortal(
    <div
      role="presentation"
      onMouseDown={onBackdropMouseDown}
      className={cx('fixed inset-0 z-(--z-overlay) flex items-center justify-center bg-backdrop p-4', entrance)}
    >
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        tabIndex={-1}
        className={cx(
          'relative max-h-full w-full overflow-y-auto rounded-control bg-surface-raised p-6 text-text',
          'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring',
          'starting:scale-97',
          entrance,
          SIZES[size] ?? SIZES.md,
          className,
        )}
      >
        <div className={cx('flex items-start justify-between gap-4', (children || footer) && 'mb-4')}>
          <div>
            <h2 id={titleId} className="text-lg font-bold">
              {title}
            </h2>
            {description && (
              <p id={descriptionId} className="mt-2 text-sm text-text-muted">
                {description}
              </p>
            )}
          </div>
          <IconButton
            icon={X}
            label={closeLabel ?? t('actions.close')}
            tone="neutral"
            onClick={onClose}
            disabled={!dismissible}
            data-modal-close="true"
            className="-mr-2 -mt-2"
          />
        </div>
        {children}
        {footer && <div className={cx('flex flex-wrap justify-end gap-2', children && 'mt-4')}>{footer}</div>}
      </div>
    </div>,
    document.body,
  )
}

export default Modal
