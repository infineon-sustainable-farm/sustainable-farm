import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal } from "../../../shared/ui";
import PersonSelect from "../../../app/PersonSelect";
import EquipmentField from "./EquipmentField";
import FormActions from "./FormActions";
import FormAlert from "./FormAlert";

const MAX_TEXT_LENGTH = 60;

function toFormState(assignment) {
  return {
    personId: assignment?.personId != null ? String(assignment.personId) : "",
    jobTitle: assignment?.jobTitle ?? "",
    equipmentId: assignment?.equipmentId != null ? String(assignment.equipmentId) : "",
    startDate: assignment?.startDate ?? "",
    endDate: assignment?.endDate ?? "",
  };
}

function OperatorAssignmentFormModal({
  isOpen,
  assignment,
  equipments,
  isEquipmentLoading,
  onClose,
  onSubmit,
  serverError,
}) {
  const { t } = useTranslation("machinery");
  const [form, setForm] = useState(() => toFormState(assignment));
  const [fieldErrors, setFieldErrors] = useState({});

  const isEditing = Boolean(assignment);
  const hasEquipment = equipments.length > 0;

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  function validate() {
    const errors = {};
    const jobTitle = form.jobTitle.trim();

    if (!form.personId) errors.personId = t("operators.personRequired");

    if (!jobTitle) errors.jobTitle = t("operators.jobTitleRequired");
    else if (jobTitle.length > MAX_TEXT_LENGTH) errors.jobTitle = t("operators.jobTitleTooLong", { max: MAX_TEXT_LENGTH });

    if (!form.equipmentId) errors.equipmentId = t("validation.equipmentRequired");
    if (!form.startDate) errors.startDate = t("operators.startDateRequired");
    if (form.endDate && form.startDate && form.endDate < form.startDate) {
      errors.endDate = t("operators.endDateBeforeStart");
    }

    return errors;
  }

  function handleSubmit(e) {
    e.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    onSubmit({
      personId: Number(form.personId),
      jobTitle: form.jobTitle.trim(),
      equipmentId: Number(form.equipmentId),
      startDate: form.startDate,
      endDate: form.endDate || null,
    });
  }

  function handleClose() {
    setForm(toFormState(assignment));
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal open={isOpen} onClose={handleClose} title={isEditing ? t("operators.editTitle") : t("operators.add")}>
      {serverError && <FormAlert>{serverError}</FormAlert>}

      {!hasEquipment && !isEquipmentLoading && <FormAlert tone="warning">{t("operators.noEquipment")}</FormAlert>}

      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <PersonSelect
          label={t("operators.person")}
          value={form.personId}
          currentName={assignment?.fullName}
          onChange={(id) => setForm((previous) => ({ ...previous, personId: id }))}
          error={fieldErrors.personId}
          required
        />

        <Field label={t("operators.jobTitle")} error={fieldErrors.jobTitle}>
          <Input name="jobTitle" value={form.jobTitle} onChange={handleChange} />
        </Field>

        <EquipmentField
          value={form.equipmentId}
          onChange={handleChange}
          error={fieldErrors.equipmentId}
          equipments={equipments}
          isEquipmentLoading={isEquipmentLoading}
          emptyOptionLabel={t("operators.selectEquipment")}
        />

        <div className="flex gap-3">
          <Field className="flex-1" label={t("operators.startDate")} error={fieldErrors.startDate}>
            <Input type="date" name="startDate" value={form.startDate} onChange={handleChange} />
          </Field>

          <Field className="flex-1" label={t("operators.endDateOptional")} error={fieldErrors.endDate}>
            <Input type="date" name="endDate" value={form.endDate} onChange={handleChange} />
          </Field>
        </div>

        <FormActions onCancel={handleClose} submitDisabled={!hasEquipment} />
      </form>
    </Modal>
  );
}
export default OperatorAssignmentFormModal;
