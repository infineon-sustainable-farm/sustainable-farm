import { useState } from "react";
import { DANGER_BUTTON, PRIMARY_BUTTON, SECONDARY_BUTTON } from "../utils/formClasses";
import { ErrorAlert } from "./formUi";
import Modal from "./Modal";

/**
 * Asks before an action on a user (revoke, disable...). `request` is
 * { title, message, confirmLabel, danger, run } or null; run() returns a promise.
 */
export default function ConfirmActionDialog({ request, onClose }) {
    const [error, setError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const close = () => {
        setError(null);
        onClose();
    };

    const confirm = async () => {
        setIsSubmitting(true);
        setError(null);
        try {
            await request.run();
            close();
        }
        catch (err) {
            setError(err.message || "The action failed.");
        }
        finally {
            setIsSubmitting(false);
        }
    };

    return (<Modal open={Boolean(request)} onClose={close} eyebrow="Please confirm" title={request?.title ?? ""}>
      <div className="space-y-4">
        <p className="font-sans text-[13px] text-slate-600">{request?.message}</p>
        <ErrorAlert>{error}</ErrorAlert>
        <div className="flex justify-end gap-2.5">
          <button type="button" onClick={close} className={SECONDARY_BUTTON}>Cancel</button>
          <button type="button" data-autofocus onClick={confirm} disabled={isSubmitting} className={request?.danger ? DANGER_BUTTON : PRIMARY_BUTTON}>
            {request?.confirmLabel ?? "Confirm"}
          </button>
        </div>
      </div>
    </Modal>);
}
