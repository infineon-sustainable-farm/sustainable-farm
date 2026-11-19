import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal, Textarea } from "../../../shared/ui";
import { NON_NEGATIVE_DECIMAL } from "../../../shared/utils/validation";
import { todayIsoDate } from "../../../shared/utils/todayIsoDate";
import PersonSelect from "../../../app/PersonSelect";
import EquipmentField from "./EquipmentField";
import FormActions from "./FormActions";
import FormAlert from "./FormAlert";

const MAX_ISSUE_LENGTH = 500;

const initialForm = {
  equipmentId: "",
  date: "",
  issue: "",
  downtime: "",
  cost: "",
  technicianPersonId: "",
};

function toFormState(repairLog) {
  if (!repairLog) {
    return { ...initialForm, date: todayIsoDate() };
  }

  return {
    equipmentId: repairLog.equipmentId != null ? String(repairLog.equipmentId) : "",
    date: repairLog.date ?? "",
    issue: repairLog.issue ?? "",
    downtime: repairLog.downtime != null ? String(repairLog.downtime) : "",
    cost: repairLog.cost != null ? String(repairLog.cost) : "",
    technicianPersonId: repairLog.technicianPersonId != null ? String(repairLog.technicianPersonId) : "",
  };
}

function RepairLogFormModal({
  isOpen,
  repairLog,
  equipments,
  isEquipmentLoading,
  onClose,
  onSubmit,
  serverError,
}) {
  const { t } = useTranslation("machinery");
  const [form, setForm] = useState(() => toFormState(repairLog));
  const [fieldErrors, setFieldErrors] = useState({});

  const isEditing = Boolean(repairLog);
  const canLeaveEquipmentEmpty = !isEditing || !repairLog?.equipmentId;
  const hasEquipment = equipments.length > 0;

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  function validate() {
    const errors = {};
    const issue = form.issue.trim();

    if (!form.equipmentId && !isEditing) errors.equipmentId = t("validation.equipmentRequired");
    if (!form.date) errors.date = t("validation.dateRequired");

    if (!issue) errors.issue = t("repair.issueRequired");
    else if (issue.length > MAX_ISSUE_LENGTH) {
      errors.issue = t("repair.issueTooLong", { max: MAX_ISSUE_LENGTH });
    }

    if (form.downtime.trim() && !NON_NEGATIVE_DECIMAL.test(form.downtime.trim())) {
      errors.downtime = t("repair.downtimeInvalid");
    }
    if (form.cost.trim() && !NON_NEGATIVE_DECIMAL.test(form.cost.trim())) {
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
      issue: form.issue.trim(),
      downtime: form.downtime.trim() || null,
      cost: form.cost.trim() || null,
      technicianPersonId: form.technicianPersonId ? Number(form.technicianPersonId) : null,
    });
  }

  function handleClose() {
    setForm(toFormState(repairLog));
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal open={isOpen} onClose={handleClose} title={isEditing ? t("repair.edit") : t("repair.add")}>
      {serverError && <FormAlert>{serverError}</FormAlert>}

      {!hasEquipment && !isEquipmentLoading && <FormAlert tone="warning">{t("repair.noEquipment")}</FormAlert>}

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

          <PersonSelect
            className="flex-1"
            label={t("repair.technician")}
            value={form.technicianPersonId}
            currentName={repairLog?.technician}
            onChange={(id) => setForm((previous) => ({ ...previous, technicianPersonId: id }))}
            placeholder={t("common.noPerson")}
            error={fieldErrors.technicianPersonId}
          />
        </div>

        <Field label={t("repair.issueDescription")} error={fieldErrors.issue}>
          <Textarea name="issue" value={form.issue} onChange={handleChange} />
        </Field>

        <div className="flex gap-3">
          <Field className="flex-1" label={t("repair.downtimeHours")} error={fieldErrors.downtime}>
            <Input type="number" name="downtime" min="0" step="0.1" value={form.downtime} onChange={handleChange} />
          </Field>

          <Field className="flex-1" label={t("fields.cost")} error={fieldErrors.cost}>
            <Input type="number" name="cost" min="0" step="0.01" value={form.cost} onChange={handleChange} />
          </Field>
        </div>

        <FormActions onCancel={handleClose} />
      </form>
    </Modal>
  );
}
export default RepairLogFormModal;
