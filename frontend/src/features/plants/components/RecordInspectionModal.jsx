import { useRef, useState } from "react";
import { Plus, Trash2, X } from "lucide-react";
import { useHealthIssues } from "../hooks/useHealthIssues";
import { formatCode } from "../utils/format";
import { issueOptions, OTHER_ISSUE_CODE } from "../utils/healthIssues";
import PlantsModal from "./PlantsModal";

const LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none";
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";
const SECONDARY_BUTTON_CLASS =
    "inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm font-semibold text-gray-700 hover:bg-gray-50";

// The API codes, sent as they are; the method may be left out.
const METHODS = ["VISUAL", "TRAP"];

/**
 * Same rules as the API: a block as for a planting, a whole score from 0 to
 * 100, a tree label of up to 20 letters, digits or hyphens, and a name of at
 * most 60 characters for "Other". Spaces around a code are removed by the API.
 */
const BLOCK_CODE_PATTERN = /^\s*[A-Za-z0-9]{1,10}\s*$/;
const SCORE_PATTERN = /^\d{1,3}$/;
const TREE_LABEL_PATTERN = /^\s*([A-Za-z0-9-]{1,20})?\s*$/;
const OTHER_LABEL_MAX_LENGTH = 60;

const TREE_NOTE = "Optional, such as 42 or R3-12. Empty for the whole block.";
const CATALOGUE_NOTE_ID = "inspection-catalogue-note";

/** Today as "YYYY-MM-DD", in local time: the latest inspection date the form accepts. */
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
 * One finding: the problem, its name when it is "Other", and the tree. In a
 * fieldset, so each field is read with "Finding 2" before it. A new finding
 * takes the focus on its problem.
 */
function FindingFields({ finding, number, options, catalogueNote, errorOf, onChange, onRemove }) {
    const id = `inspection-finding-${finding.key}`;
    const isOther = finding.issueCode === OTHER_ISSUE_CODE;

    return (
        <fieldset className="rounded-lg border border-gray-200 px-4 pb-4">
            <legend className="px-1 text-xs font-semibold tracking-wide text-gray-600 uppercase">
                {`Finding ${number}`}
            </legend>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <FormField id={`${id}-problem`} label="Problem" error={errorOf("issueCode")}>
                    <select
                        id={`${id}-problem`}
                        autoFocus
                        value={finding.issueCode}
                        onChange={(event) => onChange("issueCode", event.target.value)}
                        className={INPUT_CLASS}
                        {...describe(`${id}-problem`, errorOf("issueCode"), catalogueNote ? CATALOGUE_NOTE_ID : null)}
                    >
                        {/* Empty value: sending without a choice gives "Problem is required." */}
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

                {isOther && (
                    <FormField id={`${id}-other-label`} label="Name of the problem" error={errorOf("otherLabel")}>
                        <input
                            id={`${id}-other-label`}
                            type="text"
                            value={finding.otherLabel}
                            onChange={(event) => onChange("otherLabel", event.target.value)}
                            className={INPUT_CLASS}
                            {...describe(`${id}-other-label`, errorOf("otherLabel"))}
                        />
                    </FormField>
                )}

                <FormField id={`${id}-tree`} label="Tree" error={errorOf("treeLabel")} note={TREE_NOTE}>
                    <input
                        id={`${id}-tree`}
                        type="text"
                        value={finding.treeLabel}
                        onChange={(event) => onChange("treeLabel", event.target.value)}
                        className={INPUT_CLASS}
                        {...describe(`${id}-tree`, errorOf("treeLabel"), `${id}-tree-note`)}
                    />
                </FormField>
            </div>

            <div className="mt-3 flex justify-end">
                <button
                    type="button"
                    onClick={onRemove}
                    aria-label={`Remove finding ${number}`}
                    className={SECONDARY_BUTTON_CLASS}
                >
                    <Trash2 size={14} />
                    Remove
                </button>
            </div>
        </fieldset>
    );
}

/**
 * Form of an inspection of a block: its date, health score, observer and
 * method, and the problems found, none or several, recorded together.
 *
 * The block is typed, as an inspection needs no planting; the known blocks
 * are offered as suggestions. No farm is sent, as for a planting. The
 * problems are those of the catalogue, then "Other" once and last; "Other"
 * asks for the name of the problem.
 *
 * The checks mirror the API rules; what the API still refuses is shown the
 * same way as for a planting:
 * - a 400 with fieldErrors: each message under its field, a finding's under
 *   the field of that finding;
 * - a 422 (a problem not in the catalogue), or any other error: one message
 *   above the form.
 */
export default function RecordInspectionModal({ blocks, isSubmitting, error, onSubmit, onResetError, onClose }) {
    const [blockCode, setBlockCode] = useState("");
    const [inspectedOn, setInspectedOn] = useState("");
    const [healthScorePct, setHealthScorePct] = useState("");
    // A number field holds "" for text it cannot read, such as "1e"; this
    // keeps such a value from passing as an empty one.
    const [isScoreUnreadable, setIsScoreUnreadable] = useState(false);
    const [observer, setObserver] = useState("");
    const [method, setMethod] = useState("");
    const [findings, setFindings] = useState([]);
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);
    // Each finding keeps its key when an earlier one is removed.
    const nextKey = useRef(1);
    const addButtonRef = useRef(null);

    const issues = useHealthIssues();
    const options = issueOptions(issues.data);
    let catalogueNote = null;
    if (issues.isPending) {
        catalogueNote = "Loading the pests and diseases…";
    } else if (issues.isError) {
        catalogueNote = "The pests and diseases could not be loaded: only “Other” can be picked.";
    } else if (options.groups.length === 0) {
        catalogueNote = "Only “Other” can be picked until a pest or disease is added to the catalogue.";
    }

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];
    // The checks here name a finding by its key, the API by its index.
    const findingError = (finding, index, field) =>
        fieldErrors[`finding-${finding.key}-${field}`] ?? serverFieldErrors?.[`findings[${index}].${field}`];

    function changeScore(event) {
        setHealthScorePct(event.target.value);
        setIsScoreUnreadable(event.target.validity.badInput);
    }

    function addFinding() {
        const key = nextKey.current;
        nextKey.current += 1;
        setFindings((current) => [...current, { key, issueCode: "", otherLabel: "", treeLabel: "" }]);
    }

    function changeFinding(key, field, value) {
        // A name goes with "Other" only: the API refuses one for any other problem.
        const dropsLabel = field === "issueCode" && value !== OTHER_ISSUE_CODE;
        setFindings((current) =>
            current.map((finding) => {
                if (finding.key !== key) return finding;
                return dropsLabel ? { ...finding, issueCode: value, otherLabel: "" } : { ...finding, [field]: value };
            }));
        if (dropsLabel) {
            setFieldErrors((current) =>
                Object.fromEntries(Object.entries(current).filter(([field]) => field !== `finding-${key}-otherLabel`)));
        }
    }

    function removeFinding(key) {
        setFindings((current) => current.filter((finding) => finding.key !== key));
        setFieldErrors((current) =>
            Object.fromEntries(Object.entries(current).filter(([field]) => !field.startsWith(`finding-${key}-`))));
        // The API names the findings by index, which the removal shifts: its answer no longer applies.
        onResetError();
        addButtonRef.current?.focus();
    }

    function validate() {
        const errors = {};
        if (!blockCode.trim()) {
            errors.blockCode = "Block is required.";
        } else if (!BLOCK_CODE_PATTERN.test(blockCode)) {
            errors.blockCode = "Block must be a short code such as A or B2, without prefix or space.";
        }
        if (!inspectedOn) {
            errors.inspectedOn = "Inspection date is required.";
        } else if (inspectedOn > today) {
            errors.inspectedOn = "Inspection date cannot be in the future.";
        }
        const score = healthScorePct.trim();
        if (!isScoreUnreadable && score === "") {
            errors.healthScorePct = "Health score is required.";
        } else if (isScoreUnreadable || !SCORE_PATTERN.test(score) || Number(score) > 100) {
            errors.healthScorePct = "Health score must be a whole number from 0 to 100.";
        }
        if (observer.length > 255) {
            errors.observer = "Observer must be at most 255 characters.";
        }
        for (const finding of findings) {
            const prefix = `finding-${finding.key}-`;
            if (!finding.issueCode) {
                errors[`${prefix}issueCode`] = "Problem is required.";
            } else if (finding.issueCode === OTHER_ISSUE_CODE) {
                if (!finding.otherLabel.trim()) {
                    errors[`${prefix}otherLabel`] = "Name of the problem is required with “Other”.";
                } else if (finding.otherLabel.length > OTHER_LABEL_MAX_LENGTH) {
                    errors[`${prefix}otherLabel`] =
                        `Name of the problem must be at most ${OTHER_LABEL_MAX_LENGTH} characters.`;
                }
            }
            if (!TREE_LABEL_PATTERN.test(finding.treeLabel)) {
                errors[`${prefix}treeLabel`] = "Tree must be up to 20 letters, digits or hyphens, such as 42 or R3-12.";
            }
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        // Optional values left empty are not sent at all.
        const inspection = {
            blockCode,
            inspectedOn,
            healthScorePct: Number(healthScorePct.trim()),
            findings: findings.map((finding) => {
                const found = { issueCode: finding.issueCode };
                if (finding.issueCode === OTHER_ISSUE_CODE) found.otherLabel = finding.otherLabel;
                if (finding.treeLabel.trim()) found.treeLabel = finding.treeLabel;
                return found;
            }),
        };
        if (observer.trim()) inspection.observer = observer;
        if (method) inspection.method = method;
        onSubmit(inspection);
    }

    return (
        <PlantsModal labelledBy="record-inspection-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h3 id="record-inspection-title" className="font-heading text-xl font-bold text-gray-900">
                    Record an inspection
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
                    <FormField id="inspection-block" label="Block" error={fieldError("blockCode")}
                        note="Such as A or B2. The block needs no planting.">
                        <input
                            id="inspection-block"
                            type="text"
                            autoFocus
                            list="inspection-block-options"
                            value={blockCode}
                            onChange={(event) => setBlockCode(event.target.value)}
                            className={INPUT_CLASS}
                            {...describe("inspection-block", fieldError("blockCode"), "inspection-block-note")}
                        />
                        <datalist id="inspection-block-options">
                            {blocks.map((block) => (
                                <option key={block} value={block} />
                            ))}
                        </datalist>
                    </FormField>

                    <FormField id="inspection-date" label="Inspection date" error={fieldError("inspectedOn")}>
                        <input
                            id="inspection-date"
                            type="date"
                            max={today}
                            value={inspectedOn}
                            onChange={(event) => setInspectedOn(event.target.value)}
                            className={INPUT_CLASS}
                            {...describe("inspection-date", fieldError("inspectedOn"))}
                        />
                    </FormField>

                    <FormField id="inspection-score" label="Health score (%)" error={fieldError("healthScorePct")}
                        note="A whole number from 0 to 100.">
                        <input
                            id="inspection-score"
                            type="number"
                            min="0"
                            max="100"
                            step="1"
                            inputMode="numeric"
                            value={healthScorePct}
                            onChange={changeScore}
                            className={INPUT_CLASS}
                            {...describe("inspection-score", fieldError("healthScorePct"), "inspection-score-note")}
                        />
                    </FormField>

                    <FormField id="inspection-method" label="Method" error={fieldError("method")}>
                        <select
                            id="inspection-method"
                            value={method}
                            onChange={(event) => setMethod(event.target.value)}
                            className={INPUT_CLASS}
                            {...describe("inspection-method", fieldError("method"))}
                        >
                            <option value="">Not specified</option>
                            {METHODS.map((code) => (
                                <option key={code} value={code}>
                                    {formatCode(code)}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField id="inspection-observer" label="Observer" error={fieldError("observer")}
                        note="Optional." className="sm:col-span-2">
                        <input
                            id="inspection-observer"
                            type="text"
                            value={observer}
                            onChange={(event) => setObserver(event.target.value)}
                            className={INPUT_CLASS}
                            {...describe("inspection-observer", fieldError("observer"), "inspection-observer-note")}
                        />
                    </FormField>
                </div>

                <fieldset className="mt-6">
                    <legend className={LABEL_CLASS}>Findings</legend>
                    <p className={NOTE_CLASS}>
                        {findings.length === 0
                            ? "None: the inspection records a visit with nothing found."
                            : "Each problem found, on the whole block or on one tree."}
                    </p>
                    {catalogueNote && (
                        <p id={CATALOGUE_NOTE_ID} className={NOTE_CLASS}>
                            {catalogueNote}
                        </p>
                    )}
                    {fieldError("findings") && <p className={ERROR_CLASS}>{fieldError("findings")}</p>}

                    {findings.length > 0 && (
                        <div className="mt-3 flex flex-col gap-4">
                            {findings.map((finding, index) => (
                                <FindingFields
                                    key={finding.key}
                                    finding={finding}
                                    number={index + 1}
                                    options={options}
                                    catalogueNote={catalogueNote}
                                    errorOf={(field) => findingError(finding, index, field)}
                                    onChange={(field, value) => changeFinding(finding.key, field, value)}
                                    onRemove={() => removeFinding(finding.key)}
                                />
                            ))}
                        </div>
                    )}

                    <button
                        ref={addButtonRef}
                        type="button"
                        onClick={addFinding}
                        className={`mt-3 ${SECONDARY_BUTTON_CLASS}`}
                    >
                        <Plus size={16} />
                        Add a finding
                    </button>
                </fieldset>

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
                        {isSubmitting ? "Recording…" : "Record inspection"}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
