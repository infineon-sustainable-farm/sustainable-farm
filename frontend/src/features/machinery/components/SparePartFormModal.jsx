import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal } from "../../../shared/ui";
import { NON_NEGATIVE_DECIMAL, NON_NEGATIVE_INTEGER } from "../../../shared/utils/validation";
import EquipmentField from "./EquipmentField";
import FormActions from "./FormActions";
import FormAlert from "./FormAlert";

const MAX_NAME_LENGTH = 255;
const MAX_UNIT_COST = 100000000;

const initialForm = {
  name: "",
  quantity: "",
  reorderThreshold: "",
  unitCost: "",
  equipmentId: "",
};

function toFormState(sparePart) {
  if (!sparePart) return initialForm;

  return {
    name: sparePart.name ?? "",
    quantity: sparePart.quantity != null ? String(sparePart.quantity) : "",
    reorderThreshold: sparePart.reorderThreshold != null ? String(sparePart.reorderThreshold) : "",
    unitCost: sparePart.unitCost != null ? String(sparePart.unitCost) : "",
    equipmentId: sparePart.equipmentId != null ? String(sparePart.equipmentId) : "",
  };
}

function SparePartFormModal({
  isOpen,
  sparePart,
  equipments,
  isEquipmentLoading,
  onClose,
  onSubmit,
  serverError,
}) {
  const { t } = useTranslation("machinery");
  const [form, setForm] = useState(() => toFormState(sparePart));
  const [fieldErrors, setFieldErrors] = useState({});

  const isEditing = Boolean(sparePart);
  const canUnassignEquipment = !isEditing || !sparePart?.equipmentId;

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  function validate() {
    const errors = {};
    const name = form.name.trim();

    if (!name) errors.name = t("validation.nameRequired");
    else if (name.length > MAX_NAME_LENGTH) errors.name = t("validation.nameTooLong", { max: MAX_NAME_LENGTH });

    if (!form.quantity.trim()) errors.quantity = t("spareParts.quantityRequired");
    else if (!NON_NEGATIVE_INTEGER.test(form.quantity.trim())) errors.quantity = t("spareParts.quantityInvalid");

    if (!form.reorderThreshold.trim()) errors.reorderThreshold = t("spareParts.thresholdRequired");
    else if (!NON_NEGATIVE_INTEGER.test(form.reorderThreshold.trim())) {
      errors.reorderThreshold = t("spareParts.thresholdInvalid");
    }

    if (!form.unitCost.trim()) errors.unitCost = t("spareParts.unitCostRequired");
    else if (!NON_NEGATIVE_DECIMAL.test(form.unitCost.trim())) errors.unitCost = t("spareParts.unitCostInvalid");
    else if (Number(form.unitCost) >= MAX_UNIT_COST) errors.unitCost = t("spareParts.unitCostTooLarge");

    return errors;
  }

  function handleSubmit(e) {
    e.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    onSubmit({
      name: form.name.trim(),
      quantity: Number(form.quantity),
      reorderThreshold: Number(form.reorderThreshold),
      unitCost: form.unitCost.trim(),
      equipmentId: form.equipmentId ? Number(form.equipmentId) : null,
    });
  }

  function handleClose() {
    setForm(toFormState(sparePart));
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal open={isOpen} onClose={handleClose} title={isEditing ? t("spareParts.edit") : t("spareParts.add")}>
      {serverError && <FormAlert>{serverError}</FormAlert>}

      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <Field label={t("fields.name")} error={fieldErrors.name}>
          <Input name="name" value={form.name} onChange={handleChange} />
        </Field>

        <EquipmentField
          label={t("spareParts.compatibleAsset")}
          value={form.equipmentId}
          onChange={handleChange}
          error={fieldErrors.equipmentId}
          equipments={equipments}
          isEquipmentLoading={isEquipmentLoading}
          emptyOptionLabel={canUnassignEquipment ? t("spareParts.noCompatibleAsset") : null}
        />

        <div className="flex gap-3">
          <Field className="flex-1" label={t("spareParts.quantity")} error={fieldErrors.quantity}>
            <Input type="number" name="quantity" min="0" step="1" value={form.quantity} onChange={handleChange} />
          </Field>

          <Field className="flex-1" label={t("spareParts.reorderThreshold")} error={fieldErrors.reorderThreshold}>
            <Input
              type="number"
              name="reorderThreshold"
              min="0"
              step="1"
              value={form.reorderThreshold}
              onChange={handleChange}
            />
          </Field>
        </div>

        <Field label={t("spareParts.unitCost")} error={fieldErrors.unitCost}>
          <Input type="number" name="unitCost" min="0" step="0.01" value={form.unitCost} onChange={handleChange} />
        </Field>

        <FormActions onCancel={handleClose} />
      </form>
    </Modal>
  );
}
export default SparePartFormModal;
