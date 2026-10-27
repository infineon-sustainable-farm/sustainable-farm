import { useState } from "react";
import { X } from "lucide-react";
import { useVarieties } from "../hooks/useVarieties";
import { formatBlock } from "../utils/format";
import {
    MEASURED_VALUE_ERROR,
    measuredValueError,
    METHOD_OPTIONS,
    methodTakesValue,
    methodUnit,
    observationRequest,
    STATUS_OPTIONS,
} from "../utils/maturityObservations";
import PlantsModal from "./PlantsModal";

const LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none disabled:cursor-not-allowed disabled:bg-gray-50";
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

/** Today as "YYYY-MM-DD", in local time: the latest observation date the form accepts. */
function todayAsIsoDate() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${now.getFullYear()}-${month}-${day}`;
}

/** aria attributes of a field: invalid while it has an error, described by its error and notes. */
function describe(id, error, ...noteIds) {
    return {
        "aria-invalid": error ? true : undefined,
        "aria-describedby": [error ? `${id}-error` : null, ...noteIds].filter(Boolean).join(" ") || undefined,
    };
}

function FormField({ id, label, error, note, className, children }) {
    return (
        <div className={className}>
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
 * Form of a maturity observation: what an observer saw on one variety of one
 * block, on one day (PL-05-01, "Manual entry").
 *
 * The block and the variety are picked from lists, as for a harvest, so the
 * variety name goes out exactly as it was planted; only the varieties with a
 * recorded planting on the block are offered. A measured value is offered
 * with Brix and dry matter only, in their unit, and another method starts
 * without one. No farm is sent, as for a harvest.
 *
 * The checks mirror the API rules; what the API still refuses is shown the
 * same way as for a harvest:
 * - a 400 with fieldErrors: each message under its field;
 * - a 422 (no planting of that variety on the block, or an observation before
 *   the planting), or any other error: one message above the form.
 */
export default function RecordMaturityObservationModal({ blocks, isSubmitting, error, onSubmit, onClose }) {
    const [observedOn, setObservedOn] = useState("");
    const [blockCode, setBlockCode] = useState("");
    const [varietyName, setVarietyName] = useState("");
    const [maturityStatus, setMaturityStatus] = useState("");
    const [method, setMethod] = useState("");
    const [measuredValue, setMeasuredValue] = useState("");
    // A number field holds "" for text it cannot read, such as "1e"; this
    // keeps such a value from passing as an empty one.
    const [isValueUnreadable, setIsValueUnreadable] = useState(false);
    const [observer, setObserver] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);

    const varieties = useVarieties({ blockCode: blockCode || null });
    // A variety row has a current tree count once a planting is recorded for it.
    const plantedVarieties = blockCode && varieties.data
        ? varieties.data.filter((variety) => variety.currentTreeCount !== null && variety.currentTreeCount !== undefined)
        : [];

    let varietyNote = null;
    if (blockCode && varieties.isError) {
        varietyNote = "Varieties could not be loaded";
    } else if (blockCode && varieties.isSuccess && plantedVarieties.length === 0) {
        varietyNote = "No planted variety on this block";
    }
    const canChooseVariety = plantedVarieties.length > 0;

    const takesValue = methodTakesValue(method);
    const unit = methodUnit(method);

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    function changeBlock(value) {
        setBlockCode(value);
        // The variety belongs to the block: another block means another list.
        setVarietyName("");
    }

    function changeMethod(value) {
        setMethod(value);
        // A value is read with its method, in its unit: another method starts without one.
        setMeasuredValue("");
        setIsValueUnreadable(false);
        setFieldErrors((current) =>
            Object.fromEntries(Object.entries(current).filter(([field]) => field !== "measuredValue")));
    }

    function changeMeasuredValue(event) {
        setMeasuredValue(event.target.value);
        setIsValueUnreadable(event.target.validity.badInput);
    }

    function validate() {
        const errors = {};
        if (!observedOn) {
            errors.observedOn = "Observation date is required.";
        } else if (observedOn > today) {
            errors.observedOn = "Observation date cannot be in the future.";
        }
        if (!blockCode) {
            errors.blockCode = "Block is required.";
        }
        if (!varietyName) {
            errors.varietyName = "Variety is required.";
        }
        if (!maturityStatus) {
            errors.maturityStatus = "Maturity status is required.";
        }
        if (!method) {
            errors.method = "Method is required.";
        }
        if (takesValue) {
            const valueError = isValueUnreadable ? MEASURED_VALUE_ERROR : measuredValueError(measuredValue);
            if (valueError) errors.measuredValue = valueError;
        }
        if (observer.length > 255) {
            errors.observer = "Observer must be at most 255 characters.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit(observationRequest({
            observedOn,
            blockCode,
            varietyName,
            maturityStatus,
            method,
            measuredValue,
            observer,
        }));
    }

    return (
        <PlantsModal labelledBy="record-observation-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h3
                    id="record-observation-title"
                    className="font-heading text-xl font-bold text-gray-900"
                >
                    Record a maturity observation
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
                    <FormField id="observation-date" label="Observation date" error={fieldError("observedOn")}>
                        <input
                            id="observation-date"
                            type="date"
                            autoFocus
                            max={today}
                            value={observedOn}
                            onChange={(event) => setObservedOn(event.target.value)}
                            className={INPUT_CLASS}
                            {...describe("observation-date", fieldError("observedOn"))}
                        />
                    </FormField>

                    <FormField id="observation-block" label="Block" error={fieldError("blockCode")}>
                        <select
                            id="observation-block"
                            value={blockCode}
                            onChange={(event) => changeBlock(event.target.value)}
                            className={INPUT_CLASS}
                            {...describe("observation-block", fieldError("blockCode"))}
                        >
                            {/* Empty value: sending without a choice gives "Block is required." */}
                            <option value="">Select a block</option>
                            {blocks.map((block) => (
                                <option key={block} value={block}>
                                    {formatBlock(block)}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField
                        id="observation-variety"
                        label="Variety"
                        error={fieldError("varietyName")}
                        note={varietyNote}
                    >
                        <select
                            id="observation-variety"
                            value={varietyName}
                            disabled={!canChooseVariety}
                            onChange={(event) => setVarietyName(event.target.value)}
                            className={INPUT_CLASS}
                            {...describe("observation-variety", fieldError("varietyName"),
                                varietyNote ? "observation-variety-note" : null)}
                        >
                            {/* Empty value: sending without a choice gives "Variety is required." */}
                            <option value="">Select a variety</option>
                            {plantedVarieties.map((variety) => (
                                <option key={variety.id} value={variety.name}>
                                    {variety.name}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField id="observation-status" label="Maturity status" error={fieldError("maturityStatus")}>
                        <select
                            id="observation-status"
                            value={maturityStatus}
                            onChange={(event) => setMaturityStatus(event.target.value)}
                            className={INPUT_CLASS}
                            {...describe("observation-status", fieldError("maturityStatus"))}
                        >
                            <option value="">Select a status</option>
                            {STATUS_OPTIONS.map((status) => (
                                <option key={status.code} value={status.code}>
                                    {status.label}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField id="observation-method" label="Method" error={fieldError("method")}>
                        <select
                            id="observation-method"
                            value={method}
                            onChange={(event) => changeMethod(event.target.value)}
                            className={INPUT_CLASS}
                            {...describe("observation-method", fieldError("method"))}
                        >
                            <option value="">Select a method</option>
                            {METHOD_OPTIONS.map((option) => (
                                <option key={option.code} value={option.code}>
                                    {option.label}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField
                        id="observation-value"
                        label={unit ? `Measured value (${unit})` : "Measured value"}
                        error={fieldError("measuredValue")}
                        note={takesValue ? "Optional. Above 0 and up to 100, one decimal at most." : "With Brix or dry matter only."}
                    >
                        <input
                            id="observation-value"
                            type="number"
                            min="0"
                            max="100"
                            step="0.1"
                            inputMode="decimal"
                            disabled={!takesValue}
                            value={measuredValue}
                            onChange={changeMeasuredValue}
                            className={INPUT_CLASS}
                            {...describe("observation-value", fieldError("measuredValue"), "observation-value-note")}
                        />
                    </FormField>

                    <FormField id="observation-observer" label="Observer" error={fieldError("observer")}
                        note="Optional." className="sm:col-span-2">
                        <input
                            id="observation-observer"
                            type="text"
                            value={observer}
                            onChange={(event) => setObserver(event.target.value)}
                            className={INPUT_CLASS}
                            {...describe("observation-observer", fieldError("observer"), "observation-observer-note")}
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
                        disabled={isSubmitting || varietyNote !== null}
                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                    >
                        {isSubmitting ? "Recording…" : "Record observation"}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
