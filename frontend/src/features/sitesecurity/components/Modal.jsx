import { useId, useRef } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import { cn } from "../utils/cn";
import { useDialogFocus } from "../hooks/useDialogFocus";
import { Eyebrow } from "./ui";

/** The dialog shell of AddAccessModal, shared by the account and user dialogs. */
export default function Modal({ open, onClose, eyebrow, title, children, className }) {
    const dialogRef = useRef(null);
    const titleId = useId();
    useDialogFocus(dialogRef, open, onClose);
    return (<AnimatePresence>
      {open && (<motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/40 p-6 backdrop-blur-sm" onClick={onClose}>
          <motion.div ref={dialogRef} role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1} initial={{ opacity: 0, scale: 0.96, y: 14 }} animate={{ opacity: 1, scale: 1, y: 0 }} exit={{ opacity: 0, scale: 0.96, y: 14 }} transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }} onClick={(e) => e.stopPropagation()} className={cn("max-h-full w-full max-w-[480px] overflow-y-auto rounded-2xl border border-slate-200 bg-white shadow-[0_24px_60px_-20px_rgba(10,17,31,0.4)] outline-none", className)}>
            <div className="flex items-start justify-between px-6 pb-4 pt-6">
              <div>
                {eyebrow && <Eyebrow className="mb-1 text-accent-700">{eyebrow}</Eyebrow>}
                <h3 id={titleId} className="font-display text-[19px] font-semibold text-navy-900">{title}</h3>
              </div>
              <button type="button" onClick={onClose} aria-label="Close" className="rounded-lg p-1.5 text-slate-600 transition-colors hover:bg-slate-100 hover:text-navy-900">
                <X className="size-4"/>
              </button>
            </div>
            <div className="px-6 pb-6">{children}</div>
          </motion.div>
        </motion.div>)}
    </AnimatePresence>);
}
