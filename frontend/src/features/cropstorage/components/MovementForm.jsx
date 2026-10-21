import { useState } from "react";
import CropStorageField from "./CropStorageField";

const INITIAL_VALUES = {
    batchId: "",
    movementType: "",
    quantityKg: "",
    targetZoneId: "",
    reason: "",
};

function getFieldError(errors, field, aliases = []) {
    for (const key of [field, ...aliases]) {
        if (errors?.[key]) return errors[key];
    }
    return undefined;
}

export default function MovementForm({
    batches,
    zones,
    isSubmitting,
    fieldErrors,
    submitError,
    successNote,
    onSubmit,
    onClearFeedback,
}) {
    const [values, setValues] = useState(INITIAL_VALUES);
    const batchOptions = batches.map((batch) => ({
        value: batch.id,
        label: `${batch.batchCode ?? batch.batch_code ?? `Batch ${batch.id}`}${
            batch.productType ?? batch.product_type
                ? ` — ${batch.productType ?? batch.product_type}`
                : ""
        }`,
    }));
    const zoneOptions = zones.map((zone) => ({
        value: zone.id,
        label: zone.name
            ? `${zone.zoneCode ?? zone.zone_code} — ${zone.name}`
            : zone.zoneCode ?? zone.zone_code ?? `Zone ${zone.id}`,
    }));

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
            movedAt: new Date().toISOString(),
        });
    }

    return (
        <form onSubmit={handleSubmit} className="rounded-xl border border-gray-200 bg-white p-5">
            <h2 className="font-heading text-lg font-semibold text-gray-900">Register movement</h2>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <CropStorageField
                    id="movement-batch"
                    label="Batch"
                    value={values.batchId}
                    onChange={(event) => update("batchId", event.target.value)}
                    options={batchOptions}
                    required
                    error={getFieldError(fieldErrors, "batchId", ["batch", "batch.id"])}
                />
                <CropStorageField
                    id="movement-type"
                    label="Movement type"
                    value={values.movementType}
                    onChange={(event) => update("movementType", event.target.value)}
                    options={[
                        { value: "IN", label: "IN" },
                        { value: "OUT", label: "OUT" },
                        { value: "TRANSFER", label: "TRANSFER" },
                    ]}
                    required
                    error={getFieldError(fieldErrors, "movementType", ["type"])}
                />
                <CropStorageField
                    id="movement-quantity"
                    label="Quantity (kg)"
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={values.quantityKg}
                    onChange={(event) => update("quantityKg", event.target.value)}
                    required
                    error={getFieldError(fieldErrors, "quantityKg")}
                />
                <CropStorageField
                    id="movement-target-zone"
                    label="Target zone"
                    value={values.targetZoneId}
                    onChange={(event) => update("targetZoneId", event.target.value)}
                    options={zoneOptions}
                    required={values.movementType === "IN" || values.movementType === "TRANSFER"}
                    error={getFieldError(fieldErrors, "targetZoneId", ["toZone", "toZone.id"])}
                />
                <CropStorageField
                    id="movement-reason"
                    label="Reason"
                    value={values.reason}
                    onChange={(event) => update("reason", event.target.value)}
                    error={getFieldError(fieldErrors, "reason")}
                />
            </div>
            {submitError && (
                <p role="alert" className="mt-4 rounded-md border border-error/30 bg-error/5 px-3 py-2 text-sm text-error">
                    {submitError}
                </p>
            )}
            {successNote && (
                <p role="status" className="mt-4 rounded-md border border-success/30 bg-success/5 px-3 py-2 text-sm text-success">
                    {successNote}
                </p>
            )}
            <button
                type="submit"
                disabled={isSubmitting || batches.length === 0}
                className="mt-4 rounded-md bg-primary px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
                {isSubmitting ? "Registering…" : "Register movement"}
            </button>
        </form>
    );
}
