import { useState } from "react";
import { X } from "lucide-react";
import { formatCode } from "../utils/format";
import PlantsModal from "./PlantsModal";

const LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none";
// The default placeholder is the text colour at half opacity, about 3:1 on
// white; gray-500 (#6a7282) reaches 4.84:1.
const TEXT_INPUT_CLASS = `${INPUT_CLASS} placeholder:text-gray-500`;
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

// The API codes, sent as they are; "Other" is not one of them, the API adds it on first use.
const KINDS = ["PEST", "DISEASE"];

/** Same rules as the API: a name of at most 50 characters once trimmed, an EPPO code of 1 to 10 letters or digits. */
const NAME_MAX_LENGTH = 50;
const EPPO_CODE_PATTERN = /^\s*([A-Za-z0-9]{1,10})?\s*$/;

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
 * Form of a new pest or disease of the catalogue. The API derives its code
 * from the name, so the note under the name says how.
 *
 * The checks mirror the API rules; what the API still refuses is shown under
 * its field, as for the reference of a variety: a 400 under each field it
 * names, a 409 (the name or its code is taken) under the name. Any other error
 * is shown above the form.
 */
export default function AddHealthIssueModal({ isSubmitting, error, onSubmit, onClose }) {
    const [name, setName] = useState("");
    const [kind, setKind] = useState("");
    const [scientificName, setScientificName] = useState("");
    const [eppoCode, setEppoCode] = useState("");
    const [source, setSource] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const nameConflict = error?.status === 409 ? error.message : null;
    const formError = error && !serverFieldErrors && !nameConflict ? error.message : null;
    const fieldError = (field) =>
        fieldErrors[field] ?? serverFieldErrors?.[field] ?? (field === "name" ? nameConflict : null);
    const fieldProps = (field, id, hasNote = false) => ({
        "aria-invalid": fieldError(field) ? true : undefined,
        "aria-describedby":
            [fieldError(field) ? `${id}-error` : null, hasNote ? `${id}-note` : null]
                .filter(Boolean)
                .join(" ") || undefined,
    });

    function validate() {
        const errors = {};
        if (!name.trim()) {
            errors.name = "Name is required.";
        } else if (name.trim().length > NAME_MAX_LENGTH) {
            errors.name = `Name must be at most ${NAME_MAX_LENGTH} characters.`;
        }
        if (!kind) {
            errors.kind = "Kind is required.";
        }
        if (scientificName.length > 255) {
            errors.scientificName = "Scientific name must be at most 255 characters.";
        }
        if (!EPPO_CODE_PATTERN.test(eppoCode)) {
            errors.eppoCode = "EPPO code must be 1 to 10 letters or digits.";
        }
        if (source.length > 255) {
            errors.source = "Source must be at most 255 characters.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        // Optional fields left empty are not sent at all.
        const issue = { name, kind };
        if (scientificName.trim()) issue.scientificName = scientificName;
        if (eppoCode.trim()) issue.eppoCode = eppoCode;
        if (source.trim()) issue.source = source;
        onSubmit(issue);
    }

    return (
        <PlantsModal labelledBy="add-health-issue-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h3 id="add-health-issue-title" className="font-heading text-xl font-bold text-gray-900">
                    Add a pest or disease
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
                    <FormField id="health-issue-name" label="Name" error={fieldError("name")}
                        note="Its code comes from the name: “Powdery mildew” gives POWDERY_MILDEW.">
                        <input
                            id="health-issue-name"
                            type="text"
                            autoFocus
                            value={name}
                            onChange={(event) => setName(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("name", "health-issue-name", true)}
                        />
                    </FormField>

                    <FormField id="health-issue-kind" label="Kind" error={fieldError("kind")}>
                        <select
                            id="health-issue-kind"
                            value={kind}
                            onChange={(event) => setKind(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("kind", "health-issue-kind")}
                        >
                            {/* Empty value: sending without a choice gives "Kind is required." */}
                            <option value="">Select a kind</option>
                            {KINDS.map((code) => (
                                <option key={code} value={code}>
                                    {formatCode(code)}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField id="health-issue-scientific-name" label="Scientific name"
                        error={fieldError("scientificName")} note="Optional.">
                        <input
                            id="health-issue-scientific-name"
                            type="text"
                            value={scientificName}
                            onChange={(event) => setScientificName(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("scientificName", "health-issue-scientific-name", true)}
                        />
                    </FormField>

                    <FormField id="health-issue-eppo-code" label="EPPO code" error={fieldError("eppoCode")}
                        note="Optional, 1 to 10 letters or digits.">
                        <input
                            id="health-issue-eppo-code"
                            type="text"
                            value={eppoCode}
                            onChange={(event) => setEppoCode(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("eppoCode", "health-issue-eppo-code", true)}
                        />
                    </FormField>

                    <FormField id="health-issue-source" label="Source" error={fieldError("source")}
                        note="Optional. user_entry is recorded when left empty." className="sm:col-span-2">
                        <input
                            id="health-issue-source"
                            type="text"
                            value={source}
                            onChange={(event) => setSource(event.target.value)}
                            placeholder="user_entry"
                            className={TEXT_INPUT_CLASS}
                            {...fieldProps("source", "health-issue-source", true)}
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
                        {isSubmitting ? "Adding…" : "Add to catalogue"}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
