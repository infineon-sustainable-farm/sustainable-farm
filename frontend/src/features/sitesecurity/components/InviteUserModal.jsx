import { useState } from "react";
import { Send } from "lucide-react";
import { inviteUser } from "../api/sitesecurityApi";
import { toIsoDate } from "../utils/credentialForm";
import { buildInvitePayload, emptyInviteForm, inviteFormErrors, isFormValid } from "../utils/userForm";
import { PRIMARY_BUTTON, SECONDARY_BUTTON } from "../utils/formClasses";
import { ErrorAlert, Field } from "./formUi";
import Modal from "./Modal";
import UserAccessFields from "./UserAccessFields";

export default function InviteUserModal({ open, onClose, onInvited }) {
    const [form, setForm] = useState(emptyInviteForm);
    const [showErrors, setShowErrors] = useState(false);
    const [serverErrors, setServerErrors] = useState({});
    const [error, setError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const errors = { ...inviteFormErrors(form, toIsoDate(new Date())), ...serverErrors };
    const shown = (field) => (showErrors || serverErrors[field] ? errors[field] : null);
    /* A mismatch shows as soon as the second email is typed. */
    const confirmationError = shown("emailConfirmation") ?? (form.emailConfirmation && errors.emailConfirmation ? errors.emailConfirmation : null);

    const setField = (field, value) => {
        setForm((f) => ({ ...f, [field]: value }));
        setServerErrors((e) => ({ ...e, [field]: undefined }));
    };

    const close = () => {
        setForm(emptyInviteForm());
        setShowErrors(false);
        setServerErrors({});
        setError(null);
        onClose();
    };

    const submit = async (event) => {
        event.preventDefault();
        setShowErrors(true);
        if (!isFormValid(inviteFormErrors(form, toIsoDate(new Date()))) || isSubmitting) return;
        setIsSubmitting(true);
        setError(null);
        try {
            onInvited(await inviteUser(buildInvitePayload(form)));
            close();
        }
        catch (err) {
            const fieldErrors = err.data?.fieldErrors ?? {};
            if (err.status === 409 && !fieldErrors.email) fieldErrors.email = "An account with this email already exists.";
            setServerErrors(fieldErrors);
            setError(err.message || "The invitation could not be sent.");
        }
        finally {
            setIsSubmitting(false);
        }
    };

    return (<Modal open={open} onClose={close} eyebrow="New account" title="Invite user" className="max-w-[540px]">
      <form onSubmit={submit} noValidate className="space-y-4">
        <ErrorAlert>{error}</ErrorAlert>
        <UserAccessFields form={form} setField={setField} errors={errors} showErrors={showErrors}/>
        <Field label="Email" type="email" autoComplete="off" maxLength={254} value={form.email} onChange={(e) => setField("email", e.target.value)} error={shown("email")}/>
        <Field label="Confirm email" type="email" autoComplete="off" maxLength={254} value={form.emailConfirmation} onChange={(e) => setField("emailConfirmation", e.target.value)} onPaste={(e) => e.preventDefault()} error={confirmationError} hint="Type it again: the invitation link goes to this address."/>
        <p className="font-sans text-[12px] text-slate-600">An invitation email with a personal activation link is sent. The person chooses their own password.</p>
        <div className="flex justify-end gap-2.5 pt-1">
          <button type="button" onClick={close} className={SECONDARY_BUTTON}>Cancel</button>
          <button type="submit" disabled={isSubmitting} className={PRIMARY_BUTTON}>
            <Send className="size-4"/>
            Send invitation
          </button>
        </div>
      </form>
    </Modal>);
}
