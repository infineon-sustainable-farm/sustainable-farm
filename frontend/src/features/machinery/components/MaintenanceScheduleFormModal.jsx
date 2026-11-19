import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal, Select } from "../../../shared/ui";
import { MAINTENANCE_TYPE } from "../constants";
import { useEnumLabels } from "../format";
import PersonSelect from "../../../app/PersonSelect";
import EquipmentField from "./EquipmentField";
import FormActions from "./FormActions";
import FormAlert from "./FormAlert";

const MAX_FREQUENCY_LENGTH = 100;

const initialForm = {
  equipmentId: "",
  type: MAINTENANCE_TYPE.PREVENTIVE,
  frequency: "",
  lastCompleted: "",
  nextDue: "",
  operatorPersonId: "",
};

function toFormState(schedule) {
  if (!schedule) return initialForm;

  return {
    equipmentId: schedule.equipmentId != null ? String(schedule.equipmentId) : "",
    type: schedule.type ?? initialForm.type,
    frequency: schedule.frequency ?? "",
    lastCompleted: schedule.lastCompleted ?? "",
    nextDue: schedule.nextDue ?? "",
    operatorPersonId: schedule.operatorPersonId != null ? String(schedule.operatorPersonId) : "",
  };
}

function MaintenanceScheduleFormModal({
  isOpen,
  schedule,
  equipments,
  isEquipmentLoading,
  onClose,
  onSubmit,
  serverError,
}) {
  const { t } = useTranslation("machinery");
  const enumLabels = useEnumLabels();
  const [form, setForm] = useState(() => toFormState(schedule));
  const [fieldErrors, setFieldErrors] = useState({});

  const isEditing = Boolean(schedule);
  const canLeaveEquipmentEmpty = !isEditing || !schedule?.equipmentId;
  const hasEquipment = equipments.length > 0;

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  function validate() {
    const errors = {};

    if (!form.equipmentId && !isEditing) errors.equipmentId = t("validation.equipmentRequired");
    if (!form.lastCompleted) errors.lastCompleted = t("maintenance.lastCompletedRequired");
    if (!form.nextDue) errors.nextDue = t("maintenance.nextDueRequired");
    else if (form.lastCompleted && form.nextDue < form.lastCompleted) {
      errors.nextDue = t("maintenance.nextDueBeforeLast");
    }
    if (form.frequency.trim().length > MAX_FREQUENCY_LENGTH) {
      errors.frequency = t("maintenance.frequencyTooLong", { max: MAX_FREQUENCY_LENGTH });
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
      type: form.type,
      frequency: form.frequency.trim() || null,
      lastCompleted: form.lastCompleted,
      nextDue: form.nextDue,
      operatorPersonId: form.operatorPersonId ? Number(form.operatorPersonId) : null,
    });
  }

  function handleClose() {
    setForm(toFormState(schedule));
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal open={isOpen} onClose={handleClose} title={isEditing ? t("maintenance.edit") : t("maintenance.add")}>
      {serverError && <FormAlert>{serverError}</FormAlert>}

      {!hasEquipment && !isEquipmentLoading && <FormAlert tone="warning">{t("maintenance.noEquipment")}</FormAlert>}

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
          <Field className="flex-1" label={t("fields.type")}>
            <Select name="type" value={form.type} onChange={handleChange}>
              {Object.values(MAINTENANCE_TYPE).map((value) => (
                <option key={value} value={value}>{enumLabels.maintenanceType(value)}</option>
              ))}
            </Select>
          </Field>

          <Field className="flex-1" label={t("maintenance.frequency")} error={fieldErrors.frequency}>
            <Input
              name="frequency"
              value={form.frequency}
              onChange={handleChange}
              placeholder={t("maintenance.frequencyPlaceholder")}
            />
          </Field>
        </div>

        <div className="flex gap-3">
          <Field className="flex-1" label={t("maintenance.lastCompleted")} error={fieldErrors.lastCompleted}>
            <Input type="date" name="lastCompleted" value={form.lastCompleted} onChange={handleChange} />
          </Field>

          <Field className="flex-1" label={t("maintenance.nextDueDate")} error={fieldErrors.nextDue}>
            <Input type="date" name="nextDue" value={form.nextDue} onChange={handleChange} />
          </Field>
        </div>

        <PersonSelect
          label={t("fields.operator")}
          value={form.operatorPersonId}
          currentName={schedule?.operator}
          onChange={(id) => setForm((previous) => ({ ...previous, operatorPersonId: id }))}
          placeholder={t("common.noPerson")}
          error={fieldErrors.operatorPersonId}
        />

        <FormActions onCancel={handleClose} />
      </form>
    </Modal>
  );
}
export default MaintenanceScheduleFormModal;
