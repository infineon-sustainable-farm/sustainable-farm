import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { CheckCircle2, KeyRound } from "lucide-react";
import { activateAccount, confirmInvitation } from "../api/sitesecurityApi";
import { useLinkToken } from "../hooks/useLinkToken";
import { isNewPasswordReady } from "../utils/passwordPolicy";
import { authErrorMessage, passwordFieldError } from "../utils/authErrors";
import { PRIMARY_BUTTON } from "../utils/formClasses";
import { AuthShell, ErrorAlert, NewPasswordFields } from "../components/formUi";
import LinkStatus from "../components/LinkStatus";

/** Public page of the invitation link: /sitesecurity/activate#token=... */
export default function ActivateAccount() {
    const navigate = useNavigate();
    const { token, state, info, error: linkError, markInvalid } = useLinkToken("invitation");
    const [confirmed, setConfirmed] = useState(false);
    const [password, setPassword] = useState("");
    const [confirmation, setConfirmation] = useState("");
    const [error, setError] = useState(null);
    const [fieldError, setFieldError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const fail = (err) => {
        if (err.status === 400 && !passwordFieldError(err)) {
            markInvalid();
            return;
        }
        setFieldError(passwordFieldError(err));
        setError(passwordFieldError(err) ? null : authErrorMessage(err, err.message, "Something went wrong. Try again."));
    };

    const confirm = async () => {
        setIsSubmitting(true);
        setError(null);
        try {
            await confirmInvitation(token);
            setConfirmed(true);
        }
        catch (err) {
            fail(err);
        }
        finally {
            setIsSubmitting(false);
        }
    };

    const activate = async (event) => {
        event.preventDefault();
        if (!isNewPasswordReady(password, confirmation)) return;
        setIsSubmitting(true);
        setError(null);
        setFieldError(null);
        try {
            await activateAccount(token, password);
            navigate("/sitesecurity", { replace: true });
        }
        catch (err) {
            fail(err);
            setIsSubmitting(false);
        }
    };

    if (state !== "ready") {
        return <AuthShell title="Activate your account"><LinkStatus state={state} error={linkError}/></AuthShell>;
    }

    return (<AuthShell title="Activate your account">
      <p className="mt-2 font-sans text-[13.5px] text-slate-600">
        Invitation for <span className="font-semibold text-navy-900">{info.fullName}</span> ({info.maskedEmail})
      </p>
      {!confirmed ? (<div className="mt-5 space-y-4">
          <p className="font-sans text-[12.5px] text-slate-600">If this is you, confirm the invitation to choose your password. If it is not, close this page.</p>
          <ErrorAlert>{error}</ErrorAlert>
          <button type="button" onClick={confirm} disabled={isSubmitting} className={`${PRIMARY_BUTTON} w-full`}>
            <CheckCircle2 className="size-4"/>
            Confirm invitation
          </button>
        </div>) : (<form onSubmit={activate} className="mt-5 space-y-4">
          <NewPasswordFields label="Choose a password" autoFocus password={password} confirmation={confirmation} onPasswordChange={setPassword} onConfirmationChange={setConfirmation} serverError={fieldError}/>
          <ErrorAlert>{error}</ErrorAlert>
          <button type="submit" disabled={!isNewPasswordReady(password, confirmation) || isSubmitting} className={`${PRIMARY_BUTTON} w-full`}>
            <KeyRound className="size-4"/>
            Activate and sign in
          </button>
        </form>)}
    </AuthShell>);
}
