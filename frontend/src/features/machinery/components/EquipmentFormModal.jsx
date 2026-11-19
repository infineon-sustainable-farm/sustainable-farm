import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal, Select } from "../../../shared/ui";
import { CATEGORY, STAGE, STATUS } from "../constants";
import { useEnumLabels } from "../format";
import FormActions from "./FormActions";
import FormAlert from "./FormAlert";

const MAX_NAME_LENGTH = 100;

const initialForm = {
  name: "",
  category: CATEGORY.AGRICULTURAL_MACHINERY,
  stage: STAGE.CULTIVATION,
  status: STATUS.OPERATIONAL,
};

function EquipmentFormModal({ isOpen, onClose, onSubmit, serverError }) {
  const { t } = useTranslation("machinery");
  const enumLabels = useEnumLabels();
  const [form, setForm] = useState(initialForm);
  const [fieldErrors, setFieldErrors] = useState({});

  function handleChange(e) {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
  }

  function validate() {
    const errors = {};
    if (!form.name.trim()) errors.name = t("validation.nameRequired");
    else if (form.name.length > MAX_NAME_LENGTH) errors.name = t("validation.nameTooLong", { max: MAX_NAME_LENGTH });
    return errors;
  }

  function handleSubmit(e) {
    e.preventDefault();
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;
    onSubmit(form);
  }

  function handleClose() {
    setForm(initialForm);
    setFieldErrors({});
    onClose();
  }

  return (
    <Modal open={isOpen} onClose={handleClose} title={t("equipment.add")}>
      {serverError && <FormAlert>{serverError}</FormAlert>}

      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <Field label={t("fields.name")} error={fieldErrors.name}>
          <Input name="name" value={form.name} onChange={handleChange} />
        </Field>

        <Field label={t("equipment.category")}>
          <Select name="category" value={form.category} onChange={handleChange}>
            {Object.values(CATEGORY).map((value) => (
              <option key={value} value={value}>{enumLabels.category(value)}</option>
            ))}
          </Select>
        </Field>

        <Field label={t("equipment.stage")}>
          <Select name="stage" value={form.stage} onChange={handleChange}>
            {Object.values(STAGE).map((value) => (
              <option key={value} value={value}>{enumLabels.stage(value)}</option>
            ))}
          </Select>
        </Field>

        <FormActions onCancel={handleClose} />
      </form>
    </Modal>
  );
}
export default EquipmentFormModal;
