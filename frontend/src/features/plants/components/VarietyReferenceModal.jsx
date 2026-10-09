import { useState } from "react";
import { X } from "lucide-react";
import { monthName } from "../utils/format";
import PlantsModal from "./PlantsModal";

const LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none";
// The default placeholder is the text colour at half opacity, about 3:1 on
// white; gray-500 (#6a7282) reaches 4.84:1.
const TEXT_INPUT_CLASS = `${INPUT_CLASS} placeholder:text-gray-500`;
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

const MONTHS = Array.from({ length: 12 }, (_, index) => index + 1);

/** Same rule as the API: above 0 and at most 1000 kg, with at most 1 decimal. */
const YIELD_PATTERN = /^\d{1,4}(\.\d)?$/;
const YIELD_ERROR = "Yield per tree must be greater than 0 and at most 1,000 kg, with at most 1 decimal.";

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

function MonthSelect({ id, value, onChange, fieldProps }) {
    return (
        <select id={id} value={value} onChange={(event) => onChange(event.target.value)} className={INPUT_CLASS}
            {...fieldProps}>
            {/* Empty value: sending without a choice gives the "required" message. */}
            <option value="">Select a month</option>
            {MONTHS.map((month) => (
                <option key={month} value={month}>
                    {monthName(month)}
                </option>
            ))}
        </select>
    );
}

/**
 * Note under a source field. A new reference records an empty source as
 * user_entry; a correction keeps the recorded source while its value stays
 * the same, as the API does.
 */
function sourceNote(recordedSource, unchangedValue) {
    return recordedSource
        ? `Leave empty to keep “${recordedSource}” if ${unchangedValue}; otherwise user_entry is recorded.`
        : "Optional. user_entry is recorded when left empty.";
}

/**
 * Form of the reference of a variety: its yield per tree and its harvest
 * months, each with an optional source. `reference` is the recorded one to
 * correct, or null to add a variety.
 *
 * The checks mirror the API rules; what the API still refuses is shown under
 * its field: a 400 under each field it names, a 409 (the name is taken) under
 * the name. Any other error is shown above the form.
 */
export default function VarietyReferenceModal({ reference, isSubmitting, error, onSubmit, onClose }) {
    const isEditing = reference !== null;
    const [varietyName, setVarietyName] = useState(reference?.varietyName ?? "");
    const [yieldPerTreeKg, setYieldPerTreeKg] = useState(isEditing ? String(reference.yieldPerTreeKg) : "");
    // A number field holds "" for text it cannot read, such as "1e"; this
    // keeps such a value from passing as an empty field.
    const [isYieldUnreadable, setIsYieldUnreadable] = useState(false);
    const [harvestStartMonth, setHarvestStartMonth] = useState(isEditing ? String(reference.harvestStartMonth) : "");
    const [harvestEndMonth, setHarvestEndMonth] = useState(isEditing ? String(reference.harvestEndMonth) : "");
    // The sources start empty: on a correction, empty keeps the recorded one while its value stays the same.
    const [yieldSource, setYieldSource] = useState("");
    const [seasonSource, setSeasonSource] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const nameConflict = error?.status === 409 ? error.message : null;
    const formError = error && !serverFieldErrors && !nameConflict ? error.message : null;
    const fieldError = (field) =>
        fieldErrors[field] ?? serverFieldErrors?.[field] ?? (field === "varietyName" ? nameConflict : null);
    const fieldProps = (field, id, hasNote = false) => ({
        "aria-invalid": fieldError(field) ? true : undefined,
        "aria-describedby":
            [fieldError(field) ? `${id}-error` : null, hasNote ? `${id}-note` : null]
                .filter(Boolean)
                .join(" ") || undefined,
    });

    function changeYield(event) {
        setYieldPerTreeKg(event.target.value);
        setIsYieldUnreadable(event.target.validity.badInput);
    }

    function validate() {
        const errors = {};
        if (!varietyName.trim()) {
            errors.varietyName = "Variety is required.";
        } else if (varietyName.length > 255) {
            errors.varietyName = "Variety must be at most 255 characters.";
        }
        const yieldText = yieldPerTreeKg.trim();
        if (!isYieldUnreadable && yieldText === "") {
            errors.yieldPerTreeKg = "Yield per tree is required.";
        } else if (isYieldUnreadable || !YIELD_PATTERN.test(yieldText)
            || Number(yieldText) <= 0 || Number(yieldText) > 1000) {
            errors.yieldPerTreeKg = YIELD_ERROR;
        }
        if (!harvestStartMonth) {
            errors.harvestStartMonth = "First harvest month is required.";
        }
        if (!harvestEndMonth) {
            errors.harvestEndMonth = "Last harvest month is required.";
        }
        if (yieldSource.length > 255) {
            errors.yieldSource = "Yield source must be at most 255 characters.";
        }
        if (seasonSource.length > 255) {
            errors.seasonSource = "Season source must be at most 255 characters.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        // Sources left empty are not sent at all.
        const values = {
            varietyName,
            yieldPerTreeKg: Number(yieldPerTreeKg.trim()),
            harvestStartMonth: Number(harvestStartMonth),
            harvestEndMonth: Number(harvestEndMonth),
        };
        if (yieldSource.trim()) values.yieldSource = yieldSource;
        if (seasonSource.trim()) values.seasonSource = seasonSource;
        onSubmit(values);
    }

    return (
        <PlantsModal labelledBy="variety-reference-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h3 id="variety-reference-title" className="font-heading text-xl font-bold text-gray-900">
                    {isEditing ? `Edit ${reference.varietyName}` : "Add a variety"}
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
                    <FormField id="reference-variety" label="Variety" error={fieldError("varietyName")}
                        note="Its plantings find it ignoring case and accents." className="sm:col-span-2">
                        <input
                            id="reference-variety"
                            type="text"
                            autoFocus
                            value={varietyName}
                            onChange={(event) => setVarietyName(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("varietyName", "reference-variety", true)}
                        />
                    </FormField>

                    <FormField id="reference-yield" label="Yield per tree (kg)" error={fieldError("yieldPerTreeKg")}
                        note="In full production, per year.">
                        <input
                            id="reference-yield"
                            type="number"
                            min="0"
                            max="1000"
                            step="any"
                            inputMode="decimal"
                            value={yieldPerTreeKg}
                            onChange={changeYield}
                            className={INPUT_CLASS}
                            {...fieldProps("yieldPerTreeKg", "reference-yield", true)}
                        />
                    </FormField>

                    <FormField id="reference-yield-source" label="Yield source" error={fieldError("yieldSource")}
                        note={sourceNote(reference?.yieldSource, "the yield does not change")}>
                        <input
                            id="reference-yield-source"
                            type="text"
                            value={yieldSource}
                            onChange={(event) => setYieldSource(event.target.value)}
                            placeholder={reference?.yieldSource ?? "user_entry"}
                            className={TEXT_INPUT_CLASS}
                            {...fieldProps("yieldSource", "reference-yield-source", true)}
                        />
                    </FormField>

                    <FormField id="reference-start-month" label="First harvest month"
                        error={fieldError("harvestStartMonth")}>
                        <MonthSelect
                            id="reference-start-month"
                            value={harvestStartMonth}
                            onChange={setHarvestStartMonth}
                            fieldProps={fieldProps("harvestStartMonth", "reference-start-month")}
                        />
                    </FormField>

                    <FormField id="reference-end-month" label="Last harvest month" error={fieldError("harvestEndMonth")}
                        note="Before the first month for a season over the new year.">
                        <MonthSelect
                            id="reference-end-month"
                            value={harvestEndMonth}
                            onChange={setHarvestEndMonth}
                            fieldProps={fieldProps("harvestEndMonth", "reference-end-month", true)}
                        />
                    </FormField>

                    <FormField id="reference-season-source" label="Season source" error={fieldError("seasonSource")}
                        note={sourceNote(reference?.seasonSource, "the months do not change")}
                        className="sm:col-span-2">
                        <input
                            id="reference-season-source"
                            type="text"
                            value={seasonSource}
                            onChange={(event) => setSeasonSource(event.target.value)}
                            placeholder={reference?.seasonSource ?? "user_entry"}
                            className={TEXT_INPUT_CLASS}
                            {...fieldProps("seasonSource", "reference-season-source", true)}
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
                        {isEditing ? (isSubmitting ? "Saving…" : "Save") : (isSubmitting ? "Adding…" : "Add variety")}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
