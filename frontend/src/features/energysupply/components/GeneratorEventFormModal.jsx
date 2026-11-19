import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal } from "../../../shared/ui";
import { todayAsLocalDate } from "../format";
import FormActions from "./FormActions";
import InlineAlert from "./InlineAlert";

const MAX_REASON_LENGTH = 500;

function initialValues(event) {
  return {
    date: event?.date || todayAsLocalDate(),
    triggerReason: event?.triggerReason || "",
    durationHours: event?.durationHours ?? "",
    fuelLiters: event?.fuelLiters ?? "",
    costEur: event?.costEur ?? "",
  };
}

function validate(values, t) {
  const errors = {};
  if (!values.date) {
    errors.date = t("validation.dateRequired");
  } else if (values.date > todayAsLocalDate()) {
    errors.date = t("generator.dateNotFuture");
  }

  for (const field of ["durationHours", "fuelLiters", "costEur"]) {
    if (values[field] !== "" && (!Number.isFinite(Number(values[field])) || Number(values[field]) < 0)) {
      errors[field] = t("generator.valueNonNegative");
    }
  }

  if (values.triggerReason.length > MAX_REASON_LENGTH) {
    errors.triggerReason = t("generator.reasonTooLong", { max: MAX_REASON_LENGTH });
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

export default function GeneratorEventFormModal({ event, saving, error, onClose, onSave }) {
  const { t } = useTranslation("energysupply");
  const [values, setValues] = useState(() => initialValues(event));
  const [errors, setErrors] = useState({});
  const editing = Boolean(event);

  function updateField(field, value) {
    setValues((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(submitEvent) {
    submitEvent.preventDefault();
    const nextErrors = validate(values, t);
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
    <Modal
      open
      size="lg"
      onClose={onClose}
      dismissible={!saving}
      closeLabel={t("generator.closeForm")}
      title={editing ? t("generator.editEvent") : t("generator.addEvent")}
    >
      <form className="grid gap-4 sm:grid-cols-2" onSubmit={handleSubmit} noValidate>
        <Field id="generator-event-date" label={t("fields.date")} error={errors.date}>
          <Input
            type="date"
            max={todayAsLocalDate()}
            value={values.date}
            onChange={(eventChange) => updateField("date", eventChange.target.value)}
          />
        </Field>
        <Field id="generator-event-reason" label={t("generator.triggerReasonLabel")} error={errors.triggerReason}>
          <Input
            type="text"
            maxLength={MAX_REASON_LENGTH}
            value={values.triggerReason}
            onChange={(eventChange) => updateField("triggerReason", eventChange.target.value)}
          />
        </Field>
        <Field id="generator-event-duration" label={t("generator.durationHours")} error={errors.durationHours}>
          <Input
            type="number"
            min="0"
            step="any"
            value={values.durationHours}
            onChange={(eventChange) => updateField("durationHours", eventChange.target.value)}
          />
        </Field>
        <Field id="generator-event-fuel" label={t("generator.fuelLiters")} error={errors.fuelLiters}>
          <Input
            type="number"
            min="0"
            step="any"
            value={values.fuelLiters}
            onChange={(eventChange) => updateField("fuelLiters", eventChange.target.value)}
          />
        </Field>
        <Field id="generator-event-cost" label={t("generator.costEur")} error={errors.costEur}>
          <Input
            type="number"
            min="0"
            step="any"
            value={values.costEur}
            onChange={(eventChange) => updateField("costEur", eventChange.target.value)}
          />
        </Field>
        {error && <InlineAlert className="sm:col-span-2">{error}</InlineAlert>}
        <FormActions
          className="sm:col-span-2"
          onCancel={onClose}
          saving={saving}
          submitLabel={editing ? t("form.saveChanges") : t("generator.addEventButton")}
        />
      </form>
    </Modal>
  );
}
