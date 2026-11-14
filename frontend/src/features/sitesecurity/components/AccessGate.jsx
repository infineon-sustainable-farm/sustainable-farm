import { useEffect, useState } from "react";
import { LogIn } from "lucide-react";
import { restoreSession, signIn } from "../api/sitesecurityApi";
import { isValidEmail } from "../utils/credentialForm";
import { LINK_BUTTON, PRIMARY_BUTTON } from "../utils/formClasses";
import { WRONG_PASSWORD_MESSAGE, authErrorMessage } from "../utils/authErrors";
import { AuthShell, ErrorAlert, Field } from "./formUi";
import CodeSignIn from "./CodeSignIn";
import ForgotPasswordForm from "./ForgotPasswordForm";

const TITLE = { password: "Sign in", forgot: "Forgot password", code: "Sign in with an email code" };

/** onAuthenticated receives the whole SessionDto. */
export default function AccessGate({ onAuthenticated }) {
    const [step, setStep] = useState("checking");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const cleanEmail = email.trim();

    // A session opened earlier in this browser is still valid for up to 8 hours.
    useEffect(() => {
        let active = true;
        restoreSession()
            .then((session) => active && onAuthenticated(session))
            .catch(() => active && setStep("password"));
        return () => {
            active = false;
        };
    }, [onAuthenticated]);

    const submit = async (event) => {
        event.preventDefault();
        if (!isValidEmail(cleanEmail) || !password) return;
        setIsSubmitting(true);
        setError(null);
        try {
            onAuthenticated(await signIn(cleanEmail, password));
        }
        catch (err) {
            setError(authErrorMessage(err, WRONG_PASSWORD_MESSAGE, "Signing in failed."));
            setIsSubmitting(false);
        }
    };

    const goTo = (next) => {
        setError(null);
        setPassword("");
        setStep(next);
    };

    if (step === "checking") {
        return (<main className="flex min-h-screen items-center justify-center bg-[#f4f6f9] p-6">
          <p role="status" className="font-sans text-[13px] text-slate-600">Checking your session…</p>
        </main>);
    }

    return (<AuthShell title={TITLE[step]}>
      {step === "forgot" && <ForgotPasswordForm initialEmail={cleanEmail} onBack={() => goTo("password")}/>}
      {step === "code" && <CodeSignIn initialEmail={cleanEmail} onAuthenticated={onAuthenticated} onBack={() => goTo("password")}/>}
      {step === "password" && (<form onSubmit={submit} className="mt-5 space-y-4">
          <Field label="Email" id="sitesecurity-email" type="email" autoFocus autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)}/>
          <div>
            <Field label="Password" id="sitesecurity-password" type="password" autoComplete="current-password" maxLength={256} value={password} onChange={(e) => setPassword(e.target.value)}/>
            <div className="mt-1.5 text-right">
              <button type="button" onClick={() => goTo("forgot")} className={LINK_BUTTON}>Forgot password?</button>
            </div>
          </div>
          <ErrorAlert>{error}</ErrorAlert>
          <button type="submit" disabled={!isValidEmail(cleanEmail) || !password || isSubmitting} className={`${PRIMARY_BUTTON} w-full`}>
            <LogIn className="size-4"/>
            Sign in
          </button>
          <div className="border-t border-slate-100 pt-4 text-center">
            <button type="button" onClick={() => goTo("code")} className={LINK_BUTTON}>Sign in with an email code</button>
            <p className="mt-1 font-sans text-[11.5px] text-slate-600">Only for accounts that have no password yet.</p>
          </div>
        </form>)}
    </AuthShell>);
}
