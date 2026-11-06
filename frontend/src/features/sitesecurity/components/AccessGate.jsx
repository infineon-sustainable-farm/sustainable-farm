import { useState } from "react";
import { KeyRound } from "lucide-react";
import { openSession } from "../api/sitesecurityApi";
import { Card, Eyebrow } from "./ui";

export default function AccessGate({ onAuthenticated }) {
    const [token, setToken] = useState("");
    const [error, setError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const submit = async (event) => {
        event.preventDefault();
        const value = token.trim();
        if (!value)
            return;
        setIsSubmitting(true);
        setError(null);
        try {
            onAuthenticated(await openSession(value));
        }
        catch (err) {
            setError(err.status === 401 ? "This access token was not accepted." : err.message);
            setIsSubmitting(false);
        }
    };

    return (<main className="flex min-h-screen items-center justify-center bg-[#f4f6f9] p-6">
      <Card className="w-full max-w-[420px] p-6">
        <Eyebrow className="mb-1 text-primary-700">Fenceline Security</Eyebrow>
        <h1 className="font-display text-[20px] font-semibold text-navy-900">Restricted access</h1>
        <p className="mt-1 text-[13px] text-slate-600 font-sans">
          Enter your access token. It is kept for this page only and is asked again after a reload.
        </p>

        <form onSubmit={submit} className="mt-5 space-y-4">
          <div>
            <label htmlFor="sitesecurity-token" className="mb-1.5 block text-[12px] font-semibold text-slate-600">
              Access token
            </label>
            <input id="sitesecurity-token" type="password" autoFocus autoComplete="off" value={token} onChange={(e) => setToken(e.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-[13.5px] text-navy-900 outline-none transition-all focus:border-accent-500 focus:ring-4 focus:ring-accent-100"/>
          </div>

          {error && (<div role="alert" className="rounded-xl border border-sec-red/30 bg-sec-red/5 px-4 py-3 font-sans text-[13px] text-sec-red">
            {error}
          </div>)}

          <button type="submit" disabled={!token.trim() || isSubmitting} className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary-600 px-4 py-2.5 text-[13px] font-semibold text-white transition-colors hover:bg-primary-700 disabled:cursor-not-allowed disabled:opacity-40 font-sans">
            <KeyRound className="size-4"/>
            Unlock
          </button>
        </form>
      </Card>
    </main>);
}
