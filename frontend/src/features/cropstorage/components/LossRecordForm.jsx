import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Card, Field, Input, Select } from "../../../shared/ui";
import { localDateTimeValue } from "../utils/dates";
import FormMessage from "./FormMessage";

function fieldError(errors, name, aliases = []) {
    for (const key of [name, ...aliases]) {
        if (errors?.[key]) return errors[key];
    }
    return undefined;
}

export default function LossRecordForm({ batches, isSubmitting, fieldErrors, submitError, success, onSubmit, onClearFeedback }) {
    const { t } = useTranslation("cropstorage");
    const [values, setValues] = useState({ batchId: "", quantityKg: "", cause: "", valueFcfa: "", recordedAt: localDateTimeValue() });

    function update(name, value) {
        setValues((current) => ({ ...current, [name]: value }));
        onClearFeedback();
    }

    function handleSubmit(event) {
        event.preventDefault();
        onSubmit({
            batch: { id: Number(values.batchId) },
            quantityKg: Number(values.quantityKg),
            cause: values.cause.trim() || null,
            valueFcfa: values.valueFcfa === "" ? null : Number(values.valueFcfa),
            recordedAt: values.recordedAt,
        });
    }

    return (
        <Card title={t("losses.form.title")} titleAs="h3">
            <form onSubmit={handleSubmit}>
                <div className="grid gap-4 sm:grid-cols-2">
                    <Field label={t("losses.form.batch")} required error={fieldError(fieldErrors, "batchId", ["batch", "batch.id"])}>
                        <Select value={values.batchId} onChange={(event) => update("batchId", event.target.value)} required>
                            <option value="">{t("form.choose")}</option>
                            {batches.map((batch) => (
                                <option key={batch.id} value={batch.id}>
                                    {batch.batchCode}
                                </option>
                            ))}
                        </Select>
                    </Field>
                    <Field label={t("losses.form.recordedAt")} required error={fieldError(fieldErrors, "recordedAt")}>
                        <Input type="datetime-local" value={values.recordedAt} onChange={(event) => update("recordedAt", event.target.value)} required />
                    </Field>
                    <Field label={t("losses.form.quantityKg")} required error={fieldError(fieldErrors, "quantityKg")}>
                        <Input
                            type="number"
                            min="0"
                            step="0.01"
                            value={values.quantityKg}
                            onChange={(event) => update("quantityKg", event.target.value)}
                            required
                        />
                    </Field>
                    <Field label={t("losses.form.value")} error={fieldError(fieldErrors, "valueFcfa")}>
                        <Input
                            type="number"
                            min="0"
                            step="0.01"
                            value={values.valueFcfa}
                            onChange={(event) => update("valueFcfa", event.target.value)}
                        />
                    </Field>
                    <Field label={t("losses.form.cause")} error={fieldError(fieldErrors, "cause")} className="sm:col-span-2">
                        <Input value={values.cause} onChange={(event) => update("cause", event.target.value)} />
                    </Field>
                </div>
                <FormMessage error={submitError} success={success ? t("losses.form.success") : null} />
                <div className="mt-4">
                    <Button type="submit" loading={isSubmitting} disabled={batches.length === 0}>
                        {t("losses.form.submit")}
                    </Button>
                </div>
            </form>
        </Card>
    );
}
