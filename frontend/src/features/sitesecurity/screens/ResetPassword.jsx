import { useState } from "react";
import { Link } from "react-router-dom";
import { KeyRound } from "lucide-react";
import { resetPassword } from "../api/sitesecurityApi";
import { useLinkToken } from "../hooks/useLinkToken";
import { isNewPasswordReady } from "../utils/passwordPolicy";
import { authErrorMessage, passwordFieldError } from "../utils/authErrors";
import { LINK_BUTTON, PRIMARY_BUTTON } from "../utils/formClasses";
import { AuthShell, ErrorAlert, NewPasswordFields, SuccessNote } from "../components/formUi";
import LinkStatus from "../components/LinkStatus";

/** Public page of the reset link: /sitesecurity/reset-password#token=... */
export default function ResetPassword() {
    const { token, state, info, error: linkError, markInvalid } = useLinkToken("password_reset");
    const [password, setPassword] = useState("");
    const [confirmation, setConfirmation] = useState("");
    const [done, setDone] = useState(false);
    const [error, setError] = useState(null);
    const [fieldError, setFieldError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const submit = async (event) => {
        event.preventDefault();
        if (!isNewPasswordReady(password, confirmation)) return;
        setIsSubmitting(true);
        setError(null);
        setFieldError(null);
        try {
            await resetPassword(token, password);
            setDone(true);
        }
        catch (err) {
            if (err.status === 400 && !passwordFieldError(err)) markInvalid();
            setFieldError(passwordFieldError(err));
            if (err.status !== 400) setError(authErrorMessage(err, err.message, "The password could not be changed."));
        }
        finally {
            setIsSubmitting(false);
        }
    };

    if (done) {
        return (<AuthShell title="Password changed">
          <div className="mt-5 space-y-4">
            <SuccessNote>Your password has been changed. Sign in with your new password.</SuccessNote>
            <Link to="/sitesecurity" className={`${LINK_BUTTON} block text-center`}>Go to sign in</Link>
          </div>
        </AuthShell>);
    }

    if (state !== "ready") {
        return <AuthShell title="Choose a new password"><LinkStatus state={state} error={linkError}/></AuthShell>;
    }

    return (<AuthShell title="Choose a new password">
      <p className="mt-2 font-sans text-[13.5px] text-slate-600">
        Password reset for <span className="font-semibold text-navy-900">{info.fullName}</span> ({info.maskedEmail})
      </p>
      <form onSubmit={submit} className="mt-5 space-y-4">
        <NewPasswordFields autoFocus password={password} confirmation={confirmation} onPasswordChange={setPassword} onConfirmationChange={setConfirmation} serverError={fieldError}/>
        <ErrorAlert>{error}</ErrorAlert>
        <button type="submit" disabled={!isNewPasswordReady(password, confirmation) || isSubmitting} className={`${PRIMARY_BUTTON} w-full`}>
          <KeyRound className="size-4"/>
          Change password
        </button>
      </form>
    </AuthShell>);
}
