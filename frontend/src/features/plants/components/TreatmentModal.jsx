import { useState } from "react";
import { X } from "lucide-react";
import { useHealthIssues } from "../hooks/useHealthIssues";
import { formatDate } from "../utils/format";
import { issueOptions, OTHER_ISSUE_CODE } from "../utils/healthIssues";
import FindingSummary from "./FindingSummary";
import PlantsModal from "./PlantsModal";

const LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none";
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

// The API codes, sent as they are; only their labels are shown.
const UNITS = [
    { code: "KG", label: "kg" },
    { code: "L", label: "L" },
];

/**
 * Same rules as the API: a block as for a planting, a quantity above 0 with at
 * most 3 decimals, a whole number of days, and a name of at most 60 characters
 * for "Other".
 */
const BLOCK_CODE_PATTERN = /^\s*[A-Za-z0-9]{1,10}\s*$/;
const QUANTITY_PATTERN = /^\d{1,9}(\.\d{1,3})?$/;
const DAYS_PATTERN = /^\d{1,9}$/;
const OTHER_LABEL_MAX_LENGTH = 60;

const INITIAL_VALUES = {
    blockCode: "",
    targetIssueCode: "",
    targetOtherLabel: "",
    treatedOn: "",
    preHarvestIntervalDays: "",
    productName: "",
    activeIngredient: "",
    quantity: "",
    unit: "",
    applicator: "",
    equipment: "",
};

/** Today as "YYYY-MM-DD", in local time: the latest treatment date the form accepts. */
function todayAsIsoDate() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${now.getFullYear()}-${month}-${day}`;
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

/** A required text of at most 255 characters, as the API checks it. */
function checkText(value, label) {
    if (!value.trim()) return `${label} is required.`;
    if (value.length > 255) return `${label} must be at most 255 characters.`;
    return null;
}

/**
 * Form of a plant protection treatment. `finding` is the finding it answers,
 * or null for a preventive treatment.
 *
 * For a finding, the API copies the block and the problem targeted from it, so
 * the form only recalls them; the treatment cannot predate the inspection. A
 * preventive treatment names its block, typed as for an inspection, and its
 * problem: the catalogue, then "Other" once and last, with the name of the
 * problem. The date from which harvesting is allowed is computed by the API.
 *
 * The checks mirror the API rules; what the API still refuses is shown the
 * same way as for a planting:
 * - a 400 with fieldErrors: each message under its field;
 * - a 404 or a 422 (the finding is resolved, or the treatment predates its
 *   inspection), or any other error: one message above the form.
 */
export default function TreatmentModal({ finding, blocks, isSubmitting, error, onSubmit, onClose }) {
    const isPreventive = finding === null;
    const [values, setValues] = useState(INITIAL_VALUES);
    // A number field holds "" for text it cannot read, such as "1e"; these keep
    // such a value from passing as an empty one.
    const [unreadable, setUnreadable] = useState({ quantity: false, preHarvestIntervalDays: false });
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);

    const issues = useHealthIssues();
    const options = issueOptions(issues.data);
    let catalogueNote = null;
    if (isPreventive && issues.isPending) {
        catalogueNote = "Loading the pests and diseases…";
    } else if (isPreventive && issues.isError) {
        catalogueNote = "The pests and diseases could not be loaded: only “Other” can be picked.";
    } else if (isPreventive && options.groups.length === 0) {
        catalogueNote = "Only “Other” can be picked until a pest or disease is added to the catalogue.";
    }
    const dateNote = isPreventive ? null : `Not before the inspection, on ${formatDate(finding.inspectedOn)}.`;

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];
    const fieldProps = (field, id, note = null) => ({
        "aria-invalid": fieldError(field) ? true : undefined,
        "aria-describedby":
            [fieldError(field) ? `${id}-error` : null, note ? `${id}-note` : null].filter(Boolean).join(" ")
            || undefined,
    });

    function setValue(field, value) {
        setValues((current) => ({ ...current, [field]: value }));
    }

    function setNumber(field, event) {
        setValue(field, event.target.value);
        setUnreadable((current) => ({ ...current, [field]: event.target.validity.badInput }));
    }

    function changeTarget(code) {
        // A name goes with "Other" only: the API refuses one for any other problem.
        setValues((current) => ({
            ...current,
            targetIssueCode: code,
            targetOtherLabel: code === OTHER_ISSUE_CODE ? current.targetOtherLabel : "",
        }));
        if (code !== OTHER_ISSUE_CODE) {
            setFieldErrors((current) =>
                Object.fromEntries(Object.entries(current).filter(([field]) => field !== "targetOtherLabel")));
        }
    }

    function validate() {
        const errors = {};
        if (isPreventive) {
            if (!values.blockCode.trim()) {
                errors.blockCode = "Block is required.";
            } else if (!BLOCK_CODE_PATTERN.test(values.blockCode)) {
                errors.blockCode = "Block must be a short code such as A or B2, without prefix or space.";
            }
            if (!values.targetIssueCode) {
                errors.targetIssueCode = "Problem targeted is required.";
            } else if (values.targetIssueCode === OTHER_ISSUE_CODE) {
                if (!values.targetOtherLabel.trim()) {
                    errors.targetOtherLabel = "Name of the problem is required with “Other”.";
                } else if (values.targetOtherLabel.length > OTHER_LABEL_MAX_LENGTH) {
                    errors.targetOtherLabel = `Name of the problem must be at most ${OTHER_LABEL_MAX_LENGTH} characters.`;
                }
            }
        }
        if (!values.treatedOn) {
            errors.treatedOn = "Treatment date is required.";
        } else if (values.treatedOn > today) {
            errors.treatedOn = "Treatment date cannot be in the future.";
        } else if (!isPreventive && values.treatedOn < finding.inspectedOn) {
            errors.treatedOn = `Treatment date cannot be before the inspection, on ${formatDate(finding.inspectedOn)}.`;
        }
        const days = values.preHarvestIntervalDays.trim();
        if (!unreadable.preHarvestIntervalDays && days === "") {
            errors.preHarvestIntervalDays = "Pre-harvest interval is required.";
        } else if (unreadable.preHarvestIntervalDays || !DAYS_PATTERN.test(days)) {
            errors.preHarvestIntervalDays = "Pre-harvest interval must be a whole number of days, 0 or more.";
        }
        const productError = checkText(values.productName, "Product");
        if (productError) errors.productName = productError;
        const ingredientError = checkText(values.activeIngredient, "Active ingredient");
        if (ingredientError) errors.activeIngredient = ingredientError;
        const quantity = values.quantity.trim();
        if (unreadable.quantity || !QUANTITY_PATTERN.test(quantity) || Number(quantity) <= 0) {
            errors.quantity = "Quantity must be a number greater than 0, with at most 3 decimals.";
        }
        if (!values.unit) {
            errors.unit = "Unit is required.";
        }
        const applicatorError = checkText(values.applicator, "Applicator");
        if (applicatorError) errors.applicator = applicatorError;
        if (values.equipment.length > 255) {
            errors.equipment = "Equipment must be at most 255 characters.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        // Optional values left empty are not sent at all.
        const treatment = {
            treatedOn: values.treatedOn,
            productName: values.productName,
            activeIngredient: values.activeIngredient,
            quantity: Number(values.quantity.trim()),
            unit: values.unit,
            preHarvestIntervalDays: Number(values.preHarvestIntervalDays.trim()),
            applicator: values.applicator,
        };
        if (values.equipment.trim()) treatment.equipment = values.equipment;
        if (isPreventive) {
            treatment.blockCode = values.blockCode;
            treatment.targetIssueCode = values.targetIssueCode;
            if (values.targetIssueCode === OTHER_ISSUE_CODE) treatment.targetOtherLabel = values.targetOtherLabel;
        }
        onSubmit(treatment);
    }

    return (
        <PlantsModal labelledBy="treatment-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h3 id="treatment-title" className="font-heading text-xl font-bold text-gray-900">
                    {isPreventive ? "Record a preventive treatment" : "Treat a finding"}
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

                {!isPreventive && <FindingSummary finding={finding} />}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    {isPreventive && (
                        <>
                            <FormField id="treatment-block" label="Block" error={fieldError("blockCode")}
                                note="Such as A or B2. The block needs no planting.">
                                <input
                                    id="treatment-block"
                                    type="text"
                                    autoFocus
                                    list="treatment-block-options"
                                    value={values.blockCode}
                                    onChange={(event) => setValue("blockCode", event.target.value)}
                                    className={INPUT_CLASS}
                                    {...fieldProps("blockCode", "treatment-block", true)}
                                />
                                <datalist id="treatment-block-options">
                                    {blocks.map((block) => (
                                        <option key={block} value={block} />
                                    ))}
                                </datalist>
                            </FormField>

                            <FormField id="treatment-target" label="Problem targeted" error={fieldError("targetIssueCode")}
                                note={catalogueNote}>
                                <select
                                    id="treatment-target"
                                    value={values.targetIssueCode}
                                    onChange={(event) => changeTarget(event.target.value)}
                                    className={INPUT_CLASS}
                                    {...fieldProps("targetIssueCode", "treatment-target", catalogueNote)}
                                >
                                    {/* Empty value: sending without a choice gives "Problem targeted is required." */}
                                    <option value="">Select a problem</option>
                                    {options.groups.map((group) => (
                                        <optgroup key={group.label} label={group.label}>
                                            {group.issues.map((issue) => (
                                                <option key={issue.code} value={issue.code}>
                                                    {issue.name}
                                                </option>
                                            ))}
                                        </optgroup>
                                    ))}
                                    <option value={options.other.code}>{options.other.name}</option>
                                </select>
                            </FormField>

                            {values.targetIssueCode === OTHER_ISSUE_CODE && (
                                <FormField id="treatment-other-label" label="Name of the problem"
                                    error={fieldError("targetOtherLabel")} className="sm:col-span-2">
                                    <input
                                        id="treatment-other-label"
                                        type="text"
                                        value={values.targetOtherLabel}
                                        onChange={(event) => setValue("targetOtherLabel", event.target.value)}
                                        className={INPUT_CLASS}
                                        {...fieldProps("targetOtherLabel", "treatment-other-label")}
                                    />
                                </FormField>
                            )}
                        </>
                    )}

                    <FormField id="treatment-date" label="Treatment date" error={fieldError("treatedOn")} note={dateNote}>
                        <input
                            id="treatment-date"
                            type="date"
                            autoFocus={!isPreventive}
                            min={isPreventive ? undefined : finding.inspectedOn}
                            max={today}
                            value={values.treatedOn}
                            onChange={(event) => setValue("treatedOn", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("treatedOn", "treatment-date", dateNote)}
                        />
                    </FormField>

                    <FormField id="treatment-interval" label="Pre-harvest interval (days)"
                        error={fieldError("preHarvestIntervalDays")} note="Whole days before the block can be harvested again.">
                        <input
                            id="treatment-interval"
                            type="number"
                            min="0"
                            step="1"
                            inputMode="numeric"
                            value={values.preHarvestIntervalDays}
                            onChange={(event) => setNumber("preHarvestIntervalDays", event)}
                            className={INPUT_CLASS}
                            {...fieldProps("preHarvestIntervalDays", "treatment-interval", true)}
                        />
                    </FormField>

                    <FormField id="treatment-product" label="Product" error={fieldError("productName")}
                        note="Trade name.">
                        <input
                            id="treatment-product"
                            type="text"
                            value={values.productName}
                            onChange={(event) => setValue("productName", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("productName", "treatment-product", true)}
                        />
                    </FormField>

                    <FormField id="treatment-ingredient" label="Active ingredient" error={fieldError("activeIngredient")}>
                        <input
                            id="treatment-ingredient"
                            type="text"
                            value={values.activeIngredient}
                            onChange={(event) => setValue("activeIngredient", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("activeIngredient", "treatment-ingredient")}
                        />
                    </FormField>

                    <FormField id="treatment-quantity" label="Quantity" error={fieldError("quantity")}>
                        <input
                            id="treatment-quantity"
                            type="number"
                            min="0"
                            step="any"
                            inputMode="decimal"
                            value={values.quantity}
                            onChange={(event) => setNumber("quantity", event)}
                            className={INPUT_CLASS}
                            {...fieldProps("quantity", "treatment-quantity")}
                        />
                    </FormField>

                    <FormField id="treatment-unit" label="Unit" error={fieldError("unit")}>
                        <select
                            id="treatment-unit"
                            value={values.unit}
                            onChange={(event) => setValue("unit", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("unit", "treatment-unit")}
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

                    <FormField id="treatment-applicator" label="Applicator" error={fieldError("applicator")}>
                        <input
                            id="treatment-applicator"
                            type="text"
                            value={values.applicator}
                            onChange={(event) => setValue("applicator", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("applicator", "treatment-applicator")}
                        />
                    </FormField>

                    <FormField id="treatment-equipment" label="Equipment" error={fieldError("equipment")} note="Optional.">
                        <input
                            id="treatment-equipment"
                            type="text"
                            value={values.equipment}
                            onChange={(event) => setValue("equipment", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("equipment", "treatment-equipment", true)}
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
                        {isSubmitting ? "Recording…" : "Record treatment"}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
