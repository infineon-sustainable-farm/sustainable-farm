import { useEffect, useRef } from "react";
import { FOCUSABLE_SELECTOR, trappedFocusIndex } from "../utils/focus";

/*
 * Keyboard behaviour shared by the dialogs: focus moves inside on opening and
 * goes to [data-autofocus] if there is one, Tab stays inside, Escape closes,
 * and focus returns to the element that opened the dialog.
 */
export function useDialogFocus(containerRef, isOpen, onClose) {
    const onCloseRef = useRef(onClose);

    useEffect(() => {
        onCloseRef.current = onClose;
    });

    useEffect(() => {
        if (!isOpen) {
            return undefined;
        }
        const opener = document.activeElement;
        const container = containerRef.current;
        (container?.querySelector("[data-autofocus]") ?? container)?.focus({ preventScroll: true });

        const onKeyDown = (event) => {
            if (event.key === "Escape") {
                event.preventDefault();
                onCloseRef.current();
                return;
            }
            if (event.key !== "Tab" || !container) {
                return;
            }
            const focusable = [...container.querySelectorAll(FOCUSABLE_SELECTOR)];
            const next = trappedFocusIndex(focusable.indexOf(document.activeElement), focusable.length, event.shiftKey);
            if (next !== null) {
                event.preventDefault();
                focusable[next].focus();
            }
        };

        document.addEventListener("keydown", onKeyDown);
        return () => {
            document.removeEventListener("keydown", onKeyDown);
            if (opener instanceof HTMLElement && opener.isConnected) {
                opener.focus({ preventScroll: true });
            }
        };
    }, [isOpen, containerRef]);
}
