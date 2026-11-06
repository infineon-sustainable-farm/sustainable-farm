import { useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { HardHat, KeyRound, ShieldCheck, UserRound, Wrench, X } from "lucide-react";
import { cn } from "../utils/cn";
import { Eyebrow, SensitivityMeter } from "./ui";
import { LEVEL_META, LEVEL_ORDER } from "../api/sitesecurityTypes";
import { allowedLevels, buildCredentialPayload, defaultValidUntil, isCredentialFormComplete, levelForType, toIsoDate, } from "../utils/credentialForm";
import { useDialogFocus } from "../hooks/useDialogFocus";
const TYPES = [
    { type: "Staff", icon: HardHat, hint: "Daily operations" },
    { type: "Visitor", icon: UserRound, hint: "Time-boxed pass" },
    { type: "Service", icon: Wrench, hint: "Contractors & teams" },
    { type: "Security", icon: ShieldCheck, hint: "Full oversight" },
];
const INPUT_CLASS = "w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-[13.5px] text-navy-900 outline-none transition-all placeholder:text-slate-500 focus:border-accent-500 focus:ring-4 focus:ring-accent-100";
function FieldError({ id, message }) {
    return message ? <p id={id} className="mt-1 text-[11.5px] text-sec-red">{message}</p> : null;
}
function errorProps(id, message) {
    return message ? { "aria-invalid": true, "aria-describedby": id } : {};
}
export default function AddAccessModal({ open, onClose, onAdd, }) {
    const dialogRef = useRef(null);
    const [name, setName] = useState("");
    const [role, setRole] = useState("");
    const [type, setType] = useState("Staff");
    const [level, setLevel] = useState("controlled");
    const [validUntil, setValidUntil] = useState(() => defaultValidUntil(new Date()));
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState(null);
    const [fieldErrors, setFieldErrors] = useState({});
    const form = { name, role, type, level, validUntil };
    const permittedLevels = allowedLevels(type);
    const reset = () => {
        setName("");
        setRole("");
        setType("Staff");
        setLevel("controlled");
        setValidUntil(defaultValidUntil(new Date()));
        setError(null);
        setFieldErrors({});
    };
    const close = () => {
        reset();
        onClose();
    };
    useDialogFocus(dialogRef, open, close);
    const chooseType = (next) => {
        setType(next);
        setLevel((current) => levelForType(next, current));
    };
    const submit = async (event) => {
        event.preventDefault();
        if (!isCredentialFormComplete(form) || isSubmitting)
            return;
        setIsSubmitting(true);
        setError(null);
        setFieldErrors({});
        try {
            await onAdd(buildCredentialPayload(form));
            close();
        }
        catch (err) {
            setError(err.message);
            setFieldErrors(err.data?.fieldErrors ?? {});
        }
        finally {
            setIsSubmitting(false);
        }
    };
    return (<AnimatePresence>
      {open && (<motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/40 p-6 backdrop-blur-sm" onClick={close}>
          <motion.div ref={dialogRef} role="dialog" aria-modal="true" aria-labelledby="credential-dialog-title" tabIndex={-1} initial={{ opacity: 0, scale: 0.96, y: 14 }} animate={{ opacity: 1, scale: 1, y: 0 }} exit={{ opacity: 0, scale: 0.96, y: 14 }} transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }} onClick={(e) => e.stopPropagation()} className="max-h-full w-full max-w-[480px] overflow-y-auto rounded-2xl border border-slate-200 bg-white shadow-[0_24px_60px_-20px_rgba(10,17,31,0.4)] outline-none">
            <div className="flex items-start justify-between px-6 pb-4 pt-6">
              <div>
                <Eyebrow className="mb-1 text-accent-700">New credential</Eyebrow>
                <h3 id="credential-dialog-title" className="font-display text-[19px] font-semibold text-navy-900">Grant Access</h3>
              </div>
              <button type="button" onClick={close} aria-label="Close" className="rounded-lg p-1.5 text-slate-600 transition-colors hover:bg-slate-100 hover:text-navy-900">
                <X className="size-4"/>
              </button>
            </div>

            <form onSubmit={submit}>
              <div className="space-y-5 px-6 pb-2">
                {error && (<div role="alert" className="rounded-xl border border-sec-red/30 bg-sec-red/5 px-4 py-3 font-sans text-[13px] text-sec-red">
                  {error}
                </div>)}

                {/* name */}
                <div>
                  <label htmlFor="credential-name" className="mb-1.5 block text-[12px] font-semibold text-slate-600">Name</label>
                  <input id="credential-name" data-autofocus required maxLength={100} value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Alex Fourie" className={INPUT_CLASS} {...errorProps("credential-name-error", fieldErrors.name)}/>
                  <FieldError id="credential-name-error" message={fieldErrors.name}/>
                </div>

                {/* role */}
                <div>
                  <label htmlFor="credential-role" className="mb-1.5 block text-[12px] font-semibold text-slate-600">Role</label>
                  <input id="credential-role" required maxLength={100} value={role} onChange={(e) => setRole(e.target.value)} placeholder="e.g. Grounds supervisor" className={INPUT_CLASS} {...errorProps("credential-role-error", fieldErrors.role)}/>
                  <FieldError id="credential-role-error" message={fieldErrors.role}/>
                </div>

                {/* user type */}
                <div role="group" aria-labelledby="credential-type-label">
                  <span id="credential-type-label" className="mb-1.5 block text-[12px] font-semibold text-slate-600">User type</span>
                  <div className="grid grid-cols-4 gap-2">
                    {TYPES.map((t) => {
                const Icon = t.icon;
                const active = type === t.type;
                return (<button key={t.type} type="button" aria-pressed={active} onClick={() => chooseType(t.type)} className={cn("rounded-xl border px-2 py-2.5 text-center transition-all", active
                        ? "border-navy-900 bg-navy-900 text-white"
                        : "border-slate-200 bg-white text-slate-600 hover:border-slate-300")}>
                          <Icon className={cn("mx-auto size-4", active ? "text-white" : "text-slate-600")}/>
                          <span className="mt-1 block text-[11px] font-semibold">{t.type}</span>
                          <span className={cn("block text-[9px]", active ? "text-white/60" : "text-slate-600")}>
                            {t.hint}
                          </span>
                        </button>);
            })}
                  </div>
                  <FieldError id="credential-type-error" message={fieldErrors.type}/>
                </div>

                {/* access level */}
                <div role="group" aria-labelledby="credential-level-label">
                  <div className="mb-1.5 flex items-center justify-between text-[12px] font-semibold text-slate-600">
                    <span id="credential-level-label">Access level</span>
                    <span className="font-mono text-[9.5px] font-normal uppercase tracking-[0.14em] text-slate-600">
                      higher level = stronger control
                    </span>
                  </div>
                  <div className="grid grid-cols-4 gap-2">
                    {LEVEL_ORDER.map((l) => {
                const meta = LEVEL_META[l];
                const active = level === l;
                return (<button key={l} type="button" aria-pressed={active} disabled={!permittedLevels.includes(l)} onClick={() => setLevel(l)} className={cn("rounded-xl border px-2 py-2.5 text-center transition-all disabled:cursor-not-allowed disabled:opacity-40", active ? "border-transparent" : "border-slate-200 bg-white hover:border-slate-300")} style={active ? { backgroundColor: meta.fill, borderColor: meta.solid } : undefined}>
                          <span className="block text-[11px] font-bold uppercase tracking-wide" style={{ color: active ? "#101B2E" : "#64748B" }}>
                            {meta.label}
                          </span>
                          <span className="mt-1.5 flex justify-center">
                            <SensitivityMeter level={l}/>
                          </span>
                        </button>);
            })}
                  </div>
                  <p className="mt-2 text-[11.5px] leading-relaxed text-slate-600">
                    {level === "open" && "Open areas only — no farm credential needed, visitor check-in applies."}
                    {level === "controlled" && "Adds the Controlled Zone — staff credential required at gates."}
                    {level === "restricted" && "Adds the Restricted Zone — authorised personnel, entry is guarded."}
                    {level === "critical" && "Adds the Critical Zone — dual verification and supervisor alert on entry."}
                  </p>
                  <FieldError id="credential-level-error" message={fieldErrors.level}/>
                </div>

                {/* valid until */}
                <div>
                  <label htmlFor="credential-valid-until" className="mb-1.5 block text-[12px] font-semibold text-slate-600">Valid until</label>
                  <input id="credential-valid-until" type="date" required value={validUntil} min={toIsoDate(new Date())} onChange={(e) => setValidUntil(e.target.value)} className={cn(INPUT_CLASS, "font-mono text-[13px]")} {...errorProps("credential-date-error", fieldErrors.validUntil)}/>
                  <FieldError id="credential-date-error" message={fieldErrors.validUntil}/>
                </div>
              </div>

              <div className="flex items-center justify-end gap-2.5 px-6 py-5">
                <button type="button" onClick={close} className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-[13px] font-semibold text-slate-600 transition-colors hover:bg-slate-50">
                  Cancel
                </button>
                <button type="submit" disabled={!isCredentialFormComplete(form) || isSubmitting} className="inline-flex items-center gap-2 rounded-xl bg-accent-700 px-4 py-2.5 text-[13px] font-semibold text-white shadow-[0_8px_20px_-8px_rgba(178,58,104,0.7)] transition-all hover:bg-[#9a4200] disabled:cursor-not-allowed disabled:opacity-40 disabled:shadow-none">
                  <KeyRound className="size-4"/>
                  Grant Access
                </button>
              </div>
            </form>
          </motion.div>
        </motion.div>)}
    </AnimatePresence>);
}
