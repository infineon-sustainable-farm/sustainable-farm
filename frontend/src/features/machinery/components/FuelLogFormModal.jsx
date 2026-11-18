import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal } from "../../../shared/ui";
import { NON_NEGATIVE_DECIMAL } from "../../../shared/utils/validation";
import { todayIsoDate } from "../../../shared/utils/todayIsoDate";
import EquipmentField from "./EquipmentField";
import FormActions from "./FormActions";
import FormAlert from "./FormAlert";

const initialForm = {
  equipmentId: "",
  date: "",
  liters: "",
  cost: "",
};

function toFormState(fuelLog) {
  if (!fuelLog) {
    return { ...initialForm, date: todayIsoDate() };
  }

  return {
    equipmentId: fuelLog.equipmentId != null ? String(fuelLog.equipmentId) : "",
    date: fuelLog.date ?? "",
    liters: fuelLog.liters != null ? String(fuelLog.liters) : "",
    cost: fuelLog.cost != null ? String(fuelLog.cost) : "",
  };
}

function FuelLogFormModal({
  isOpen,
  fuelLog,
  equipments,
  isEquipmentLoading,
  onClose,
  onSubmit,
  serverError,
}) {
  const { t } = useTranslation("machinery");
  const [form, setForm] = useState(() => toFormState(fuelLog));
  const [fieldErrors, setFieldErrors] = useState({});

  const isEditing = Boolean(fuelLog);
  const canLeaveEquipmentEmpty = !isEditing || !fuelLog?.equipmentId;
  const hasEquipment = equipments.length > 0;

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  function validate() {
    const errors = {};

    if (!form.equipmentId && !isEditing) errors.equipmentId = t("validation.equipmentRequired");
    if (!form.date) errors.date = t("validation.dateRequired");

    if (!form.liters.trim()) errors.liters = t("fuel.litersRequired");
    else if (!NON_NEGATIVE_DECIMAL.test(form.liters.trim())) {
      errors.liters = t("fuel.litersInvalid");
    }

    if (!form.cost.trim()) errors.cost = t("validation.costRequired");
    else if (!NON_NEGATIVE_DECIMAL.test(form.cost.trim())) {
      errors.cost = t("validation.costInvalid");
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
      liters: form.liters.trim(),
      cost: form.cost.trim(),
    });
  }

  function handleClose() {
    setForm(toFormState(fuelLog));
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal open={isOpen} onClose={handleClose} title={isEditing ? t("fuel.edit") : t("fuel.add")}>
      {serverError && <FormAlert>{serverError}</FormAlert>}

      {!hasEquipment && !isEquipmentLoading && <FormAlert tone="warning">{t("fuel.noEquipment")}</FormAlert>}

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

          <Field className="flex-1" label={t("fuel.liters")} error={fieldErrors.liters}>
            <Input type="number" name="liters" min="0" step="0.01" value={form.liters} onChange={handleChange} />
          </Field>
        </div>

        <Field label={t("fields.cost")} error={fieldErrors.cost}>
          <Input type="number" name="cost" min="0" step="0.01" value={form.cost} onChange={handleChange} />
        </Field>

        <FormActions onCancel={handleClose} />
      </form>
    </Modal>
  );
}
export default FuelLogFormModal;
