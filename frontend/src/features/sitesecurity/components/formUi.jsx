import { useId } from "react";
import { Check, X } from "lucide-react";
import { cn } from "../utils/cn";
import { INPUT_CLASS, LABEL_CLASS } from "../utils/formClasses";
import { passwordRuleChecks } from "../utils/passwordPolicy";
import { Card, Eyebrow } from "./ui";

/** The centred card of the sign-in, activation and reset pages. */
export function AuthShell({ title, children }) {
    return (<main className="flex min-h-screen items-center justify-center bg-[#f4f6f9] p-6">
      <Card className="w-full max-w-[420px] p-6">
        <Eyebrow className="mb-1 text-primary-700">Fenceline Security</Eyebrow>
        <h1 className="font-display text-[20px] font-semibold text-navy-900">{title}</h1>
        {children}
      </Card>
    </main>);
}

export function ErrorAlert({ children }) {
    if (!children) return null;
    return (<div role="alert" className="rounded-xl border border-sec-red/30 bg-sec-red/5 px-4 py-3 font-sans text-[13px] text-sec-red">
      {children}
    </div>);
}

export function SuccessNote({ children }) {
    return (<div role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 font-sans text-[13px] text-emerald-800">
      {children}
    </div>);
}

/** A labelled input with its error wired to aria-describedby. */
export function Field({ label, error, hint, className, inputClassName, ...inputProps }) {
    const generated = useId();
    const id = inputProps.id ?? generated;
    const errorId = `${id}-error`;
    const hintId = `${id}-hint`;
    const describedBy = [error && errorId, hint && hintId, inputProps["aria-describedby"]].filter(Boolean).join(" ") || undefined;
    return (<div className={className}>
      <label htmlFor={id} className={LABEL_CLASS}>{label}</label>
      <input {...inputProps} id={id} aria-invalid={error ? true : undefined} aria-describedby={describedBy} className={cn(INPUT_CLASS, inputClassName)}/>
      {hint && <p id={hintId} className="mt-1 text-[11.5px] text-slate-600">{hint}</p>}
      {error && <p id={errorId} className="mt-1 text-[11.5px] text-sec-red">{error}</p>}
    </div>);
}

/** New password + confirmation with live policy hints. */
export function NewPasswordFields({ password, confirmation, onPasswordChange, onConfirmationChange, email = null, serverError, autoFocus = false, label = "New password" }) {
    const hintsId = useId();
    const checks = passwordRuleChecks(password, email);
    const mismatch = confirmation && password !== confirmation ? "The two passwords do not match." : null;
    return (<div className="space-y-4">
      <div>
        <Field label={label} type="password" autoComplete="new-password" autoFocus={autoFocus} data-autofocus={autoFocus || undefined} maxLength={256} value={password} onChange={(e) => onPasswordChange(e.target.value)} error={serverError} aria-describedby={hintsId}/>
        <ul id={hintsId} aria-label="Password rules" className="mt-2 space-y-1">
          {checks.map((rule) => (<li key={rule.id} className={cn("flex items-center gap-1.5 font-sans text-[11.5px]", rule.ok ? "text-emerald-700" : "text-slate-600")}>
              {rule.ok ? <Check className="size-3.5" aria-hidden="true"/> : <X className="size-3.5" aria-hidden="true"/>}
              <span>{rule.label}</span>
              <span className="sr-only">{rule.ok ? "(met)" : "(not met)"}</span>
            </li>))}
          <li className="font-sans text-[11.5px] text-slate-600">Spaces are allowed: a short sentence is easy to remember.</li>
        </ul>
      </div>
      <Field label="Confirm new password" type="password" autoComplete="new-password" maxLength={256} value={confirmation} onChange={(e) => onConfirmationChange(e.target.value)} error={mismatch}/>
    </div>);
}
