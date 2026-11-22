import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Card, Field, Input, Select } from "../../../shared/ui";
import PersonSelect from "../../../app/PersonSelect";
import { localDateTimeValue } from "../utils/dates";
import FormMessage from "./FormMessage";

function fieldError(errors, name, aliases = []) {
    for (const key of [name, ...aliases]) {
        if (errors?.[key]) return errors[key];
    }
    return undefined;
}

export default function QualityCheckForm({ batches, isSubmitting, fieldErrors, submitError, success, onSubmit, onClearFeedback }) {
    const { t } = useTranslation("cropstorage");
    const [values, setValues] = useState({
        batchId: "",
        moisturePercent: "",
        qualityGrade: "",
        passed: "",
        inspectorPersonId: "",
        notes: "",
        checkedAt: localDateTimeValue(),
    });

    function update(name, value) {
        setValues((current) => ({ ...current, [name]: value }));
        onClearFeedback();
    }

    function handleSubmit(event) {
        event.preventDefault();
        onSubmit({
            batch: { id: Number(values.batchId) },
            moisturePercent: values.moisturePercent === "" ? null : Number(values.moisturePercent),
            qualityGrade: values.qualityGrade.trim() || null,
            passed: values.passed === "true",
            inspectorPersonId: values.inspectorPersonId === "" ? null : Number(values.inspectorPersonId),
            notes: values.notes.trim() || null,
            checkedAt: values.checkedAt,
        });
    }

    return (
        <Card title={t("quality.form.title")} titleAs="h3">
            <form onSubmit={handleSubmit}>
                <div className="grid gap-4 sm:grid-cols-2">
                    <Field label={t("quality.form.batch")} required error={fieldError(fieldErrors, "batchId", ["batch", "batch.id"])}>
                        <Select value={values.batchId} onChange={(event) => update("batchId", event.target.value)} required>
                            <option value="">{t("form.choose")}</option>
                            {batches.map((batch) => (
                                <option key={batch.id} value={batch.id}>
                                    {batch.batchCode}
                                </option>
                            ))}
                        </Select>
                    </Field>
                    <Field label={t("quality.form.checkedAt")} required error={fieldError(fieldErrors, "checkedAt")}>
                        <Input type="datetime-local" value={values.checkedAt} onChange={(event) => update("checkedAt", event.target.value)} required />
                    </Field>
                    <Field label={t("quality.form.moisture")} error={fieldError(fieldErrors, "moisturePercent")}>
                        <Input
                            type="number"
                            min="0"
                            max="100"
                            step="0.01"
                            value={values.moisturePercent}
                            onChange={(event) => update("moisturePercent", event.target.value)}
                        />
                    </Field>
                    <Field label={t("quality.form.grade")} error={fieldError(fieldErrors, "qualityGrade")}>
                        <Input value={values.qualityGrade} onChange={(event) => update("qualityGrade", event.target.value)} />
                    </Field>
                    <Field label={t("quality.form.result")} required error={fieldError(fieldErrors, "passed")}>
                        <Select value={values.passed} onChange={(event) => update("passed", event.target.value)} required>
                            <option value="">{t("form.choose")}</option>
                            <option value="true">{t("quality.passed")}</option>
                            <option value="false">{t("quality.failed")}</option>
                        </Select>
                    </Field>
                    <PersonSelect
                        label={t("quality.form.inspector")}
                        value={values.inspectorPersonId}
                        onChange={(value) => update("inspectorPersonId", value)}
                        error={fieldError(fieldErrors, "inspectorPersonId")}
                    />
                    <Field label={t("quality.form.notes")} error={fieldError(fieldErrors, "notes")} className="sm:col-span-2">
                        <Input value={values.notes} onChange={(event) => update("notes", event.target.value)} />
                    </Field>
                </div>
                <FormMessage error={submitError} success={success ? t("quality.form.success") : null} />
                <div className="mt-4">
                    <Button type="submit" loading={isSubmitting} disabled={batches.length === 0}>
                        {t("quality.form.submit")}
                    </Button>
                </div>
            </form>
        </Card>
    );
}
