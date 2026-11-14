import { useEffect, useState } from "react";
import { Check, X } from "lucide-react";
import { fetchCodeSignInSettings, restoreCodeSignIn, retireCodeSignIn } from "../api/sitesecurityApi";
import { codeSignInConditions } from "../utils/userStatus";
import { cn } from "../utils/cn";
import { DANGER_BUTTON, SECONDARY_BUTTON } from "../utils/formClasses";
import { ErrorAlert, Field } from "./formUi";
import { Card } from "./ui";

const CONFIRM_WORD = "RETIRE";

/** Admin card: when the legacy email-code sign-in may be retired, and the switch itself. */
export default function CodeSignInCard() {
    const [settings, setSettings] = useState(null);
    const [typed, setTyped] = useState("");
    const [error, setError] = useState(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        let active = true;
        fetchCodeSignInSettings()
            .then((res) => active && setSettings(res))
            .catch((err) => active && setError(err.message));
        return () => {
            active = false;
        };
    }, []);

    const run = async (call) => {
        setIsSubmitting(true);
        setError(null);
        try {
            const next = await call();
            setSettings(next?.conditions ? next : await fetchCodeSignInSettings());
            setTyped("");
        }
        catch (err) {
            setError(err.message || "The setting could not be changed.");
        }
        finally {
            setIsSubmitting(false);
        }
    };

    const retired = Boolean(settings?.retired);
    return (<Card className="p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-display text-[16px] font-semibold text-navy-900">Legacy email-code sign-in</h2>
          <p className="mt-0.5 font-sans text-[12.5px] text-slate-600">
            {retired ? "Retired: every account signs in with a password." : "Still on for accounts without a password. Retire it once these conditions are met:"}
          </p>
        </div>
        {settings && (<span className={cn("rounded-full border px-2.5 py-0.5 font-sans text-[12px] font-medium", retired ? "border-slate-200 bg-slate-100 text-slate-600" : "border-amber-200 bg-amber-50 text-amber-700")}>
            {retired ? "Retired" : "Active"}
          </span>)}
      </div>
      {!settings && !error && <p role="status" className="mt-3 font-sans text-[13px] text-slate-600">Loading…</p>}
      {settings && !retired && (<ul className="mt-3 space-y-1.5">
          {codeSignInConditions(settings).map((c) => (<li key={c.id} className={cn("flex items-start gap-2 font-sans text-[12.5px]", c.ok ? "text-emerald-700" : "text-slate-600")}>
              {c.ok ? <Check className="mt-0.5 size-3.5 shrink-0" aria-hidden="true"/> : <X className="mt-0.5 size-3.5 shrink-0 text-rose-600" aria-hidden="true"/>}
              <span>{c.label}<span className="sr-only">{c.ok ? " (met)" : " (not met)"}</span></span>
            </li>))}
        </ul>)}
      <div className="mt-4 space-y-3">
        <ErrorAlert>{error}</ErrorAlert>
        {settings && !retired && (<form onSubmit={(e) => { e.preventDefault(); if (typed === CONFIRM_WORD && settings.canRetire) run(() => retireCodeSignIn(typed)); }} className="flex flex-wrap items-end gap-3">
            <Field label={`Type ${CONFIRM_WORD} to confirm`} className="w-[200px]" autoComplete="off" value={typed} onChange={(e) => setTyped(e.target.value)} disabled={!settings.canRetire}/>
            <button type="submit" disabled={!settings.canRetire || typed !== CONFIRM_WORD || isSubmitting} className={DANGER_BUTTON}>Retire email-code sign-in</button>
          </form>)}
        {retired && (<button type="button" onClick={() => run(restoreCodeSignIn)} disabled={isSubmitting} className={SECONDARY_BUTTON}>Restore email-code sign-in</button>)}
      </div>
    </Card>);
}
