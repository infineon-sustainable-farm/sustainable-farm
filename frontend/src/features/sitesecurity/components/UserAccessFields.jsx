import { useId } from "react";
import { cn } from "../utils/cn";
import { INPUT_CLASS, LABEL_CLASS } from "../utils/formClasses";
import { toIsoDate } from "../utils/credentialForm";
import { CATEGORIES, PERMISSIONS, ROLES, ZONES, toggleValue } from "../utils/userForm";
import { Field } from "./formUi";

function Select({ label, value, onChange, options, error }) {
    const id = useId();
    return (<div>
      <label htmlFor={id} className={LABEL_CLASS}>{label}</label>
      <select id={id} value={value} onChange={(e) => onChange(e.target.value)} aria-invalid={error ? true : undefined} className={INPUT_CLASS}>
        {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
      {error && <p className="mt-1 text-[11.5px] text-sec-red">{error}</p>}
    </div>);
}

function CheckboxGroup({ legend, options, values, onChange, disabled, hint }) {
    return (<fieldset disabled={disabled} className="disabled:opacity-50">
      <legend className={LABEL_CLASS}>{legend}</legend>
      <div className="grid grid-cols-2 gap-2">
        {options.map((o) => (<label key={o.value} className={cn("flex cursor-pointer items-center gap-2 rounded-xl border px-3 py-2 font-sans text-[12.5px] transition-colors", values.includes(o.value) ? "border-primary-600 bg-primary-50 text-navy-900" : "border-slate-200 text-slate-600 hover:border-slate-300")}>
            <input type="checkbox" checked={values.includes(o.value)} onChange={() => onChange(toggleValue(values, o.value))} className="size-4 accent-primary-600"/>
            {o.label}
          </label>))}
      </div>
      {hint && <p className="mt-1.5 text-[11.5px] text-slate-600">{hint}</p>}
    </fieldset>);
}

/** Name, category, role, permissions, zones and validity: shared by the invite and edit dialogs. */
export default function UserAccessFields({ form, setField, errors, showErrors }) {
    const shown = (field) => (showErrors ? errors[field] : null);
    const isAdminRole = form.role === "admin";
    return (<div className="space-y-4">
      <Field label="Full name" data-autofocus maxLength={100} value={form.fullName} onChange={(e) => setField("fullName", e.target.value)} error={shown("fullName")}/>
      <div className="grid grid-cols-2 gap-3">
        <Select label="Category" value={form.category} onChange={(v) => setField("category", v)} options={CATEGORIES} error={shown("category")}/>
        <Select label="Role" value={form.role} onChange={(v) => setField("role", v)} options={ROLES} error={shown("role")}/>
      </div>
      <CheckboxGroup legend="Screens this user may open" options={PERMISSIONS} values={form.permissions} onChange={(v) => setField("permissions", v)} disabled={isAdminRole} hint={isAdminRole ? "Admins can open every screen and manage users." : "None ticked: the user can sign in but sees no screen."}/>
      {form.hadLegacyAccess && !isAdminRole && (<label className="flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 font-sans text-[12.5px] text-amber-800">
          <input type="checkbox" checked={form.keepLegacyAccess} onChange={(e) => setField("keepLegacyAccess", e.target.checked)} className="mt-0.5 size-4"/>
          Keep legacy read-only access (all four views). Once removed, it cannot be given back.
        </label>)}
      <CheckboxGroup legend="Zones" options={ZONES} values={form.zoneIds} onChange={(v) => setField("zoneIds", v)} hint="The user only sees data of the ticked zones."/>
      <Field label="Valid until (optional)" type="date" min={toIsoDate(new Date())} value={form.validUntil} onChange={(e) => setField("validUntil", e.target.value)} error={shown("validUntil")} inputClassName="font-mono text-[13px]" hint="Leave empty for no end date."/>
    </div>);
}
