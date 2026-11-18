import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal, Select } from "../../../shared/ui";
import { COMPONENT_CATEGORIES, COMPONENT_STATUSES, nextComponentId } from "../constants";
import { useEnumLabels } from "../format";
import FormActions from "./FormActions";
import InlineAlert from "./InlineAlert";

export default function ComponentFormModal({ component, usedIds, saving, error, onClose, onSave }) {
  const { t } = useTranslation("energysupply");
  const labels = useEnumLabels();
  const [values, setValues] = useState({
    componentId: component?.componentId || nextComponentId(usedIds.map((componentId) => ({ componentId }))),
    name: component?.name || "",
    category: component?.category || "PV_ARRAY",
    capacityValue: component?.capacityValue ?? "",
    status: component?.status || "OPERATIONAL",
  });
  const [errors, setErrors] = useState({});

  async function submit(event) {
    event.preventDefault();
    const next = {};
    if (!values.componentId) next.componentId = t("components.idLimit");
    if (!values.name.trim()) next.name = t("validation.nameRequired");
    if (values.capacityValue !== "" && (!Number.isFinite(Number(values.capacityValue)) || Number(values.capacityValue) < 0)) {
      next.capacityValue = t("validation.capacityNonNegative");
    }
    if (!component && usedIds.includes(values.componentId)) next.componentId = t("components.idInUse");
    setErrors(next);
    if (Object.keys(next).length) return;

    const saved = await onSave({
      ...values,
      capacityValue: values.capacityValue === "" ? null : Number(values.capacityValue),
    });
    if (saved) onClose();
  }

  return (
    <Modal
      open
      size="lg"
      onClose={onClose}
      dismissible={!saving}
      title={component ? t("components.edit") : t("components.add")}
    >
      <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <Field id="component-componentId" label={t("components.registryId")} error={errors.componentId}>
          <Input value={values.componentId} readOnly aria-readonly="true" />
        </Field>
        <Field id="component-name" label={t("fields.name")} error={errors.name}>
          <Input maxLength={255} value={values.name} onChange={(e) => setValues({ ...values, name: e.target.value })} />
        </Field>
        <Field id="component-category" label={t("fields.category")}>
          <Select value={values.category} onChange={(e) => setValues({ ...values, category: e.target.value })}>
            {COMPONENT_CATEGORIES.map((category) => (
              <option key={category} value={category}>{labels.category(category)}</option>
            ))}
          </Select>
        </Field>
        <Field id="component-capacityValue" label={t("fields.capacity")} error={errors.capacityValue}>
          <Input
            type="number"
            min="0"
            step="any"
            value={values.capacityValue}
            onChange={(e) => setValues({ ...values, capacityValue: e.target.value })}
          />
        </Field>
        <Field id="component-status" label={t("fields.status")}>
          <Select value={values.status} onChange={(e) => setValues({ ...values, status: e.target.value })}>
            {COMPONENT_STATUSES.map((status) => (
              <option key={status} value={status}>{labels.status(status)}</option>
            ))}
          </Select>
        </Field>
        {error && <InlineAlert className="sm:col-span-2">{error}</InlineAlert>}
        <FormActions
          className="sm:col-span-2"
          onCancel={onClose}
          saving={saving}
          submitLabel={component ? t("form.saveChanges") : t("components.add")}
        />
      </form>
    </Modal>
  );
}
