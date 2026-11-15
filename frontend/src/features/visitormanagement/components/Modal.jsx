import { useEffect } from "react";
import { X } from "lucide-react";

/**
 * Dialog of the Visitor Management module, used for every create/edit form so
 * the screens keep their table in view. Kept inside the feature rather than in
 * shared/components to stay merge-safe with the other modules.
 *
 * Escape and the overlay close it; the content scrolls on its own so a long
 * form never pushes the header out of reach.
 */
export default function Modal({ title, onClose, children, widthClass = "max-w-2xl" }) {
    useEffect(() => {
        function handleKey(event) {
            if (event.key === "Escape") onClose();
        }
        document.addEventListener("keydown", handleKey);
        return () => document.removeEventListener("keydown", handleKey);
    }, [onClose]);

    return (
        <div
            className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/50 p-4 sm:p-8"
            onClick={onClose}
            role="presentation"
        >
            <div
                role="dialog"
                aria-modal="true"
                aria-label={title}
                onClick={(event) => event.stopPropagation()}
                className={`mt-6 w-full ${widthClass} rounded-lg bg-white shadow-xl`}
            >
                <div className="flex items-center justify-between gap-3 border-b border-line px-5 py-3.5">
                    <h3 className="font-heading text-base font-bold text-ink">{title}</h3>
                    <button
                        type="button"
                        onClick={onClose}
                        aria-label="Close"
                        className="rounded-md p-1 text-muted hover:bg-gray-100"
                    >
                        <X size={18} />
                    </button>
                </div>
                <div className="max-h-[75dvh] overflow-y-auto p-5">{children}</div>
            </div>
        </div>
    );
}