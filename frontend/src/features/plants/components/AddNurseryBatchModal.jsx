import { useState } from "react";
import { X } from "lucide-react";
import { useVarietyBlocks } from "../hooks/useVarieties";
import { useVarietyReferences } from "../hooks/useVarietyReferences";
import { formatCode, formatOrigin } from "../utils/format";
import { batchRequestBody, NURSERY_ORIGINS, NURSERY_STAGES, PURCHASED } from "../utils/nurseryRequests";
import PlantsModal from "./PlantsModal";

const LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none";
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

/**
 * Same rules as the API: a batch code of up to 20 letters, digits or hyphens,
 * a block as for a planting, a whole count of up to 9 digits, and the lengths
 * of the supplier and its lot number. Spaces around a code are removed by the
 * API.
 */
const BATCH_CODE_PATTERN = /^\s*[A-Za-z0-9-]{1,20}\s*$/;
const BLOCK_CODE_PATTERN = /^\s*[A-Za-z0-9]{1,10}\s*$/;
const COUNT_PATTERN = /^\d{1,9}$/;
const TEXT_MAX_LENGTH = 255;
const LOT_NUMBER_MAX_LENGTH = 50;

// The notes under the fields, by field.
const NOTES = {
    batchCode: "Letters are saved in capitals: p1 becomes P1.",
    varietyName: "The transplant plants this variety. The forecast uses the reference of the same name.",
    startedOn: "Day of sowing, or of reception for a purchase.",
    plannedBlockCode: "Optional. The transplant alerts name it.",
};

// The fields of a purchase only.
const SUPPLIER_FIELDS = ["supplier", "supplierLotNumber"];

const INITIAL_VALUES = {
    batchCode: "",
    varietyName: "",
    origin: "",
    supplier: "",
    supplierLotNumber: "",
    startedOn: "",
    initialCount: "",
    initialStage: "",
    plannedTransplantOn: "",
    plannedBlockCode: "",
};

/** Today as "YYYY-MM-DD", in local time: the latest start date the form accepts. */
function todayAsIsoDate() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${now.getFullYear()}-${month}-${day}`;
}

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
 * Form of a new nursery batch: its code, variety and origin, its start date,
 * count and stage, and when and where it is planned to be transplanted.
 *
 * The supplier and its lot number are asked for an external purchase only,
 * as the API requires the supplier then and refuses both otherwise. Going
 * back to in-house production empties them and drops their errors, the API's
 * included. The variety names of the reference and the blocks of the recorded
 * varieties are offered as suggestions; both stay free text.
 *
 * The checks mirror the API's 400 rules; what the API still refuses is shown
 * under its field, as for the reference of a variety: a 400 under each field
 * it names, a 409 (the code is taken) under the code. A 422 (the planned date
 * is before the start date), or any other error, is shown above the form.
 */
export default function AddNurseryBatchModal({ isSubmitting, error, onSubmit, onResetError, onClose }) {
    const [values, setValues] = useState(INITIAL_VALUES);
    // A number field holds "" for text it cannot read, such as "1e"; this
    // keeps such a value from passing as an empty one.
    const [isCountUnreadable, setIsCountUnreadable] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);

    const { data: references } = useVarietyReferences();
    const { data: blocks } = useVarietyBlocks();

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const codeConflict = error?.status === 409 ? error.message : null;
    const formError = error && !serverFieldErrors && !codeConflict ? error.message : null;
    const fieldError = (field) =>
        fieldErrors[field] ?? serverFieldErrors?.[field] ?? (field === "batchCode" ? codeConflict : null);
    const fieldProps = (field, id) => ({
        "aria-invalid": fieldError(field) ? true : undefined,
        "aria-describedby":
            [fieldError(field) ? `${id}-error` : null, NOTES[field] ? `${id}-note` : null].filter(Boolean).join(" ")
            || undefined,
    });
    const isPurchase = values.origin === PURCHASED;

    function setValue(field, value) {
        setValues((current) => ({ ...current, [field]: value }));
    }

    function changeCount(event) {
        setValue("initialCount", event.target.value);
        setIsCountUnreadable(event.target.validity.badInput);
    }

    function changeOrigin(nextOrigin) {
        if (!isPurchase) {
            setValue("origin", nextOrigin);
            return;
        }
        setValues((current) => ({ ...current, origin: nextOrigin, supplier: "", supplierLotNumber: "" }));
        setFieldErrors((current) =>
            Object.fromEntries(Object.entries(current).filter(([field]) => !SUPPLIER_FIELDS.includes(field))));
        // The API's answer may be about the supplier fields, which are gone.
        onResetError();
    }

    function validate() {
        const errors = {};
        if (!values.batchCode.trim()) {
            errors.batchCode = "Batch code is required.";
        } else if (!BATCH_CODE_PATTERN.test(values.batchCode)) {
            errors.batchCode = "Batch code must be up to 20 letters, digits or hyphens, such as P1 or KEITT-2026-01.";
        }
        if (!values.varietyName.trim()) {
            errors.varietyName = "Variety is required.";
        } else if (values.varietyName.length > TEXT_MAX_LENGTH) {
            errors.varietyName = `Variety must be at most ${TEXT_MAX_LENGTH} characters.`;
        }
        if (!values.origin) {
            errors.origin = "Origin is required.";
        }
        if (isPurchase) {
            if (!values.supplier.trim()) {
                errors.supplier = "Supplier is required for an external purchase.";
            } else if (values.supplier.length > TEXT_MAX_LENGTH) {
                errors.supplier = `Supplier must be at most ${TEXT_MAX_LENGTH} characters.`;
            }
            if (values.supplierLotNumber.trim() && values.supplierLotNumber.length > LOT_NUMBER_MAX_LENGTH) {
                errors.supplierLotNumber = `Supplier lot number must be at most ${LOT_NUMBER_MAX_LENGTH} characters.`;
            }
        }
        if (!values.startedOn) {
            errors.startedOn = "Start date is required.";
        } else if (values.startedOn > today) {
            errors.startedOn = "Start date cannot be in the future.";
        }
        const count = values.initialCount.trim();
        if (isCountUnreadable || !COUNT_PATTERN.test(count) || Number(count) < 1) {
            errors.initialCount = "Initial count must be a whole number, 1 or more.";
        }
        if (!values.initialStage) {
            errors.initialStage = "Initial stage is required.";
        }
        if (!values.plannedTransplantOn) {
            errors.plannedTransplantOn = "Planned transplant date is required.";
        }
        if (values.plannedBlockCode.trim() && !BLOCK_CODE_PATTERN.test(values.plannedBlockCode)) {
            errors.plannedBlockCode = "Planned block must be a short code such as E or B2, without prefix or space.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit(batchRequestBody(values));
    }

    return (
        <PlantsModal labelledBy="add-batch-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h3 id="add-batch-title" className="font-heading text-xl font-bold text-gray-900">
                    Add a nursery batch
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
                {/* Text in the darkened error hue of the dialogs: #9a3f32 on the tinted
                    alert (#fcf6f5) -> 6.27:1. */}
                {formError && (
                    <p
                        role="alert"
                        className="mb-5 rounded-lg border border-error/30 bg-error/5 px-3 py-2 text-sm text-[#9a3f32]"
                    >
                        {formError}
                    </p>
                )}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <FormField id="batch-code" label="Batch code" error={fieldError("batchCode")} note={NOTES.batchCode}>
                        <input
                            id="batch-code"
                            type="text"
                            autoFocus
                            value={values.batchCode}
                            onChange={(event) => setValue("batchCode", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("batchCode", "batch-code")}
                        />
                    </FormField>

                    <FormField
                        id="batch-variety"
                        label="Variety"
                        error={fieldError("varietyName")}
                        note={NOTES.varietyName}
                    >
                        <input
                            id="batch-variety"
                            type="text"
                            list="batch-variety-options"
                            value={values.varietyName}
                            onChange={(event) => setValue("varietyName", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("varietyName", "batch-variety")}
                        />
                        <datalist id="batch-variety-options">
                            {(references ?? []).map((reference) => (
                                <option key={reference.id} value={reference.varietyName} />
                            ))}
                        </datalist>
                    </FormField>

                    <fieldset className="sm:col-span-2">
                        <legend className={LABEL_CLASS}>Origin</legend>
                        <div className="mt-2 flex flex-wrap gap-x-6 gap-y-2">
                            {NURSERY_ORIGINS.map((origin) => (
                                <label key={origin} className="flex items-center gap-2 text-sm text-gray-800">
                                    <input
                                        type="radio"
                                        name="batch-origin"
                                        value={origin}
                                        checked={values.origin === origin}
                                        onChange={() => changeOrigin(origin)}
                                        aria-invalid={fieldError("origin") ? true : undefined}
                                        aria-describedby={fieldError("origin") ? "batch-origin-error" : undefined}
                                        className="accent-primary"
                                    />
                                    {formatOrigin(origin)}
                                </label>
                            ))}
                        </div>
                        {fieldError("origin") && (
                            <p id="batch-origin-error" className={ERROR_CLASS}>
                                {fieldError("origin")}
                            </p>
                        )}
                    </fieldset>

                    {isPurchase && (
                        <>
                            <FormField id="batch-supplier" label="Supplier" error={fieldError("supplier")}>
                                <input
                                    id="batch-supplier"
                                    type="text"
                                    value={values.supplier}
                                    onChange={(event) => setValue("supplier", event.target.value)}
                                    className={INPUT_CLASS}
                                    {...fieldProps("supplier", "batch-supplier")}
                                />
                            </FormField>

                            <FormField
                                id="batch-supplier-lot"
                                label="Supplier lot number"
                                error={fieldError("supplierLotNumber")}
                            >
                                <input
                                    id="batch-supplier-lot"
                                    type="text"
                                    value={values.supplierLotNumber}
                                    onChange={(event) => setValue("supplierLotNumber", event.target.value)}
                                    className={INPUT_CLASS}
                                    {...fieldProps("supplierLotNumber", "batch-supplier-lot")}
                                />
                            </FormField>
                        </>
                    )}

                    <FormField id="batch-start" label="Start date" error={fieldError("startedOn")} note={NOTES.startedOn}>
                        <input
                            id="batch-start"
                            type="date"
                            max={today}
                            value={values.startedOn}
                            onChange={(event) => setValue("startedOn", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("startedOn", "batch-start")}
                        />
                    </FormField>

                    <FormField id="batch-count" label="Initial count" error={fieldError("initialCount")}>
                        <input
                            id="batch-count"
                            type="number"
                            min="1"
                            step="1"
                            inputMode="numeric"
                            value={values.initialCount}
                            onChange={changeCount}
                            className={INPUT_CLASS}
                            {...fieldProps("initialCount", "batch-count")}
                        />
                    </FormField>

                    <FormField id="batch-stage" label="Initial stage" error={fieldError("initialStage")}>
                        <select
                            id="batch-stage"
                            value={values.initialStage}
                            onChange={(event) => setValue("initialStage", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("initialStage", "batch-stage")}
                        >
                            {/* Empty value: sending without a choice gives "Initial stage is required." */}
                            <option value="">Select a stage</option>
                            {NURSERY_STAGES.map((stage) => (
                                <option key={stage} value={stage}>
                                    {formatCode(stage)}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField
                        id="batch-planned-date"
                        label="Planned transplant date"
                        error={fieldError("plannedTransplantOn")}
                    >
                        <input
                            id="batch-planned-date"
                            type="date"
                            value={values.plannedTransplantOn}
                            onChange={(event) => setValue("plannedTransplantOn", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("plannedTransplantOn", "batch-planned-date")}
                        />
                    </FormField>

                    <FormField
                        id="batch-planned-block"
                        label="Planned block"
                        error={fieldError("plannedBlockCode")}
                        note={NOTES.plannedBlockCode}
                    >
                        <input
                            id="batch-planned-block"
                            type="text"
                            list="batch-block-options"
                            value={values.plannedBlockCode}
                            onChange={(event) => setValue("plannedBlockCode", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("plannedBlockCode", "batch-planned-block")}
                        />
                        <datalist id="batch-block-options">
                            {(blocks ?? []).map((block) => (
                                <option key={block} value={block} />
                            ))}
                        </datalist>
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
                        {isSubmitting ? "Adding…" : "Add batch"}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
