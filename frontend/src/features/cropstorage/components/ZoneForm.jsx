import { useState } from "react";
import CropStorageField from "./CropStorageField";

const INITIAL_VALUES = {
    zoneCode: "",
    name: "",
    description: "",
    capacityKg: "",
    status: "",
};

const FIELD_DEFINITIONS = [
    { name: "zoneCode", label: "Code", required: true },
    { name: "name", label: "Name" },
    { name: "capacityKg", label: "Capacity (kg)", type: "number", min: "0.01", step: "0.01", required: true },
    { name: "status", label: "Status", required: true },
    { name: "description", label: "Description" },
];

export default function ZoneForm({ isSubmitting, fieldErrors, submitError, success, onSubmit, onClearError }) {
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
            status: values.status.trim(),
        });
    }

    return (
        <form
            onSubmit={handleSubmit}
            className="rounded-xl border border-gray-200 bg-white p-5"
        >
            <h2 className="font-heading text-lg font-semibold text-gray-900">Create storage zone</h2>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
                {FIELD_DEFINITIONS.map(({ name, label, ...props }) => (
                    <CropStorageField
                        key={name}
                        id={`zone-${name}`}
                        label={label}
                        value={values[name]}
                        onChange={(event) => update(name, event.target.value)}
                        error={fieldErrors?.[name]}
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
                    Storage zone created successfully.
                </p>
            )}
            <button
                type="submit"
                disabled={isSubmitting}
                className="mt-4 rounded-md bg-primary px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
                {isSubmitting ? "Creating…" : "Create zone"}
            </button>
        </form>
    );
}
