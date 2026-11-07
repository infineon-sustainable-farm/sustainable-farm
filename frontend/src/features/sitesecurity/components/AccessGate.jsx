import { useEffect, useState } from "react";
import { KeyRound, Mail } from "lucide-react";
import { requestSignInCode, restoreSession, verifySignInCode } from "../api/sitesecurityApi";
import { isValidEmail } from "../utils/credentialForm";
import { Card, Eyebrow } from "./ui";

const INPUT_CLASS = "w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-[13.5px] text-navy-900 outline-none transition-all focus:border-accent-500 focus:ring-4 focus:ring-accent-100";
const BUTTON_CLASS = "inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary-600 px-4 py-2.5 text-[13px] font-semibold text-white transition-colors hover:bg-primary-700 disabled:cursor-not-allowed disabled:opacity-40 font-sans";

function errorMessage(err, fallback) {
    if (err.status === 401) return "This code is invalid or has expired. Check the latest email, or ask for a new code.";
    if (err.status === 403) return "Open Site Security from its usual address to sign in.";
    return err.message || fallback;
}

export default function AccessGate({ onAuthenticated }) {
    const [step, setStep] = useState("checking");
    const [email, setEmail] = useState("");
    const [code, setCode] = useState("");
    const [error, setError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    // A session opened earlier in this browser is still valid for up to 8 hours.
    useEffect(() => {
        let active = true;
        restoreSession()
            .then((role) => active && onAuthenticated(role))
            .catch(() => active && setStep("email"));
        return () => {
            active = false;
        };
    }, [onAuthenticated]);

    const cleanEmail = email.trim();
    const cleanCode = code.replace(/\s/g, "");

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
            setError(errorMessage(err, "The code could not be sent."));
        }
        finally {
            setIsSubmitting(false);
        }
    };

    const checkCode = async (event) => {
        event.preventDefault();
        if (!/^\d{6}$/.test(cleanCode)) return;
        setIsSubmitting(true);
        setError(null);
        try {
            onAuthenticated(await verifySignInCode(cleanEmail, cleanCode));
        }
        catch (err) {
            setError(errorMessage(err, "The code could not be checked."));
            setIsSubmitting(false);
        }
    };

    const restart = () => {
        setStep("email");
        setCode("");
        setError(null);
    };

    if (step === "checking") {
        return (<main className="flex min-h-screen items-center justify-center bg-[#f4f6f9] p-6">
          <p role="status" className="font-sans text-[13px] text-slate-600">Checking your session…</p>
        </main>);
    }

    return (<main className="flex min-h-screen items-center justify-center bg-[#f4f6f9] p-6">
      <Card className="w-full max-w-[420px] p-6">
        <Eyebrow className="mb-1 text-primary-700">Fenceline Security</Eyebrow>
        <h1 className="font-display text-[20px] font-semibold text-navy-900">Sign in</h1>

        {step === "email" ? (<>
          <p className="mt-1 text-[13px] text-slate-600 font-sans">
            Enter your work email. If it belongs to an operator, a 6-digit code is sent to it.
          </p>
          <form onSubmit={sendCode} className="mt-5 space-y-4">
            <div>
              <label htmlFor="sitesecurity-email" className="mb-1.5 block text-[12px] font-semibold text-slate-600">
                Email
              </label>
              <input id="sitesecurity-email" type="email" autoFocus autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} className={INPUT_CLASS}/>
            </div>
            {error && (<div role="alert" className="rounded-xl border border-sec-red/30 bg-sec-red/5 px-4 py-3 font-sans text-[13px] text-sec-red">
              {error}
            </div>)}
            <button type="submit" disabled={!isValidEmail(cleanEmail) || isSubmitting} className={BUTTON_CLASS}>
              <Mail className="size-4"/>
              Send me a code
            </button>
          </form>
        </>) : (<>
          <p className="mt-1 text-[13px] text-slate-600 font-sans">
            If <span className="font-semibold text-navy-900">{cleanEmail}</span> belongs to an operator, a code is on its way.
            It is valid for 5 minutes.
          </p>
          <form onSubmit={checkCode} className="mt-5 space-y-4">
            <div>
              <label htmlFor="sitesecurity-code" className="mb-1.5 block text-[12px] font-semibold text-slate-600">
                6-digit code
              </label>
              <input id="sitesecurity-code" type="text" inputMode="numeric" autoComplete="one-time-code" autoFocus maxLength={7} value={code} onChange={(e) => setCode(e.target.value)} className={`${INPUT_CLASS} tracking-[0.3em]`}/>
            </div>
            {error && (<div role="alert" className="rounded-xl border border-sec-red/30 bg-sec-red/5 px-4 py-3 font-sans text-[13px] text-sec-red">
              {error}
            </div>)}
            <button type="submit" disabled={!/^\d{6}$/.test(cleanCode) || isSubmitting} className={BUTTON_CLASS}>
              <KeyRound className="size-4"/>
              Sign in
            </button>
            <button type="button" onClick={restart} className="w-full text-center font-sans text-[12.5px] font-semibold text-primary-700 hover:underline">
              Use another email or get a new code
            </button>
          </form>
        </>)}
      </Card>
    </main>);
}
