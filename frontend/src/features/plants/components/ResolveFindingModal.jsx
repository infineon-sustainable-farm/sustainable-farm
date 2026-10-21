import { useState } from "react";
import { X } from "lucide-react";
import { formatDate } from "../utils/format";
import FindingSummary from "./FindingSummary";
import PlantsModal from "./PlantsModal";

const LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none";
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

/** Today as "YYYY-MM-DD", in local time: the latest resolution date the form accepts. */
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
 * Form of the resolution of a finding: the day the problem was seen to be
 * settled, with an optional note. A finding is resolved once; the API then
 * computes its status, treated or closed without treatment.
 *
 * The date cannot predate the inspection nor the last treatment, as for the
 * API. What the API still refuses is shown the same way as for a planting:
 * - a 400 with fieldErrors: each message under its field;
 * - a 409 (the finding was resolved meanwhile, for example from another tab),
 *   a 404, a 422, or any other error: one message above the form. The
 *   findings are refreshed after a 409, so the row shows the resolution.
 */
export default function ResolveFindingModal({ finding, isSubmitting, error, onSubmit, onClose }) {
    const [resolvedOn, setResolvedOn] = useState("");
    const [note, setNote] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);

    // A treatment never predates its inspection, so the last one, if any, is the earliest date allowed.
    const earliest = finding.lastTreatedOn ?? finding.inspectedOn;
    const dateNote = finding.lastTreatedOn
        ? `Not before the inspection (${formatDate(finding.inspectedOn)}) nor the last treatment `
            + `(${formatDate(finding.lastTreatedOn)}).`
        : `Not before the inspection, on ${formatDate(finding.inspectedOn)}.`;

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];
    const fieldProps = (field, id, hasNote = false) => ({
        "aria-invalid": fieldError(field) ? true : undefined,
        "aria-describedby":
            [fieldError(field) ? `${id}-error` : null, hasNote ? `${id}-note` : null].filter(Boolean).join(" ")
            || undefined,
    });

    function validate() {
        const errors = {};
        if (!resolvedOn) {
            errors.resolvedOn = "Resolution date is required.";
        } else if (resolvedOn > today) {
            errors.resolvedOn = "Resolution date cannot be in the future.";
        } else if (resolvedOn < finding.inspectedOn) {
            errors.resolvedOn = `Resolution date cannot be before the inspection, on ${formatDate(finding.inspectedOn)}.`;
        } else if (finding.lastTreatedOn && resolvedOn < finding.lastTreatedOn) {
            errors.resolvedOn =
                `Resolution date cannot be before the last treatment, on ${formatDate(finding.lastTreatedOn)}.`;
        }
        if (note.length > 255) {
            errors.note = "Note must be at most 255 characters.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        // A note left empty is not sent at all.
        const resolution = { resolvedOn };
        if (note.trim()) resolution.note = note;
        onSubmit(resolution);
    }

    return (
        <PlantsModal labelledBy="resolve-finding-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h3 id="resolve-finding-title" className="font-heading text-xl font-bold text-gray-900">
                    Resolve a finding
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

                <FindingSummary finding={finding} />

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <FormField id="resolution-date" label="Resolution date" error={fieldError("resolvedOn")} note={dateNote}>
                        <input
                            id="resolution-date"
                            type="date"
                            autoFocus
                            min={earliest}
                            max={today}
                            value={resolvedOn}
                            onChange={(event) => setResolvedOn(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("resolvedOn", "resolution-date", true)}
                        />
                    </FormField>

                    <FormField id="resolution-note" label="Note" error={fieldError("note")}
                        note="Optional, such as how the problem was settled.">
                        <input
                            id="resolution-note"
                            type="text"
                            value={note}
                            onChange={(event) => setNote(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("note", "resolution-note", true)}
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
                        {isSubmitting ? "Resolving…" : "Resolve finding"}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
