import { useId, useState } from "react";
import { MailCheck } from "lucide-react";
import { confirmEmailDelivery } from "../api/sitesecurityApi";
import { emailKindLabel } from "../utils/userStatus";
import { INPUT_CLASS, LABEL_CLASS, PRIMARY_BUTTON, SECONDARY_BUTTON } from "../utils/formClasses";
import { ErrorAlert } from "./formUi";
import Modal from "./Modal";

/** The admin attests that an email reached a real inbox, with evidence. `target` is { user, delivery } or null. */
export default function ConfirmDeliveryDialog({ target, onClose, onConfirmed }) {
    const evidenceId = useId();
    const [evidence, setEvidence] = useState("");
    const [error, setError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const close = () => {
        setEvidence("");
        setError(null);
        onClose();
    };

    const submit = async (event) => {
        event.preventDefault();
        if (!evidence.trim()) return;
        setIsSubmitting(true);
        setError(null);
        try {
            await confirmEmailDelivery(target.delivery.deliveryId, evidence.trim());
            onConfirmed();
            close();
        }
        catch (err) {
            setError(err.message || "The confirmation could not be saved.");
        }
        finally {
            setIsSubmitting(false);
        }
    };

    return (<Modal open={Boolean(target)} onClose={close} eyebrow="Email delivery" title="Confirm real delivery">
      <form onSubmit={submit} className="space-y-4">
        <p className="font-sans text-[13px] text-slate-600">
          {emailKindLabel(target?.delivery?.kind)} to {target?.user?.fullName || target?.user?.email || "this user"}. Confirm only if you know it reached a real external inbox,
          for example because the person told you or the provider log shows it.
        </p>
        <div>
          <label htmlFor={evidenceId} className={LABEL_CLASS}>Evidence</label>
          <textarea id={evidenceId} data-autofocus rows={3} maxLength={200} value={evidence} onChange={(e) => setEvidence(e.target.value)} placeholder="e.g. provider log id, or how the person confirmed" className={INPUT_CLASS}/>
        </div>
        <ErrorAlert>{error}</ErrorAlert>
        <div className="flex justify-end gap-2.5">
          <button type="button" onClick={close} className={SECONDARY_BUTTON}>Cancel</button>
          <button type="submit" disabled={!evidence.trim() || isSubmitting} className={PRIMARY_BUTTON}>
            <MailCheck className="size-4"/>
            Confirm delivery
          </button>
        </div>
      </form>
    </Modal>);
}
