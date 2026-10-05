import { useEffect } from "react";

/**
 * Dialog frame of the Plants module: the overlay, the white panel, and the two
 * ways to close it (Escape, or a click outside the panel).
 *
 * Local to the module, like the Visitor Management modal. The shared Modal has
 * no Escape handling and no dialog semantics, and changing it is a decision for
 * the team that owns it.
 *
 * `labelledBy` is the id of the element that titles the dialog.
 */
export default function PlantsModal({ labelledBy, onClose, children }) {
    useEffect(() => {
        function onKeyDown(event) {
            if (event.key === "Escape") onClose();
        }
        document.addEventListener("keydown", onKeyDown);
        return () => document.removeEventListener("keydown", onKeyDown);
    }, [onClose]);

    return (
        <div
            role="presentation"
            onClick={onClose}
            className="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50 p-4"
        >
            <div
                role="dialog"
                aria-modal="true"
                aria-labelledby={labelledBy}
                onClick={(event) => event.stopPropagation()}
                className="max-h-[85vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white shadow-xl"
            >
                {children}
            </div>
        </div>
    );
}
