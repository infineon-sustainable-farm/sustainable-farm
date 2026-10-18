import { useState } from "react";
import { X } from "lucide-react";
import { formatCode } from "../utils/format";
import PlantsModal from "./PlantsModal";

const LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none";
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

// The API codes, sent as they are; only their labels are shown.
const FERTILIZER_TYPES = ["MINERAL", "ORGANIC"];
const UNITS = [
    { code: "KG", label: "kg" },
    { code: "L", label: "L" },
];

/** Same rule as the API: 0 or more, with at most 3 decimals. */
const THRESHOLD_PATTERN = /^\d+(\.\d{1,3})?$/;

function FormField({ id, label, error, note, children }) {
    return (
        <div>
            <label htmlFor={id} className={LABEL_CLASS}>
                {label}
            </label>
            {children}
            {error && (
                <p id={`${id}-error`} className={ERROR_CLASS}>
                    {error}
                </p>
            )}
            {note && (
                <p id={`${id}-note`} className={NOTE_CLASS}>
                    {note}
                </p>
            )}
        </div>
    );
}

/**
 * Form of a new fertilizer of the catalogue. It starts with a stock of 0,
 * until a purchase is recorded.
 *
 * The alert threshold is optional and, as the API offers no way to change
 * it, final: the note under the field says so. The checks mirror the API
 * rules; what the API still refuses is shown the same way as for a planting:
 * - a 400 with fieldErrors: each message under its field;
 * - a 409 (a fertilizer of that name exists), or any other error: one message
 *   above the form.
 */
export default function AddFertilizerModal({ isSubmitting, error, onSubmit, onClose }) {
    const [name, setName] = useState("");
    const [fertilizerType, setFertilizerType] = useState("");
    const [composition, setComposition] = useState("");
    const [unit, setUnit] = useState("");
    const [reorderThreshold, setReorderThreshold] = useState("");
    // A number field holds "" for text it cannot read, such as "1e"; this
    // keeps such a value from passing as an empty, optional threshold.
    const [isThresholdUnreadable, setIsThresholdUnreadable] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];
    const fieldProps = (field, id, hasNote = false) => ({
        "aria-invalid": fieldError(field) ? true : undefined,
        "aria-describedby":
            [fieldError(field) ? `${id}-error` : null, hasNote ? `${id}-note` : null]
                .filter(Boolean)
                .join(" ") || undefined,
    });

    function changeThreshold(event) {
        setReorderThreshold(event.target.value);
        setIsThresholdUnreadable(event.target.validity.badInput);
    }

    function validate() {
        const errors = {};
        if (!name.trim()) {
            errors.name = "Name is required.";
        } else if (name.length > 255) {
            errors.name = "Name must be at most 255 characters.";
        }
        if (!fertilizerType) {
            errors.fertilizerType = "Type is required.";
        }
        if (composition.length > 100) {
            errors.composition = "Composition must be at most 100 characters.";
        }
        if (!unit) {
            errors.unit = "Unit is required.";
        }
        if (isThresholdUnreadable
            || (reorderThreshold.trim() !== "" && !THRESHOLD_PATTERN.test(reorderThreshold.trim()))) {
            errors.reorderThreshold = "Alert threshold must be 0 or more, with at most 3 decimals.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        // Optional fields left empty are not sent at all.
        const fertilizer = { name, fertilizerType, unit };
        if (composition.trim()) fertilizer.composition = composition;
        if (reorderThreshold.trim()) fertilizer.reorderThreshold = Number(reorderThreshold);
        onSubmit(fertilizer);
    }

    return (
        <PlantsModal labelledBy="add-fertilizer-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h3
                    id="add-fertilizer-title"
                    className="font-heading text-xl font-bold text-gray-900"
                >
                    Add a fertilizer
                </h3>
                <button
                    type="button"
                    onClick={onClose}
                    aria-label="Close"
                    className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-gray-800"
                >
                    <X size={20} />
                </button>
            </div>

            <form noValidate onSubmit={handleSubmit} className="px-6 py-5">
                {/* Text in the darkened error hue of VigorBadge: #9a3f32 on the tinted
                    alert (#fcf6f5) -> 6.27:1. --color-error as text gave 4.38:1. */}
                {formError && (
                    <p
                        role="alert"
                        className="mb-5 rounded-lg border border-error/30 bg-error/5 px-3 py-2 text-sm text-[#9a3f32]"
                    >
                        {formError}
                    </p>
                )}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <FormField id="fertilizer-name" label="Name" error={fieldError("name")}>
                        <input
                            id="fertilizer-name"
                            type="text"
                            autoFocus
                            value={name}
                            onChange={(event) => setName(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("name", "fertilizer-name")}
                        />
                    </FormField>

                    <FormField id="fertilizer-type" label="Type" error={fieldError("fertilizerType")}>
                        <select
                            id="fertilizer-type"
                            value={fertilizerType}
                            onChange={(event) => setFertilizerType(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("fertilizerType", "fertilizer-type")}
                        >
                            {/* Empty value: sending without a choice gives "Type is required." */}
                            <option value="">Select a type</option>
                            {FERTILIZER_TYPES.map((type) => (
                                <option key={type} value={type}>
                                    {formatCode(type)}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField id="fertilizer-composition" label="Composition" error={fieldError("composition")}>
                        {/* The default placeholder is the text colour at half opacity, about
                            3:1 on white; gray-500 (#6a7282) reaches 4.84:1. */}
                        <input
                            id="fertilizer-composition"
                            type="text"
                            value={composition}
                            onChange={(event) => setComposition(event.target.value)}
                            placeholder="e.g. 15-15-15"
                            className={`${INPUT_CLASS} placeholder:text-gray-500`}
                            {...fieldProps("composition", "fertilizer-composition")}
                        />
                    </FormField>

                    <FormField id="fertilizer-unit" label="Unit" error={fieldError("unit")}>
                        <select
                            id="fertilizer-unit"
                            value={unit}
                            onChange={(event) => setUnit(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("unit", "fertilizer-unit")}
                        >
                            {/* Empty value: sending without a choice gives "Unit is required." */}
                            <option value="">Select a unit</option>
                            {UNITS.map(({ code, label }) => (
                                <option key={code} value={code}>
                                    {label}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField
                        id="fertilizer-threshold"
                        label="Alert threshold"
                        error={fieldError("reorderThreshold")}
                        note="Optional. It cannot be changed later."
                    >
                        <input
                            id="fertilizer-threshold"
                            type="number"
                            min="0"
                            step="any"
                            inputMode="decimal"
                            value={reorderThreshold}
                            onChange={changeThreshold}
                            className={INPUT_CLASS}
                            {...fieldProps("reorderThreshold", "fertilizer-threshold", true)}
                        />
                    </FormField>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                    >
                        {isSubmitting ? "Adding…" : "Add fertilizer"}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
