import { useState } from "react";
import { Mail } from "lucide-react";
import { requestPasswordReset } from "../api/sitesecurityApi";
import { isValidEmail } from "../utils/credentialForm";
import { LINK_BUTTON, PRIMARY_BUTTON } from "../utils/formClasses";
import { authErrorMessage } from "../utils/authErrors";
import { ErrorAlert, Field, SuccessNote } from "./formUi";

const SENT_MESSAGE = "If an account exists, a reset link has been sent.";

export default function ForgotPasswordForm({ initialEmail = "", onBack }) {
    const [email, setEmail] = useState(initialEmail);
    const [sent, setSent] = useState(false);
    const [error, setError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const cleanEmail = email.trim();

    const submit = async (event) => {
        event.preventDefault();
        if (!isValidEmail(cleanEmail)) return;
        setIsSubmitting(true);
        setError(null);
        try {
            await requestPasswordReset(cleanEmail);
            setSent(true);
        }
        catch (err) {
            /* The answer never says whether the account exists; only network-level problems show. */
            if (err.status === 403 || err.status === 429 || !err.status) {
                setError(authErrorMessage(err, SENT_MESSAGE, "The request could not be sent."));
            }
            else {
                setSent(true);
            }
        }
        finally {
            setIsSubmitting(false);
        }
    };

    if (sent) {
        return (<div className="mt-5 space-y-4">
          <SuccessNote>{SENT_MESSAGE} The link is valid for a limited time; check your spam folder too.</SuccessNote>
          <button type="button" onClick={onBack} className={`${LINK_BUTTON} w-full text-center`}>Back to sign in</button>
        </div>);
    }

    return (<>
      <p className="mt-1 font-sans text-[13px] text-slate-600">Enter your work email. If it has an account, a link to choose a new password is sent to it.</p>
      <form onSubmit={submit} className="mt-5 space-y-4">
        <Field label="Email" id="sitesecurity-forgot-email" type="email" autoFocus autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)}/>
        <ErrorAlert>{error}</ErrorAlert>
        <button type="submit" disabled={!isValidEmail(cleanEmail) || isSubmitting} className={`${PRIMARY_BUTTON} w-full`}>
          <Mail className="size-4"/>
          Send reset link
        </button>
        <button type="button" onClick={onBack} className={`${LINK_BUTTON} w-full text-center`}>Back to sign in</button>
      </form>
    </>);
}
