import { useState } from "react";
import CropStorageField from "./CropStorageField";

function localDateTimeValue() {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
}

function batchOptions(batches) {
    return batches.map((batch) => ({
        value: batch.id,
        label: batch.batchCode ?? batch.batch_code ?? `Batch ${batch.id}`,
    }));
}

function fieldError(errors, name, aliases = []) {
    for (const key of [name, ...aliases]) {
        if (errors?.[key]) return errors[key];
    }
    return undefined;
}

export default function QualityCheckForm({
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
        moisturePercent: "",
        qualityGrade: "",
        passed: "",
        inspector: "",
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
            inspector: values.inspector.trim() || null,
            notes: values.notes.trim() || null,
            checkedAt: values.checkedAt,
        });
    }

    return (
        <form
            onSubmit={handleSubmit}
            className="rounded-xl border border-line bg-white p-5"
        >
            <h3 className="font-heading text-lg font-semibold text-gray-900">Add quality check</h3>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <CropStorageField
                    id="quality-batch"
                    label="Batch"
                    value={values.batchId}
                    onChange={(event) => update("batchId", event.target.value)}
                    options={batchOptions(batches)}
                    required
                    error={fieldError(fieldErrors, "batchId", ["batch", "batch.id"])}
                />
                <CropStorageField
                    id="quality-checked-at"
                    label="Checked at"
                    type="datetime-local"
                    value={values.checkedAt}
                    onChange={(event) => update("checkedAt", event.target.value)}
                    required
                    error={fieldError(fieldErrors, "checkedAt")}
                />
                <CropStorageField
                    id="quality-moisture"
                    label="Moisture (%)"
                    type="number"
                    min="0"
                    max="100"
                    step="0.01"
                    value={values.moisturePercent}
                    onChange={(event) => update("moisturePercent", event.target.value)}
                    error={fieldError(fieldErrors, "moisturePercent")}
                />
                <CropStorageField
                    id="quality-grade"
                    label="Quality grade"
                    value={values.qualityGrade}
                    onChange={(event) => update("qualityGrade", event.target.value)}
                    error={fieldError(fieldErrors, "qualityGrade")}
                />
                <CropStorageField
                    id="quality-passed"
                    label="Result"
                    value={values.passed}
                    onChange={(event) => update("passed", event.target.value)}
                    options={[
                        { value: "true", label: "Passed" },
                        { value: "false", label: "Failed" },
                    ]}
                    required
                    error={fieldError(fieldErrors, "passed")}
                />
                <CropStorageField
                    id="quality-inspector"
                    label="Inspector"
                    value={values.inspector}
                    onChange={(event) => update("inspector", event.target.value)}
                    error={fieldError(fieldErrors, "inspector")}
                />
                <div className="sm:col-span-2">
                    <CropStorageField
                        id="quality-notes"
                        label="Notes"
                        value={values.notes}
                        onChange={(event) => update("notes", event.target.value)}
                        error={fieldError(fieldErrors, "notes")}
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
                    Quality check created.
                </p>
            )}
            <button
                type="submit"
                disabled={isSubmitting || batches.length === 0}
                className="mt-4 rounded-md bg-primary px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
                {isSubmitting ? "Saving…" : "Add quality check"}
            </button>
        </form>
    );
}
