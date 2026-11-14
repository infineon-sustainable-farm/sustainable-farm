import { useRef } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import { useDialogFocus } from "../hooks/useDialogFocus";
import { Eyebrow } from "./ui";
import UserHistoryContent from "./UserHistoryContent";

/** Drawer with the emails and the event history of one account. */
export default function UserHistoryDrawer({ user, onClose, onConfirmDelivery, refreshSeed }) {
    const dialogRef = useRef(null);
    useDialogFocus(dialogRef, Boolean(user), onClose);
    return (<AnimatePresence>
      {user && (<>
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 z-40 bg-navy-950/30 backdrop-blur-[2px]" onClick={onClose}/>
          <motion.div ref={dialogRef} role="dialog" aria-modal="true" aria-label={`History of ${user.fullName || user.email}`} tabIndex={-1} initial={{ x: "104%" }} animate={{ x: 0 }} exit={{ x: "104%" }} transition={{ type: "spring", stiffness: 340, damping: 34 }} className="fixed inset-y-0 right-0 z-40 flex w-full max-w-[440px] flex-col bg-white shadow-[0_24px_80px_-20px_rgba(10,17,31,0.45)] outline-none">
            <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
              <div className="min-w-0">
                <Eyebrow>Account history</Eyebrow>
                <p className="mt-0.5 truncate font-sans text-[13px] font-semibold text-navy-900">{user.fullName || user.email}</p>
              </div>
              <button type="button" data-autofocus onClick={onClose} aria-label="Close history" className="rounded-lg p-1.5 text-slate-600 transition-colors hover:bg-slate-100 hover:text-navy-900">
                <X className="size-4"/>
              </button>
            </div>
            <div className="flex-1 overflow-y-auto px-6 py-5">
              <UserHistoryContent key={`${user.id}-${refreshSeed}`} userId={user.id} onConfirmDelivery={(delivery) => onConfirmDelivery(user, delivery)}/>
            </div>
          </motion.div>
        </>)}
    </AnimatePresence>);
}
