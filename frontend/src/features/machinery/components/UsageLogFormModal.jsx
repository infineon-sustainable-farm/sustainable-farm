import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal, Textarea } from "../../../shared/ui";
import { NON_NEGATIVE_DECIMAL } from "../../../shared/utils/validation";
import { todayIsoDate } from "../../../shared/utils/todayIsoDate";
import PersonSelect from "../../../app/PersonSelect";
import EquipmentField from "./EquipmentField";
import FormActions from "./FormActions";
import FormAlert from "./FormAlert";

const MAX_NOTES_LENGTH = 500;

const initialForm = {
  equipmentId: "",
  date: "",
  hoursUsed: "",
  operatorPersonId: "",
  notes: "",
};

function toFormState(usageLog) {
  if (!usageLog) {
    return { ...initialForm, date: todayIsoDate() };
  }

  return {
    equipmentId: usageLog.equipmentId != null ? String(usageLog.equipmentId) : "",
    date: usageLog.date ?? "",
    hoursUsed: usageLog.hoursUsed != null ? String(usageLog.hoursUsed) : "",
    operatorPersonId: usageLog.operatorPersonId != null ? String(usageLog.operatorPersonId) : "",
    notes: usageLog.notes ?? "",
  };
}

function UsageLogFormModal({
  isOpen,
  usageLog,
  equipments,
  isEquipmentLoading,
  onClose,
  onSubmit,
  serverError,
}) {
  const { t } = useTranslation("machinery");
  const [form, setForm] = useState(() => toFormState(usageLog));
  const [fieldErrors, setFieldErrors] = useState({});

  const isEditing = Boolean(usageLog);
  const canLeaveEquipmentEmpty = !isEditing || !usageLog?.equipmentId;
  const hasEquipment = equipments.length > 0;

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  function validate() {
    const errors = {};

    if (!form.equipmentId && !isEditing) errors.equipmentId = t("validation.equipmentRequired");
    if (!form.date) errors.date = t("validation.dateRequired");

    if (!form.hoursUsed.trim()) errors.hoursUsed = t("usage.hoursRequired");
    else if (!NON_NEGATIVE_DECIMAL.test(form.hoursUsed.trim())) {
      errors.hoursUsed = t("usage.hoursInvalid");
    }

    if (form.notes.trim().length > MAX_NOTES_LENGTH) {
      errors.notes = t("usage.notesTooLong", { max: MAX_NOTES_LENGTH });
    }

    return errors;
  }

  function handleSubmit(e) {
    e.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    onSubmit({
      equipmentId: form.equipmentId ? Number(form.equipmentId) : null,
      date: form.date,
      hoursUsed: form.hoursUsed.trim(),
      operatorPersonId: form.operatorPersonId ? Number(form.operatorPersonId) : null,
      notes: form.notes.trim() || null,
    });
  }

  function handleClose() {
    setForm(toFormState(usageLog));
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal open={isOpen} onClose={handleClose} title={isEditing ? t("usage.edit") : t("usage.add")}>
      {serverError && <FormAlert>{serverError}</FormAlert>}

      {!hasEquipment && !isEquipmentLoading && <FormAlert tone="warning">{t("usage.noEquipment")}</FormAlert>}

      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <EquipmentField
          value={form.equipmentId}
          onChange={handleChange}
          error={fieldErrors.equipmentId}
          equipments={equipments}
          isEquipmentLoading={isEquipmentLoading}
          emptyOptionLabel={canLeaveEquipmentEmpty ? t("common.noEquipmentDetached") : null}
        />

        <div className="flex gap-3">
          <Field className="flex-1" label={t("fields.date")} error={fieldErrors.date}>
            <Input type="date" name="date" value={form.date} onChange={handleChange} />
          </Field>

          <Field className="flex-1" label={t("usage.hoursUsed")} error={fieldErrors.hoursUsed}>
            <Input type="number" name="hoursUsed" min="0" step="0.1" value={form.hoursUsed} onChange={handleChange} />
          </Field>
        </div>

        <PersonSelect
          label={t("fields.operator")}
          value={form.operatorPersonId}
          currentName={usageLog?.operator}
          onChange={(id) => setForm((previous) => ({ ...previous, operatorPersonId: id }))}
          placeholder={t("common.noPerson")}
          error={fieldErrors.operatorPersonId}
        />

        <Field label={t("fields.notes")} error={fieldErrors.notes}>
          <Textarea name="notes" value={form.notes} onChange={handleChange} />
        </Field>

        <FormActions onCancel={handleClose} />
      </form>
    </Modal>
  );
}
export default UsageLogFormModal;
