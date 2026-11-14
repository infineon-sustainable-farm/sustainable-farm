import { useState } from "react";
import { KeyRound, Mail } from "lucide-react";
import { requestSignInCode, verifySignInCode } from "../api/sitesecurityApi";
import { isValidEmail } from "../utils/credentialForm";
import { cleanSignInCode, isValidSignInCode } from "../utils/signInCode";
import { LINK_BUTTON, PRIMARY_BUTTON } from "../utils/formClasses";
import { ErrorAlert, Field } from "./formUi";
import { authErrorMessage } from "../utils/authErrors";

const CODE_REFUSED = "This code is invalid or has expired. Check the latest email. A new code can be sent once this one has expired or after 5 wrong tries.";

/** Legacy path for accounts that have no password yet: a code sent by email. */
export default function CodeSignIn({ initialEmail = "", onAuthenticated, onBack }) {
    const [step, setStep] = useState("email");
    const [email, setEmail] = useState(initialEmail);
    const [code, setCode] = useState("");
    const [error, setError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const cleanEmail = email.trim();
    const cleanCode = cleanSignInCode(code);

    const sendCode = async (event) => {
        event.preventDefault();
        if (!isValidEmail(cleanEmail)) return;
        setIsSubmitting(true);
        setError(null);
        try {
            await requestSignInCode(cleanEmail);
            setCode("");
            setStep("code");
        }
        catch (err) {
            setError(authErrorMessage(err, CODE_REFUSED, "The code could not be sent."));
        }
        finally {
            setIsSubmitting(false);
        }
    };

    const checkCode = async (event) => {
        event.preventDefault();
        if (!isValidSignInCode(cleanCode)) return;
        setIsSubmitting(true);
        setError(null);
        try {
            onAuthenticated(await verifySignInCode(cleanEmail, cleanCode));
        }
        catch (err) {
            setError(authErrorMessage(err, CODE_REFUSED, "The code could not be checked."));
            setIsSubmitting(false);
        }
    };

    if (step === "email") {
        return (<>
          <p className="mt-1 font-sans text-[13px] text-slate-600">
            For accounts that do not have a password yet. If the email belongs to an operator, a sign-in code is sent to it: 6 digits, or 10 after repeated wrong codes.
          </p>
          <form onSubmit={sendCode} className="mt-5 space-y-4">
            <Field label="Email" id="sitesecurity-code-email" type="email" autoFocus autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)}/>
            <ErrorAlert>{error}</ErrorAlert>
            <button type="submit" disabled={!isValidEmail(cleanEmail) || isSubmitting} className={`${PRIMARY_BUTTON} w-full`}>
              <Mail className="size-4"/>
              Send me a code
            </button>
            <button type="button" onClick={onBack} className={`${LINK_BUTTON} w-full text-center`}>
              Sign in with a password
            </button>
          </form>
        </>);
    }

    return (<>
      <p className="mt-1 font-sans text-[13px] text-slate-600">
        If <span className="font-semibold text-navy-900">{cleanEmail}</span> belongs to an operator, a code is on its way.
        It is valid for 5 minutes. While a code is still valid, asking again sends nothing new: use the latest code you received.
      </p>
      <form onSubmit={checkCode} className="mt-5 space-y-4">
        <Field label="Code from the email (6 or 10 digits)" id="sitesecurity-code" type="text" inputMode="numeric" autoComplete="one-time-code" autoFocus maxLength={14} value={code} onChange={(e) => setCode(e.target.value)} inputClassName="tracking-[0.3em]"/>
        <ErrorAlert>{error}</ErrorAlert>
        <button type="submit" disabled={!isValidSignInCode(cleanCode) || isSubmitting} className={`${PRIMARY_BUTTON} w-full`}>
          <KeyRound className="size-4"/>
          Sign in
        </button>
        <button type="button" onClick={() => { setStep("email"); setCode(""); setError(null); }} className={`${LINK_BUTTON} w-full text-center`}>
          Use another email or ask for a code again
        </button>
      </form>
    </>);
}
