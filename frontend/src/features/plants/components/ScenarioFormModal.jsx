import { useState } from "react";
import { X } from "lucide-react";
import { useVarietyReferences } from "../hooks/useVarietyReferences";
import { formatKilograms, formatText } from "../utils/format";
import {
    SCENARIO_CURRENCIES,
    sameVarietyName,
    scenarioFormValues,
    scenarioRequestBody,
    SOURCE_FIELDS,
    validateScenarioForm,
} from "../utils/scenarioRequests";
import PlantsModal from "./PlantsModal";

const LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none";
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

/** The currencies as the radio buttons name them. */
const CURRENCY_LABELS = { XOF: "XOF (FCFA)", EUR: "EUR (€)" };

/** The values of a scenario, in the order of the form, with what they are. */
const VALUE_FIELDS = [
    { field: "treeCount", label: "Trees", required: true, inputMode: "numeric", note: "All planted before year 1." },
    {
        field: "yieldPerTreeKg",
        label: "Yield per tree (kg per year, full production)",
        required: true,
        inputMode: "decimal",
    },
    {
        field: "lossRate",
        label: "Loss rate (0 to 1)",
        required: false,
        inputMode: "decimal",
        note: "Share of the harvest lost before sale, 0.15 for 15%. Empty: the figures are before losses.",
    },
    {
        field: "pricePerKg",
        label: "Price per kg",
        required: false,
        inputMode: "decimal",
        note: "Selling price of fresh mangoes. Empty: no revenue is computed.",
    },
    { field: "annualFixedCost", label: "Fixed operating cost (per year)", required: true, inputMode: "decimal" },
    {
        field: "variableCostPerKg",
        label: "Variable operating cost (per kg sold)",
        required: false,
        inputMode: "decimal",
        note: "Such as packaging or transport. Empty: the operating cost is the fixed cost alone.",
    },
    { field: "initialInvestment", label: "Investment", required: true, inputMode: "decimal" },
    {
        field: "costPerAdditionalTree",
        label: "Cost per added tree",
        required: false,
        inputMode: "decimal",
        note: "Plant and planting, for a variant with more trees.",
    },
];

/** The fields of the form, by the id of their input. */
function inputId(field) {
    return `scenario-${field}`;
}

function FormField({ id, label, required, error, notes, children }) {
    return (
        <div>
            <label htmlFor={id} className={LABEL_CLASS}>
                {label}
                {!required && <span className="font-normal normal-case"> (optional)</span>}
            </label>
            {children}
            {error && (
                <p id={`${id}-error`} className={ERROR_CLASS}>
                    {error}
                </p>
            )}
            {notes}
        </div>
    );
}

/**
 * Form of a planting scenario: its name, currency and assumptions, each value
 * with an optional source. `scenario` is the recorded one to correct, or null
 * to enter a new one.
 *
 * The checks mirror the API's 400 rules; what the API still refuses is shown
 * under the field it names, and a name already taken (409), like any other
 * error, above the form. The sources start empty: a new value is recorded
 * with user_entry, and a correction keeps the recorded source of a value that
 * does not change. When the variety has an agronomic reference, its yield per
 * tree is offered, with its source; "Use this value" copies both as they are.
 */
export default function ScenarioFormModal({ scenario, isSubmitting, error, onSubmit, onClose }) {
    const isEditing = scenario !== null;
    const [values, setValues] = useState(() => scenarioFormValues(scenario));
    const [fieldErrors, setFieldErrors] = useState({});
    const { data: references } = useVarietyReferences();

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field] ?? null;
    const reference = (references ?? []).find((candidate) => sameVarietyName(candidate.varietyName, values.varietyName));
    const title = isEditing ? `Edit ${scenario.name}` : "Enter a scenario";

    function setValue(field, value) {
        setValues((current) => ({ ...current, [field]: value }));
    }

    function copyReferenceYield() {
        setValues((current) => ({
            ...current,
            yieldPerTreeKg: String(reference.yieldPerTreeKg),
            yieldSource: reference.yieldSource ?? "",
        }));
    }

    function describedBy(id, field, noteIds) {
        return [fieldError(field) ? `${id}-error` : null, ...noteIds].filter(Boolean).join(" ") || undefined;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validateScenarioForm(values);
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit(scenarioRequestBody(values));
    }

    function valueNotes(field, note) {
        const id = inputId(field);
        return (
            <>
                {note && (
                    <p id={`${id}-note`} className={NOTE_CLASS}>
                        {note}
                    </p>
                )}
                {field === "yieldPerTreeKg" && reference && (
                    <div id={`${id}-reference`} className="mt-1 flex flex-wrap items-center gap-2 text-xs text-gray-600">
                        <span>
                            {`Reference of ${reference.varietyName}: ${formatKilograms(reference.yieldPerTreeKg)} `
                                + `(${formatText(reference.yieldSource)}).`}
                        </span>
                        <button
                            type="button"
                            onClick={copyReferenceYield}
                            className="rounded-md border border-gray-200 px-2 py-0.5 font-semibold text-gray-700 hover:bg-gray-50"
                        >
                            Use this value
                        </button>
                    </div>
                )}
            </>
        );
    }

    function sourceNote(sourceField) {
        const recorded = isEditing ? scenario[sourceField] : null;
        if (!recorded) return null;
        return (
            <p id={`${inputId(sourceField)}-note`} className={NOTE_CLASS}>
                {`Recorded: ${recorded}`}
            </p>
        );
    }

    return (
        <PlantsModal labelledBy="scenario-form-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h3 id="scenario-form-title" className="font-heading text-xl font-bold text-gray-900">
                    {title}
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

                <p className="mb-5 text-sm text-gray-600">
                    Every result is computed by the server from these values. A source is optional: left empty,
                    user_entry is recorded for a new value, and the recorded source stays while its value does not
                    change.
                </p>

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <FormField id={inputId("name")} label="Name" required error={fieldError("name")}>
                        <input
                            id={inputId("name")}
                            type="text"
                            autoFocus
                            value={values.name}
                            onChange={(event) => setValue("name", event.target.value)}
                            className={INPUT_CLASS}
                            aria-required="true"
                            aria-invalid={fieldError("name") ? true : undefined}
                            aria-describedby={describedBy(inputId("name"), "name", [])}
                        />
                    </FormField>

                    <FormField id={inputId("varietyName")} label="Variety" error={fieldError("varietyName")}>
                        <input
                            id={inputId("varietyName")}
                            type="text"
                            list="scenario-variety-options"
                            value={values.varietyName}
                            onChange={(event) => setValue("varietyName", event.target.value)}
                            className={INPUT_CLASS}
                            aria-invalid={fieldError("varietyName") ? true : undefined}
                            aria-describedby={describedBy(inputId("varietyName"), "varietyName", [])}
                        />
                        <datalist id="scenario-variety-options">
                            {(references ?? []).map((candidate) => (
                                <option key={candidate.id} value={candidate.varietyName} />
                            ))}
                        </datalist>
                    </FormField>

                    <fieldset className="sm:col-span-2">
                        <legend className={LABEL_CLASS}>Currency</legend>
                        <div className="mt-2 flex flex-wrap gap-x-6 gap-y-2">
                            {SCENARIO_CURRENCIES.map((currency) => (
                                <label key={currency} className="flex items-center gap-2 text-sm text-gray-800">
                                    <input
                                        type="radio"
                                        name="scenario-currency"
                                        value={currency}
                                        checked={values.currency === currency}
                                        onChange={() => setValue("currency", currency)}
                                        aria-invalid={fieldError("currency") ? true : undefined}
                                        aria-describedby={fieldError("currency") ? "scenario-currency-error" : "scenario-currency-note"}
                                        className="accent-primary"
                                    />
                                    {CURRENCY_LABELS[currency]}
                                </label>
                            ))}
                        </div>
                        {fieldError("currency") && (
                            <p id="scenario-currency-error" className={ERROR_CLASS}>
                                {fieldError("currency")}
                            </p>
                        )}
                        <p id="scenario-currency-note" className={NOTE_CLASS}>
                            Every amount below is in this currency.
                        </p>
                    </fieldset>

                    {VALUE_FIELDS.map(({ field, label, required, inputMode, note }) => {
                        const id = inputId(field);
                        const sourceField = SOURCE_FIELDS[field];
                        const sourceId = inputId(sourceField);
                        const noteIds = [
                            note ? `${id}-note` : null,
                            field === "yieldPerTreeKg" && reference ? `${id}-reference` : null,
                        ];
                        const recordedSource = isEditing ? scenario[sourceField] : null;
                        return (
                            <div key={field} className="contents">
                                <FormField
                                    id={id}
                                    label={label}
                                    required={required}
                                    error={fieldError(field)}
                                    notes={valueNotes(field, note)}
                                >
                                    <input
                                        id={id}
                                        type="text"
                                        inputMode={inputMode}
                                        value={values[field]}
                                        onChange={(event) => setValue(field, event.target.value)}
                                        className={INPUT_CLASS}
                                        aria-required={required ? "true" : undefined}
                                        aria-invalid={fieldError(field) ? true : undefined}
                                        aria-describedby={describedBy(id, field, noteIds)}
                                    />
                                </FormField>

                                <FormField
                                    id={sourceId}
                                    label={`Source of ${label.split(" (")[0].toLowerCase()}`}
                                    error={fieldError(sourceField)}
                                    notes={sourceNote(sourceField)}
                                >
                                    <input
                                        id={sourceId}
                                        type="text"
                                        value={values[sourceField]}
                                        onChange={(event) => setValue(sourceField, event.target.value)}
                                        className={INPUT_CLASS}
                                        aria-invalid={fieldError(sourceField) ? true : undefined}
                                        aria-describedby={describedBy(sourceId, sourceField,
                                            [recordedSource ? `${sourceId}-note` : null])}
                                    />
                                </FormField>
                            </div>
                        );
                    })}
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
                        {isSubmitting ? "Saving…" : "Save scenario"}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
