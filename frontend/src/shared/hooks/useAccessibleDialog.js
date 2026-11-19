import { useCallback, useEffect, useId, useRef } from 'react';

/**
 * Shared modal behaviour for the Product Transformation dialogs and
 * `ConfirmDialog`.
 *
 * The markup of each dialog already existed and reads differently (a Tailwind
 * card, a ws-* panel), so the reusable part is the behaviour rather than the
 * markup. The hook provides the whole modal contract:
 *
 *  - `titleId` names the dialog (`aria-labelledby`); the caller puts it on its
 *    `<h2>` so the accessible name is the visible title.
 *  - focus moves inside the panel on open (first control, or `initialFocusRef`).
 *  - Tab and Shift+Tab cycle inside the panel instead of reaching the page.
 *  - Escape cancels (and can be disabled while a request is in flight).
 *  - focus returns to the element that opened the dialog on close.
 *  - `handleBackdropClick` closes on a click on the backdrop only.
 *
 * `panelRef` must be attached to the panel element (the white card), with
 * `tabIndex={-1}` so it can take focus when the panel holds no control.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

/** Every control inside the panel that the keyboard can reach, in DOM order. */
function getFocusable(panel) {
  if (!panel) return [];
  return Array.from(panel.querySelectorAll(FOCUSABLE_SELECTOR)).filter(
    (node) => node.getAttribute('aria-hidden') !== 'true' && node.tabIndex !== -1,
  );
}

export function useAccessibleDialog({
  open = true,
  onClose,
  closeOnBackdrop = true,
  closeOnEscape = true,
  initialFocusRef,
} = {}) {
  const titleId = useId();
  const panelRef = useRef(null);
  const restoreFocusRef = useRef(null);

  // Callers pass inline arrows; keeping them in refs stops the listeners from
  // being torn down and rebuilt on every render.
  const onCloseRef = useRef(onClose);
  const optionsRef = useRef({ closeOnBackdrop, closeOnEscape });

  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    optionsRef.current = { closeOnBackdrop, closeOnEscape };
  }, [closeOnBackdrop, closeOnEscape]);

  // Focus in on open, focus back out on close.
  useEffect(() => {
    if (!open) return undefined;
    const panel = panelRef.current;
    if (!panel) return undefined;

    restoreFocusRef.current =
      document.activeElement instanceof HTMLElement ? document.activeElement : null;

    const target = initialFocusRef?.current ?? getFocusable(panel)[0] ?? panel;
    target.focus?.({ preventScroll: true });

    return () => {
      const restore = restoreFocusRef.current;
      restoreFocusRef.current = null;
      if (restore && restore !== document.body && document.contains(restore)) {
        restore.focus?.({ preventScroll: true });
      }
    };
  }, [open, initialFocusRef]);

  // Escape cancels; Tab cycles inside the panel. Listening on the document
  // keeps Escape working even when focus has drifted to the backdrop.
  useEffect(() => {
    if (!open) return undefined;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        if (!optionsRef.current.closeOnEscape) return;
        event.preventDefault();
        event.stopPropagation();
        onCloseRef.current?.();
        return;
      }

      if (event.key !== 'Tab') return;

      const panel = panelRef.current;
      if (!panel) return;

      const nodes = getFocusable(panel);
      if (nodes.length === 0) {
        event.preventDefault();
        panel.focus?.({ preventScroll: true });
        return;
      }

      const first = nodes[0];
      const last = nodes[nodes.length - 1];
      const active = document.activeElement;

      if (!panel.contains(active)) {
        event.preventDefault();
        (event.shiftKey ? last : first).focus();
        return;
      }
      if (event.shiftKey && active === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && active === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open]);

  const handleBackdropClick = useCallback((event) => {
    if (!optionsRef.current.closeOnBackdrop) return;
    if (event.target !== event.currentTarget) return;
    onCloseRef.current?.();
  }, []);

  return { titleId, panelRef, handleBackdropClick };
}

export default useAccessibleDialog;
