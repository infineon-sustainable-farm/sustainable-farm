import { useState } from "react";
import { Save } from "lucide-react";
import { updateUser } from "../api/sitesecurityApi";
import { toIsoDate } from "../utils/credentialForm";
import { buildEditPayload, editFormErrors, editFormFromUser, isFormValid } from "../utils/userForm";
import { PRIMARY_BUTTON, SECONDARY_BUTTON } from "../utils/formClasses";
import { ErrorAlert } from "./formUi";
import Modal from "./Modal";
import UserAccessFields from "./UserAccessFields";

/* Mounted with key={user.id} by the screen, so the form starts from that user. */
function EditForm({ user, onClose, onSaved }) {
    const [form, setForm] = useState(() => editFormFromUser(user));
    const [serverErrors, setServerErrors] = useState({});
    const [error, setError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    /* An end date already in the past can be kept; a newly chosen one cannot be in the past. */
    const dateChanged = form.validUntil !== (user.validUntil ?? "");
    const localErrors = editFormErrors(form, dateChanged ? toIsoDate(new Date()) : null);
    const errors = { ...localErrors, ...serverErrors };

    const setField = (field, value) => {
        setForm((f) => ({ ...f, [field]: value }));
        setServerErrors((e) => ({ ...e, [field]: undefined }));
    };

    const submit = async (event) => {
        event.preventDefault();
        if (!isFormValid(localErrors) || isSubmitting) return;
        setIsSubmitting(true);
        setError(null);
        try {
            onSaved(await updateUser(user.id, buildEditPayload(form)));
            onClose();
        }
        catch (err) {
            setServerErrors(err.data?.fieldErrors ?? {});
            setError(err.message || "The changes could not be saved.");
            setIsSubmitting(false);
        }
    };

    return (<form onSubmit={submit} noValidate className="space-y-4">
      <p className="font-sans text-[12.5px] text-slate-600">{user.email}</p>
      <ErrorAlert>{error}</ErrorAlert>
      <UserAccessFields form={form} setField={setField} errors={errors} showErrors/>
      <div className="flex justify-end gap-2.5 pt-1">
        <button type="button" onClick={onClose} className={SECONDARY_BUTTON}>Cancel</button>
        <button type="submit" disabled={!isFormValid(localErrors) || isSubmitting} className={PRIMARY_BUTTON}>
          <Save className="size-4"/>
          Save changes
        </button>
      </div>
    </form>);
}

export default function EditUserModal({ user, onClose, onSaved }) {
    return (<Modal open={Boolean(user)} onClose={onClose} eyebrow="Edit account" title={user?.fullName || "Edit user"} className="max-w-[540px]">
      {user && <EditForm key={user.id} user={user} onClose={onClose} onSaved={onSaved}/>}
    </Modal>);
}
