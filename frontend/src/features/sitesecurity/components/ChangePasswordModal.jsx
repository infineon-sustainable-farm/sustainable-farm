import { useState } from "react";
import { KeyRound } from "lucide-react";
import { changePassword } from "../api/sitesecurityApi";
import { isNewPasswordReady } from "../utils/passwordPolicy";
import { passwordFieldError } from "../utils/authErrors";
import { PRIMARY_BUTTON, SECONDARY_BUTTON } from "../utils/formClasses";
import { ErrorAlert, Field, NewPasswordFields, SuccessNote } from "./formUi";
import Modal from "./Modal";

export default function ChangePasswordModal({ open, onClose, email }) {
    const [current, setCurrent] = useState("");
    const [password, setPassword] = useState("");
    const [confirmation, setConfirmation] = useState("");
    const [error, setError] = useState(null);
    const [fieldError, setFieldError] = useState(null);
    const [done, setDone] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const ready = Boolean(current) && isNewPasswordReady(password, confirmation, email);

    const close = () => {
        setCurrent("");
        setPassword("");
        setConfirmation("");
        setError(null);
        setFieldError(null);
        setDone(false);
        onClose();
    };

    const submit = async (event) => {
        event.preventDefault();
        if (!ready) return;
        setIsSubmitting(true);
        setError(null);
        setFieldError(null);
        try {
            await changePassword(current, password);
            setDone(true);
        }
        catch (err) {
            const field = passwordFieldError(err) ?? err.data?.fieldErrors?.newPassword ?? null;
            setFieldError(field);
            setError(err.status === 401 ? "The current password is incorrect." : field ? null : err.message || "The password could not be changed.");
        }
        finally {
            setIsSubmitting(false);
        }
    };

    return (<Modal open={open} onClose={close} eyebrow="Your account" title="Change password">
      {done ? (<div className="space-y-4">
          <SuccessNote>Your password has been changed. Your other sessions have been signed out.</SuccessNote>
          <div className="flex justify-end">
            <button type="button" data-autofocus onClick={close} className={PRIMARY_BUTTON}>Done</button>
          </div>
        </div>) : (<form onSubmit={submit} className="space-y-4">
          <Field label="Current password" type="password" autoComplete="current-password" data-autofocus maxLength={256} value={current} onChange={(e) => setCurrent(e.target.value)}/>
          <NewPasswordFields password={password} confirmation={confirmation} onPasswordChange={setPassword} onConfirmationChange={setConfirmation} email={email} serverError={fieldError}/>
          <ErrorAlert>{error}</ErrorAlert>
          <div className="flex justify-end gap-2.5 pt-1">
            <button type="button" onClick={close} className={SECONDARY_BUTTON}>Cancel</button>
            <button type="submit" disabled={!ready || isSubmitting} className={PRIMARY_BUTTON}>
              <KeyRound className="size-4"/>
              Change password
            </button>
          </div>
        </form>)}
    </Modal>);
}
