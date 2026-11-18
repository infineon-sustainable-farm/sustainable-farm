import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal, Select } from "../../../shared/ui";
import { todayAsLocalDate } from "../format";
import FormActions from "./FormActions";
import InlineAlert from "./InlineAlert";

export default function SolarLogFormModal({ log, arrays, saving, error, onClose, onSave }) {
  const { t } = useTranslation("energysupply");
  const [values, setValues] = useState({
    componentId: log?.componentId || arrays[0]?.componentId || "",
    date: log?.date || todayAsLocalDate(),
    kwhProduced: log?.kwhProduced ?? "",
    efficiencyPct: log?.efficiencyPct ?? "",
  });
  const [errors, setErrors] = useState({});

  async function submit(event) {
    event.preventDefault();
    const next = {};
    if (!values.componentId) next.componentId = t("solar.componentRequired");
    if (!values.date) next.date = t("validation.dateRequired");
    else if (values.date > todayAsLocalDate()) next.date = t("validation.dateNotFuture");
    for (const field of ["kwhProduced", "efficiencyPct"]) {
      if (field === "efficiencyPct" && values[field] === "") continue;
      if (values[field] === "" || !Number.isFinite(Number(values[field])) || Number(values[field]) < 0) {
        next[field] = t("validation.nonNegativeValue");
      }
    }
    setErrors(next);
    if (Object.keys(next).length) return;
    const saved = await onSave({
      ...values,
      kwhProduced: Number(values.kwhProduced),
      efficiencyPct: values.efficiencyPct === "" ? null : Number(values.efficiencyPct),
    });
    if (saved) onClose();
  }

  return (
    <Modal open size="lg" onClose={onClose} dismissible={!saving} title={log ? t("solar.editLog") : t("solar.addLog")}>
      <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <Field className="sm:col-span-2" label={t("solar.pvComponent")} error={errors.componentId}>
          <Select value={values.componentId} onChange={(e) => setValues({ ...values, componentId: e.target.value })}>
            {arrays.map((array) => (
              <option key={array.componentId} value={array.componentId}>
                {t("solar.componentOption", { id: array.componentId, name: array.name })}
              </option>
            ))}
          </Select>
        </Field>
        <Field label={t("fields.date")} error={errors.date}>
          <Input
            type="date"
            max={todayAsLocalDate()}
            value={values.date}
            onChange={(e) => setValues({ ...values, date: e.target.value })}
          />
        </Field>
        <Field label={t("solar.energyGenerated")} error={errors.kwhProduced}>
          <Input
            type="number"
            min="0"
            step="any"
            value={values.kwhProduced}
            onChange={(e) => setValues({ ...values, kwhProduced: e.target.value })}
          />
        </Field>
        <Field className="sm:col-span-2" label={t("solar.efficiency")} error={errors.efficiencyPct}>
          <Input
            type="number"
            min="0"
            max="100"
            step="any"
            value={values.efficiencyPct}
            onChange={(e) => setValues({ ...values, efficiencyPct: e.target.value })}
          />
        </Field>
        {error && <InlineAlert className="sm:col-span-2">{error}</InlineAlert>}
        <FormActions className="sm:col-span-2" onCancel={onClose} saving={saving} submitLabel={t("form.save")} />
      </form>
    </Modal>
  );
}
