import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Card, Field, Input, Select } from "../../../shared/ui";
import { GROUP, MOVEMENT_TYPES, enumLabel } from "../utils/enums";
import { localDateTimeValue } from "../utils/dates";
import FormMessage from "./FormMessage";

const INITIAL_VALUES = { batchId: "", movementType: "", quantityKg: "", targetZoneId: "", reason: "" };

function getFieldError(errors, field, aliases = []) {
    for (const key of [field, ...aliases]) {
        if (errors?.[key]) return errors[key];
    }
    return undefined;
}

export default function MovementForm({ batches, zones, isSubmitting, fieldErrors, submitError, success, onSubmit, onClearFeedback }) {
    const { t } = useTranslation("cropstorage");
    const [values, setValues] = useState(INITIAL_VALUES);
    const needsZone = values.movementType === "IN" || values.movementType === "TRANSFER";

    function update(field, value) {
        setValues((current) => ({ ...current, [field]: value }));
        onClearFeedback();
    }

    function handleSubmit(event) {
        event.preventDefault();
        onSubmit({
            batchId: values.batchId,
            movementType: values.movementType,
            quantityKg: Number(values.quantityKg),
            targetZoneId: values.targetZoneId || null,
            reason: values.reason.trim() || null,
            movedAt: localDateTimeValue(),
        });
    }

    return (
        <Card title={t("movements.form.title")}>
            <form onSubmit={handleSubmit}>
                <div className="grid gap-4 sm:grid-cols-2">
                    <Field label={t("movements.form.batch")} required error={getFieldError(fieldErrors, "batchId", ["batch", "batch.id"])}>
                        <Select value={values.batchId} onChange={(event) => update("batchId", event.target.value)} required>
                            <option value="">{t("form.choose")}</option>
                            {batches.map((batch) => (
                                <option key={batch.id} value={batch.id}>
                                    {batch.batchCode}
                                </option>
                            ))}
                        </Select>
                    </Field>
                    <Field label={t("movements.form.type")} required error={getFieldError(fieldErrors, "movementType", ["type"])}>
                        <Select value={values.movementType} onChange={(event) => update("movementType", event.target.value)} required>
                            <option value="">{t("form.choose")}</option>
                            {MOVEMENT_TYPES.map((type) => (
                                <option key={type} value={type}>
                                    {enumLabel(t, GROUP.movementType, type)}
                                </option>
                            ))}
                        </Select>
                    </Field>
                    <Field label={t("movements.form.quantityKg")} required error={getFieldError(fieldErrors, "quantityKg")}>
                        <Input
                            type="number"
                            min="0.01"
                            step="0.01"
                            value={values.quantityKg}
                            onChange={(event) => update("quantityKg", event.target.value)}
                            required
                        />
                    </Field>
                    <Field
                        label={t("movements.form.targetZone")}
                        required={needsZone}
                        error={getFieldError(fieldErrors, "targetZoneId", ["toZone", "toZone.id"])}
                    >
                        <Select value={values.targetZoneId} onChange={(event) => update("targetZoneId", event.target.value)} required={needsZone}>
                            <option value="">{t("form.none")}</option>
                            {zones.map((zone) => (
                                <option key={zone.id} value={zone.id}>
                                    {zone.name ? `${zone.zoneCode} — ${zone.name}` : zone.zoneCode}
                                </option>
                            ))}
                        </Select>
                    </Field>
                    <Field label={t("movements.form.reason")} error={getFieldError(fieldErrors, "reason")} className="sm:col-span-2">
                        <Input value={values.reason} onChange={(event) => update("reason", event.target.value)} />
                    </Field>
                </div>
                <FormMessage error={submitError} success={success ? t("movements.form.success") : null} />
                <div className="mt-4">
                    <Button type="submit" loading={isSubmitting} disabled={batches.length === 0}>
                        {t("movements.form.submit")}
                    </Button>
                </div>
            </form>
        </Card>
    );
}
