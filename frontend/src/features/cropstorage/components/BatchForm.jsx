import { useState } from "react";
import CropStorageField from "./CropStorageField";

const INITIAL_VALUES = {
    batchCode: "",
    productType: "",
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
    status: "",
    storageZoneId: "",
    expectedDispatchDate: "",
    expiryDate: "",
    customerPriority: "0",
    valuePerKgFcfa: "",
};

const FIELD_DEFINITIONS = [
    { name: "batchCode", label: "Batch code", required: true },
    { name: "productType", label: "Product type", required: true },
    { name: "productStage", label: "Product stage" },
    { name: "origin", label: "Origin" },
    { name: "supplierId", label: "Supplier", options: [] },
    { name: "mangoVariety", label: "Mango variety" },
    { name: "harvestDate", label: "Harvest date", type: "date" },
    { name: "harvestId", label: "Harvest ID", type: "number", min: "1", step: "1" },
    { name: "processingDate", label: "Processing date", type: "date" },
    { name: "packagingDate", label: "Packaging date", type: "date" },
    { name: "storageEntryDate", label: "Storage entry date", type: "date" },
    { name: "initialQuantityKg", label: "Initial quantity (kg)", type: "number", min: "0", step: "0.01", required: true },
    { name: "currentQuantityKg", label: "Current quantity (kg)", type: "number", min: "0", step: "0.01" },
    { name: "moisturePercent", label: "Moisture (%)", type: "number", min: "0", max: "100", step: "0.01" },
    { name: "qualityGrade", label: "Quality grade" },
    { name: "status", label: "Status", required: true },
    { name: "storageZoneId", label: "Storage zone", options: [] },
    { name: "expectedDispatchDate", label: "Expected dispatch date", type: "date" },
    { name: "expiryDate", label: "Expiry date", type: "date" },
    { name: "customerPriority", label: "Customer priority", type: "number", min: "0", step: "1" },
    { name: "valuePerKgFcfa", label: "Value per kg (FCFA)", type: "number", min: "0", step: "0.01" },
];

const NUMERIC_FIELDS = [
    "harvestId",
    "initialQuantityKg",
    "currentQuantityKg",
    "moisturePercent",
    "customerPriority",
    "valuePerKgFcfa",
];

const OPTIONAL_FIELDS = [
    "productStage",
    "origin",
    "supplierId",
    "mangoVariety",
    "harvestDate",
    "harvestId",
    "processingDate",
    "packagingDate",
    "storageEntryDate",
    "currentQuantityKg",
    "moisturePercent",
    "qualityGrade",
    "storageZoneId",
    "expectedDispatchDate",
    "expiryDate",
    "valuePerKgFcfa",
];

function fieldError(errors, name) {
    const snakeName = name.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`);
    return errors?.[name] ?? errors?.[snakeName];
}

export default function BatchForm({
    zones,
    suppliers,
    isSubmitting,
    fieldErrors,
    submitError,
    success,
    onSubmit,
    onClearError,
}) {
    const [values, setValues] = useState(INITIAL_VALUES);
    const optionsByField = {
        supplierId: suppliers.map((supplier) => ({
            value: supplier.id,
            label: supplier.name ?? supplier.companyName ?? supplier.id,
        })),
        storageZoneId: zones.map((zone) => ({
            value: zone.id,
            label: zone.name ? `${zone.zoneCode ?? zone.code} — ${zone.name}` : zone.zoneCode ?? zone.code,
        })),
    };

    function update(name, value) {
        setValues((current) => ({ ...current, [name]: value }));
        onClearError();
    }

    function handleSubmit(event) {
        event.preventDefault();
        const payload = { ...values };
        for (const name of OPTIONAL_FIELDS) {
            if (payload[name] === "") payload[name] = null;
        }
        for (const name of NUMERIC_FIELDS) {
            if (payload[name] !== "" && payload[name] !== null) payload[name] = Number(payload[name]);
        }
        payload.batchCode = payload.batchCode.trim();
        payload.productType = payload.productType.trim();
        payload.status = payload.status.trim();
        for (const name of ["productStage", "origin", "mangoVariety", "qualityGrade"]) {
            if (payload[name] === "") payload[name] = null;
            else if (payload[name] !== null) payload[name] = payload[name].trim();
        }
        onSubmit(payload);
    }

    return (
        <form
            onSubmit={handleSubmit}
            className="rounded-xl border border-gray-200 bg-white p-5"
        >
            <h2 className="font-heading text-lg font-semibold text-gray-900">Create batch</h2>
            <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {FIELD_DEFINITIONS.map(({ name, label, options, ...props }) => (
                    <CropStorageField
                        key={name}
                        id={`batch-${name}`}
                        label={label}
                        value={values[name]}
                        options={options ? optionsByField[name] : undefined}
                        onChange={(event) => update(name, event.target.value)}
                        error={fieldError(fieldErrors, name)}
                        {...props}
                    />
                ))}
            </div>
            {submitError && (
                <p role="alert" className="mt-4 rounded-md border border-error/30 bg-error/5 px-3 py-2 text-sm text-error">
                    {submitError}
                </p>
            )}
            {success && (
                <p role="status" className="mt-4 rounded-md border border-success/30 bg-success/5 px-3 py-2 text-sm text-success">
                    Batch created successfully.
                </p>
            )}
            <button
                type="submit"
                disabled={isSubmitting}
                className="mt-4 rounded-md bg-primary px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
                {isSubmitting ? "Creating…" : "Create batch"}
            </button>
        </form>
    );
}
