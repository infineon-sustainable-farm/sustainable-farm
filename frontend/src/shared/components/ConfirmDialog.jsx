import { useId, useRef } from 'react';
import { AlertTriangle } from 'lucide-react'
import { useAccessibleDialog } from '../hooks/useAccessibleDialog';

/**
 * Confirmation modal before deletion (ws-* design system).
 *
 * The dialog uses the shared `useAccessibleDialog` behaviour: the title names
 * it (`aria-labelledby`), the message is announced as its description, focus
 * starts on Cancel, Escape cancels, and focus returns to the delete button
 * when the dialog closes.
 *
 * The message colour is `#55635f` rather than `--ws-muted` (#6b7a78, 4.49:1 on
 * white): at 14 px the text must reach 4.5:1, and #55635f gives 6.30:1.
 *
 * @param {object} props
 * @param {string} [props.title] - dialog title.
 * @param {string} props.message - warning/consequence text.
 * @param {string} [props.confirmLabel] - confirm button label.
 * @param {() => void} props.onConfirm
 * @param {() => void} props.onCancel
 * @param {boolean} [props.busy] - disables the buttons while the API call runs.
 */
export function ConfirmDialog({ title = 'Confirm deletion', message, confirmLabel = 'Delete', onConfirm, onCancel, busy = false }) {
  const messageId = useId();
  const cancelRef = useRef(null);

  const { titleId, panelRef, handleBackdropClick } = useAccessibleDialog({
    onClose: busy ? undefined : onCancel,
    closeOnBackdrop: !busy,
    closeOnEscape: !busy,
    initialFocusRef: cancelRef,
  });

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={messageId}
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 40,
        display: 'grid',
        placeItems: 'center',
        background: 'rgba(16,47,43,.4)',
        padding: '20px',
        fontFamily: "'Inter', Arial, sans-serif",
      }}
      onClick={handleBackdropClick}
    >
      <div
        ref={panelRef}
        tabIndex={-1}
        style={{
          width: '100%',
          maxWidth: '420px',
          background: 'var(--ws-card, #fff)',
          border: '1px solid var(--ws-line, #e2e9e7)',
          borderRadius: '12px',
          padding: '24px',
          boxShadow: '0 16px 40px rgba(0,0,0,.18)',
          outline: 'none',
        }}
      >
        <div
          aria-hidden="true"
          style={{
            width: '44px',
            height: '44px',
            borderRadius: '50%',
            display: 'grid',
            placeItems: 'center',
            background: 'var(--ws-red-tint, #fbeaea)',
            color: 'var(--ws-red, #c62828)',
            marginBottom: '14px',
          }}
        >
          <AlertTriangle size={22} />
        </div>
        <h2 id={titleId} style={{ margin: '0 0 6px', fontFamily: "'Montserrat', Arial, sans-serif", fontWeight: 600, fontSize: '17px', color: 'var(--ws-ink, #1c2b29)' }}>
          {title}
        </h2>
        <p id={messageId} style={{ margin: '0 0 20px', fontSize: '14px', color: '#55635f', lineHeight: 1.5 }}>
          {message}
        </p>
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
          <button
            ref={cancelRef}
            type="button"
            disabled={busy}
            onClick={onCancel}
            style={buttonStyle(false)}
          >
            Cancel
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={onConfirm}
            style={buttonStyle(true)}
          >
            {busy ? 'Deleting…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}

function buttonStyle(danger) {
  return {
    padding: '10px 16px',
    border: 'none',
    borderRadius: '8px',
    fontSize: '14px',
    fontWeight: 600,
    fontFamily: "'Inter', Arial, sans-serif",
    cursor: 'pointer',
    color: danger ? '#fff' : 'var(--ws-ink, #1c2b29)',
    background: danger ? 'var(--ws-red, #c62828)' : 'var(--ws-line, #e2e9e7)',
  }
}
