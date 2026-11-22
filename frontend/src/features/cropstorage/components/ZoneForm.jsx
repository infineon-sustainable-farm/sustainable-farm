import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Card, Field, Input, Select } from "../../../shared/ui";
import { GROUP, OPERATIONAL_STATUSES, enumOptions } from "../utils/enums";
import FormMessage from "./FormMessage";

const INITIAL_VALUES = { zoneCode: "", name: "", description: "", capacityKg: "", status: "ACTIVE" };

export default function ZoneForm({ isSubmitting, fieldErrors, submitError, success, onSubmit, onClearError }) {
    const { t } = useTranslation("cropstorage");
    const [values, setValues] = useState(INITIAL_VALUES);

    function update(name, value) {
        setValues((current) => ({ ...current, [name]: value }));
        onClearError();
    }

    function handleSubmit(event) {
        event.preventDefault();
        onSubmit({
            zoneCode: values.zoneCode.trim(),
            name: values.name.trim() || null,
            description: values.description.trim() || null,
            capacityKg: Number(values.capacityKg),
            status: values.status,
        });
    }

    return (
        <Card title={t("zones.form.title")}>
            <form onSubmit={handleSubmit}>
                <div className="grid gap-4 sm:grid-cols-2">
                    <Field label={t("zones.form.code")} required error={fieldErrors?.zoneCode}>
                        <Input value={values.zoneCode} onChange={(event) => update("zoneCode", event.target.value)} required />
                    </Field>
                    <Field label={t("zones.form.name")} error={fieldErrors?.name}>
                        <Input value={values.name} onChange={(event) => update("name", event.target.value)} />
                    </Field>
                    <Field label={t("zones.form.capacityKg")} required error={fieldErrors?.capacityKg}>
                        <Input
                            type="number"
                            min="0.01"
                            step="0.01"
                            value={values.capacityKg}
                            onChange={(event) => update("capacityKg", event.target.value)}
                            required
                        />
                    </Field>
                    <Field label={t("zones.form.status")} required error={fieldErrors?.status}>
                        <Select
                            value={values.status}
                            onChange={(event) => update("status", event.target.value)}
                            options={enumOptions(t, GROUP.operationalStatus, OPERATIONAL_STATUSES)}
                            required
                        />
                    </Field>
                    <Field label={t("zones.form.description")} error={fieldErrors?.description} className="sm:col-span-2">
                        <Input value={values.description} onChange={(event) => update("description", event.target.value)} />
                    </Field>
                </div>
                <FormMessage error={submitError} success={success ? t("zones.form.success") : null} />
                <div className="mt-4">
                    <Button type="submit" loading={isSubmitting}>
                        {t("zones.form.submit")}
                    </Button>
                </div>
            </form>
        </Card>
    );
}
