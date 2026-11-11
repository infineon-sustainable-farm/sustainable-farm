import { useState } from "react";
import CropStorageField from "./CropStorageField";

function localDateTimeValue() {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
}

function fieldError(errors, name, aliases = []) {
    for (const key of [name, ...aliases]) {
        if (errors?.[key]) return errors[key];
    }
    return undefined;
}

export default function LossRecordForm({
    batches,
    isSubmitting,
    fieldErrors,
    submitError,
    success,
    onSubmit,
    onClearFeedback,
}) {
    const [values, setValues] = useState({
        batchId: "",
        quantityKg: "",
        cause: "",
        valueFcfa: "",
        recordedAt: localDateTimeValue(),
    });

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
        <form
            onSubmit={handleSubmit}
            className="rounded-xl border border-line bg-white p-5"
        >
            <h3 className="font-heading text-lg font-semibold text-gray-900">Add loss record</h3>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <CropStorageField
                    id="loss-batch"
                    label="Batch"
                    value={values.batchId}
                    onChange={(event) => update("batchId", event.target.value)}
                    options={batches.map((batch) => ({
                        value: batch.id,
                        label: batch.batchCode ?? batch.batch_code ?? `Batch ${batch.id}`,
                    }))}
                    required
                    error={fieldError(fieldErrors, "batchId", ["batch", "batch.id"])}
                />
                <CropStorageField
                    id="loss-recorded-at"
                    label="Recorded at"
                    type="datetime-local"
                    value={values.recordedAt}
                    onChange={(event) => update("recordedAt", event.target.value)}
                    required
                    error={fieldError(fieldErrors, "recordedAt")}
                />
                <CropStorageField
                    id="loss-quantity"
                    label="Quantity (kg)"
                    type="number"
                    min="0"
                    step="0.01"
                    value={values.quantityKg}
                    onChange={(event) => update("quantityKg", event.target.value)}
                    required
                    error={fieldError(fieldErrors, "quantityKg")}
                />
                <CropStorageField
                    id="loss-value"
                    label="Value (FCFA)"
                    type="number"
                    min="0"
                    step="0.01"
                    value={values.valueFcfa}
                    onChange={(event) => update("valueFcfa", event.target.value)}
                    error={fieldError(fieldErrors, "valueFcfa")}
                />
                <div className="sm:col-span-2">
                    <CropStorageField
                        id="loss-cause"
                        label="Cause"
                        value={values.cause}
                        onChange={(event) => update("cause", event.target.value)}
                        error={fieldError(fieldErrors, "cause")}
                    />
                </div>
            </div>
            {submitError && (
                <p role="alert" className="mt-4 rounded-md border border-error/30 bg-error/5 px-3 py-2 text-sm text-error">
                    {submitError}
                </p>
            )}
            {success && (
                <p role="status" className="mt-4 rounded-md border border-success/30 bg-success/5 px-3 py-2 text-sm text-success">
                    Loss record created.
                </p>
            )}
            <button
                type="submit"
                disabled={isSubmitting || batches.length === 0}
                className="mt-4 rounded-md bg-primary px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
                {isSubmitting ? "Saving…" : "Add loss record"}
            </button>
        </form>
    );
}
