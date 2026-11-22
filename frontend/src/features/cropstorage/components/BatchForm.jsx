import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Card, Field, Input, OwnedResourceSelect, Select } from "../../../shared/ui";
import { OWNER_LINKS } from "../../../app/ownerLinks";
import { BATCH_STATUSES, GROUP, PRODUCT_STAGES, PRODUCT_TYPES, enumOptions } from "../utils/enums";
import { toBatchPayload } from "../utils/batchPayload";
import FormMessage from "./FormMessage";

const INITIAL_VALUES = {
    batchCode: "",
    productType: "DRIED_MANGO",
    productStage: "",
    origin: "",
    supplierId: "",
    mangoVariety: "",
    harvestDate: "",
    harvestId: "",
    processingDate: "",
    packagingDate: "",
    storageEntryDate: "",
    initialQuantityKg: "",
    currentQuantityKg: "",
    moisturePercent: "",
    qualityGrade: "",
    status: "STORED",
    storageZoneId: "",
    expectedDispatchDate: "",
    expiryDate: "",
    customerPriority: "0",
    valuePerKgFcfa: "",
};

const NUMBER = "number";
const DATE = "date";

// Fields of the form, in display order. `kind` picks the control; enums use the backend values.
const FIELDS = [
    { name: "batchCode", required: true },
    { name: "productType", kind: "enum", group: GROUP.productType, values: PRODUCT_TYPES, required: true },
    { name: "productStage", kind: "enum", group: GROUP.productStage, values: PRODUCT_STAGES, optional: true },
    { name: "status", kind: "enum", group: GROUP.batchStatus, values: BATCH_STATUSES, required: true },
    { name: "mangoVariety", kind: "variety" },
    { name: "origin" },
    { name: "supplierId", kind: "supplier" },
    { name: "storageZoneId", kind: "zone" },
    { name: "initialQuantityKg", props: { type: NUMBER, min: "0", step: "0.01" }, required: true },
    { name: "currentQuantityKg", props: { type: NUMBER, min: "0", step: "0.01" } },
    { name: "moisturePercent", props: { type: NUMBER, min: "0", max: "100", step: "0.01" } },
    { name: "qualityGrade" },
    { name: "harvestId", props: { type: NUMBER, min: "1", step: "1" } },
    { name: "harvestDate", props: { type: DATE } },
    { name: "processingDate", props: { type: DATE } },
    { name: "packagingDate", props: { type: DATE } },
    { name: "storageEntryDate", props: { type: DATE } },
    { name: "expectedDispatchDate", props: { type: DATE } },
    { name: "expiryDate", props: { type: DATE } },
    { name: "customerPriority", props: { type: NUMBER, min: "0", step: "1" } },
    { name: "valuePerKgFcfa", props: { type: NUMBER, min: "0", step: "0.01" } },
];

/** The API answers field errors by request field name; supplier and zone are nested references. */
function fieldError(errors, name) {
    const aliases = { supplierId: ["supplier", "supplier.id"], storageZoneId: ["storageZone", "storageZone.id"] };
    for (const key of [name, ...(aliases[name] ?? [])]) {
        if (errors?.[key]) return errors[key];
    }
    return undefined;
}

export default function BatchForm({
    zones,
    suppliers,
    varieties,
    isSubmitting,
    fieldErrors,
    submitError,
    success,
    onSubmit,
    onClearError,
}) {
    const { t } = useTranslation("cropstorage");
    const [values, setValues] = useState(INITIAL_VALUES);

    function update(name, value) {
        setValues((current) => ({ ...current, [name]: value }));
        onClearError();
    }

    const zoneOptions = zones.map((zone) => ({
        value: String(zone.id),
        label: zone.name ? `${zone.zoneCode} — ${zone.name}` : zone.zoneCode,
    }));
    const supplierOptions = suppliers.map((supplier) => ({ value: String(supplier.id), label: supplier.name }));

    function selectField(field, options) {
        return (
            <Field key={field.name} label={t(`batches.form.${field.name}`)} required={field.required} error={fieldError(fieldErrors, field.name)}>
                <Select value={values[field.name]} onChange={(event) => update(field.name, event.target.value)} required={field.required}>
                    {field.optional !== false && !field.required && <option value="">{t("form.none")}</option>}
                    {options.map((option) => (
                        <option key={option.value} value={option.value}>
                            {option.label}
                        </option>
                    ))}
                </Select>
            </Field>
        );
    }

    function renderField(field) {
        if (field.kind === "enum") return selectField(field, enumOptions(t, field.group, field.values));
        if (field.kind === "supplier") return selectField(field, supplierOptions);
        if (field.kind === "zone") return selectField(field, zoneOptions);
        if (field.kind === "variety") {
            return (
                <OwnedResourceSelect
                    key={field.name}
                    label={t("batches.form.mangoVariety")}
                    value={values.mangoVariety}
                    onChange={(value) => update("mangoVariety", value)}
                    options={varieties.data ?? []}
                    getOptionValue={(variety) => variety.varietyName}
                    getOptionLabel={(variety) => variety.varietyName}
                    loading={varieties.isPending}
                    loadError={varieties.isError ? t("batches.form.varietiesError") : undefined}
                    onRetry={varieties.refetch}
                    owner={OWNER_LINKS.variety}
                    error={fieldError(fieldErrors, "mangoVariety")}
                />
            );
        }
        return (
            <Field key={field.name} label={t(`batches.form.${field.name}`)} required={field.required} error={fieldError(fieldErrors, field.name)}>
                <Input
                    value={values[field.name]}
                    onChange={(event) => update(field.name, event.target.value)}
                    required={field.required}
                    {...field.props}
                />
            </Field>
        );
    }

    return (
        <Card title={t("batches.form.title")}>
            <form
                onSubmit={(event) => {
                    event.preventDefault();
                    onSubmit(toBatchPayload(values));
                }}
            >
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{FIELDS.map(renderField)}</div>
                <FormMessage error={submitError} success={success ? t("batches.form.success") : null} />
                <div className="mt-4">
                    <Button type="submit" loading={isSubmitting}>
                        {t("batches.form.submit")}
                    </Button>
                </div>
            </form>
        </Card>
    );
}
