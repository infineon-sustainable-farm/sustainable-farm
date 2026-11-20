import { useRef } from 'react'
import { CircleAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import Button from './Button'
import Modal from './Modal'

/**
 * Confirmation before a destructive action, as the machinery delete dialogs: title, message,
 * Cancel (secondary) and a danger confirm button. Focus starts on Cancel. While `loading`, the
 * confirm button shows its spinner and the dialog cannot be dismissed.
 *
 * @param {object} props
 * @param {boolean} props.open
 * @param {() => void} props.onClose
 * @param {() => void} props.onConfirm
 * @param {import('react').ReactNode} [props.title] - common:confirm.deleteTitle by default.
 * @param {import('react').ReactNode} props.message - what is deleted and that it cannot be undone.
 * @param {string} [props.confirmLabel] - common:actions.delete by default.
 * @param {string} [props.cancelLabel] - common:actions.cancel by default.
 * @param {'danger'|'primary'} [props.variant='danger']
 * @param {boolean} [props.loading]
 * @param {import('react').ReactNode} [props.error] - message of a failed request.
 */
function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel,
  cancelLabel,
  variant = 'danger',
  loading = false,
  error,
}) {
  const { t } = useTranslation('common')
  const cancelRef = useRef(null)

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title ?? t('confirm.deleteTitle')}
      description={message}
      initialFocusRef={cancelRef}
      dismissible={!loading}
      footer={
        <>
          <Button ref={cancelRef} variant="secondary" onClick={onClose} disabled={loading}>
            {cancelLabel ?? t('actions.cancel')}
          </Button>
          <Button variant={variant} onClick={onConfirm} loading={loading}>
            {confirmLabel ?? t('actions.delete')}
          </Button>
        </>
      }
    >
      {error && (
        <p role="alert" className="flex items-center gap-2 rounded-control bg-error-tint px-3 py-2 text-sm text-error-fg">
          <CircleAlert size={16} aria-hidden="true" className="shrink-0" />
          <span>{error}</span>
        </p>
      )}
    </Modal>
  )
}

export default ConfirmDialog
