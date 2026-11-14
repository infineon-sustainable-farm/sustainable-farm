import { useState } from "react";
import { KeyRound, Mail, ShieldAlert } from "lucide-react";
import { refreshSession, requestSignInCode, setupPassword } from "../api/sitesecurityApi";
import { cleanSignInCode, isValidSignInCode } from "../utils/signInCode";
import { isNewPasswordReady } from "../utils/passwordPolicy";
import { authErrorMessage, passwordFieldError } from "../utils/authErrors";
import { PRIMARY_BUTTON } from "../utils/formClasses";
import { ErrorAlert, Field, NewPasswordFields } from "./formUi";
import { Card } from "./ui";

/** Stage 1: an account still signing in with email codes chooses a password. */
export default function SetPasswordCard({ session, onSessionChange }) {
    const [step, setStep] = useState("intro");
    const [code, setCode] = useState("");
    const [password, setPassword] = useState("");
    const [confirmation, setConfirmation] = useState("");
    const [error, setError] = useState(null);
    const [fieldError, setFieldError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const cleanCode = cleanSignInCode(code);
    const ready = isValidSignInCode(cleanCode) && isNewPasswordReady(password, confirmation, session.email);

    const sendCode = async () => {
        setIsSubmitting(true);
        setError(null);
        try {
            await requestSignInCode(session.email);
            setStep("form");
        }
        catch (err) {
            setError(authErrorMessage(err, err.message, "The code could not be sent."));
        }
        finally {
            setIsSubmitting(false);
        }
    };

    const submit = async (event) => {
        event.preventDefault();
        if (!ready) return;
        setIsSubmitting(true);
        setError(null);
        setFieldError(null);
        try {
            await setupPassword(cleanCode, password);
            onSessionChange(await refreshSession());
        }
        catch (err) {
            setFieldError(passwordFieldError(err));
            setError(passwordFieldError(err) ? null : err.status === 400 ? "The code is wrong or older than 10 minutes. Ask for a new one." : authErrorMessage(err, err.message, "The password could not be saved."));
            setIsSubmitting(false);
        }
    };

    return (<Card className="mb-6 border-amber-200 bg-amber-50/60 p-5">
      <div className="flex items-start gap-3">
        <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-amber-100 text-amber-700">
          <ShieldAlert className="size-5"/>
        </span>
        <div className="min-w-0 flex-1">
          <h2 className="font-display text-[16px] font-semibold text-navy-900">Set your password</h2>
          <p className="mt-0.5 font-sans text-[13px] text-slate-600">
            You still sign in with email codes, which will be retired. Choose a password now: we send a code to {session.email} to confirm it is you.
          </p>
          {step === "intro" ? (<div className="mt-4 space-y-3">
              <ErrorAlert>{error}</ErrorAlert>
              <button type="button" onClick={sendCode} disabled={isSubmitting} className={PRIMARY_BUTTON}>
                <Mail className="size-4"/>
                Send me a code
              </button>
            </div>) : (<form onSubmit={submit} className="mt-4 grid max-w-[460px] gap-4">
              <Field label="Code from the email" type="text" inputMode="numeric" autoComplete="one-time-code" autoFocus maxLength={14} value={code} onChange={(e) => setCode(e.target.value)} hint="Use it within 10 minutes." inputClassName="tracking-[0.3em]"/>
              <NewPasswordFields password={password} confirmation={confirmation} onPasswordChange={setPassword} onConfirmationChange={setConfirmation} email={session.email} serverError={fieldError}/>
              <ErrorAlert>{error}</ErrorAlert>
              <div className="flex flex-wrap gap-3">
                <button type="submit" disabled={!ready || isSubmitting} className={PRIMARY_BUTTON}>
                  <KeyRound className="size-4"/>
                  Save my password
                </button>
                <button type="button" onClick={sendCode} disabled={isSubmitting} className="font-sans text-[12.5px] font-semibold text-primary-700 hover:underline disabled:opacity-40">
                  Send a new code
                </button>
              </div>
            </form>)}
        </div>
      </div>
    </Card>);
}
