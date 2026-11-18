import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal, Select } from "../../../shared/ui";
import { ENERGY_SOURCES, LOAD_CATEGORIES } from "../constants";
import { todayAsLocalDate, useEnumLabels } from "../format";
import FormActions from "./FormActions";
import InlineAlert from "./InlineAlert";

export default function ConsumptionFormModal({ log, saving, error, onClose, onSave }) {
  const { t } = useTranslation("energysupply");
  const labels = useEnumLabels();
  const [values, setValues] = useState({
    date: log?.date || todayAsLocalDate(),
    loadCategory: log?.loadCategory || "IRRIGATION",
    kwhConsumed: log?.kwhConsumed ?? "",
    source: log?.source || "",
  });
  const [errors, setErrors] = useState({});

  async function submit(event) {
    event.preventDefault();
    const next = {};
    if (!values.date) next.date = t("validation.dateRequired");
    if (values.date > todayAsLocalDate()) next.date = t("validation.dateNotFuture");
    if (values.kwhConsumed === "" || !Number.isFinite(Number(values.kwhConsumed)) || Number(values.kwhConsumed) < 0) {
      next.kwhConsumed = t("validation.nonNegativeKwh");
    }
    setErrors(next);
    if (Object.keys(next).length) return;
    const saved = await onSave({
      ...values,
      kwhConsumed: Number(values.kwhConsumed),
      source: values.source || null,
    });
    if (saved) onClose();
  }

  return (
    <Modal
      open
      size="lg"
      onClose={onClose}
      dismissible={!saving}
      title={log ? t("consumption.editLog") : t("consumption.addLog")}
    >
      <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <Field label={t("fields.date")} error={errors.date}>
          <Input
            type="date"
            max={todayAsLocalDate()}
            value={values.date}
            onChange={(e) => setValues({ ...values, date: e.target.value })}
          />
        </Field>
        <Field label={t("consumption.loadCategory")}>
          <Select value={values.loadCategory} onChange={(e) => setValues({ ...values, loadCategory: e.target.value })}>
            {LOAD_CATEGORIES.map((item) => (
              <option key={item} value={item}>{labels.loadCategory(item)}</option>
            ))}
          </Select>
        </Field>
        <Field label={t("consumption.energy")} error={errors.kwhConsumed}>
          <Input
            type="number"
            min="0"
            step="any"
            value={values.kwhConsumed}
            onChange={(e) => setValues({ ...values, kwhConsumed: e.target.value })}
          />
        </Field>
        <Field label={t("fields.source")}>
          <Select value={values.source} onChange={(e) => setValues({ ...values, source: e.target.value })}>
            <option value="">{t("consumption.unspecified")}</option>
            {ENERGY_SOURCES.map((item) => (
              <option key={item} value={item}>{labels.source(item)}</option>
            ))}
          </Select>
        </Field>
        {error && <InlineAlert className="sm:col-span-2">{error}</InlineAlert>}
        <FormActions className="sm:col-span-2" onCancel={onClose} saving={saving} submitLabel={t("form.save")} />
      </form>
    </Modal>
  );
}
