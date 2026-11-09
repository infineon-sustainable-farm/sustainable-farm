import { useState } from "react";
import { X } from "lucide-react";

function todayAsLocalDate() {
  const today = new Date();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");
  return `${today.getFullYear()}-${month}-${day}`;
}

function initialValues(event) {
  return {
    date: event?.date || todayAsLocalDate(),
    triggerReason: event?.triggerReason || "",
    durationHours: event?.durationHours ?? "",
    fuelLiters: event?.fuelLiters ?? "",
    costEur: event?.costEur ?? "",
  };
}

function validate(values) {
  const errors = {};
  if (!values.date) {
    errors.date = "Date is required.";
  } else if (values.date > todayAsLocalDate()) {
    errors.date = "Date must be today or in the past.";
  }

  for (const field of ["durationHours", "fuelLiters", "costEur"]) {
    if (values[field] !== "" && (!Number.isFinite(Number(values[field])) || Number(values[field]) < 0)) {
      errors[field] = "Enter a value greater than or equal to 0.";
    }
  }

  if (values.triggerReason.length > 500) {
    errors.triggerReason = "Trigger reason must be 500 characters or fewer.";
  }
  return errors;
}

function toPayload(values) {
  return {
    date: values.date,
    triggerReason: values.triggerReason.trim() || null,
    durationHours: values.durationHours === "" ? null : Number(values.durationHours),
    fuelLiters: values.fuelLiters === "" ? null : Number(values.fuelLiters),
    costEur: values.costEur === "" ? null : Number(values.costEur),
  };
}

function FormField({ id, label, error, children }) {
  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-foreground">{label}</label>
      {children}
      {error && <p className="mt-1 text-xs text-destructive">{error}</p>}
    </div>
  );
}

const inputClass = "w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/20";

export default function GeneratorEventFormModal({ event, saving, error, onClose, onSave }) {
  const [values, setValues] = useState(() => initialValues(event));
  const [errors, setErrors] = useState({});
  const editing = Boolean(event);

  function updateField(field, value) {
    setValues((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(submitEvent) {
    submitEvent.preventDefault();
    const nextErrors = validate(values);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    try {
      await onSave(event?.eventId || null, toPayload(values));
      onClose();
    } catch {
      // The parent displays the API error while keeping the form open.
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" role="presentation">
      <section
        aria-labelledby="generator-event-form-title"
        aria-modal="true"
        className="w-full max-w-lg rounded-lg border border-border bg-card shadow-xl"
        role="dialog"
      >
        <div className="flex items-center justify-between border-b border-border px-5 py-4">
          <h2 id="generator-event-form-title" className="font-heading text-lg font-bold text-foreground">
            {editing ? "Edit backup event" : "Add backup event"}
          </h2>
          <button type="button" aria-label="Close form" onClick={onClose} disabled={saving}
            className="rounded-md p-1.5 text-muted-foreground hover:bg-muted disabled:opacity-50">
            <X size={18} />
          </button>
        </div>
        <form className="grid gap-4 p-5 sm:grid-cols-2" onSubmit={handleSubmit} noValidate>
          <FormField id="generator-event-date" label="Date" error={errors.date}>
            <input id="generator-event-date" className={inputClass} type="date" max={todayAsLocalDate()}
              value={values.date} onChange={(eventChange) => updateField("date", eventChange.target.value)} />
          </FormField>
          <FormField id="generator-event-reason" label="Trigger reason" error={errors.triggerReason}>
            <input id="generator-event-reason" className={inputClass} type="text" maxLength={500}
              value={values.triggerReason}
              onChange={(eventChange) => updateField("triggerReason", eventChange.target.value)} />
          </FormField>
          <FormField id="generator-event-duration" label="Duration (hours)" error={errors.durationHours}>
            <input id="generator-event-duration" className={inputClass} type="number" min="0" step="any"
              value={values.durationHours}
              onChange={(eventChange) => updateField("durationHours", eventChange.target.value)} />
          </FormField>
          <FormField id="generator-event-fuel" label="Fuel (liters)" error={errors.fuelLiters}>
            <input id="generator-event-fuel" className={inputClass} type="number" min="0" step="any"
              value={values.fuelLiters}
              onChange={(eventChange) => updateField("fuelLiters", eventChange.target.value)} />
          </FormField>
          <FormField id="generator-event-cost" label="Cost (EUR)" error={errors.costEur}>
            <input id="generator-event-cost" className={inputClass} type="number" min="0" step="any"
              value={values.costEur}
              onChange={(eventChange) => updateField("costEur", eventChange.target.value)} />
          </FormField>
          {error && <p className="text-sm text-destructive sm:col-span-2" role="alert">{error}</p>}
          <div className="flex justify-end gap-2 border-t border-border pt-4 sm:col-span-2">
            <button type="button" onClick={onClose} disabled={saving}
              className="rounded-md border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-muted disabled:opacity-50">
              Cancel
            </button>
            <button type="submit" disabled={saving}
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50">
              {saving ? "Saving..." : editing ? "Save changes" : "Add event"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
