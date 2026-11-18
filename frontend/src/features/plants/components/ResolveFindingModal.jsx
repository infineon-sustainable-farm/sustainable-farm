import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal } from "../../../shared/ui";
import { formatDate } from "../utils/format";
import { todayAsIsoDate } from "../utils/healthIssues";
import FindingSummary from "./FindingSummary";
import FormAlert from "./FormAlert";
import HealthFormActions from "./HealthFormActions";

const NOTE_MAX_LENGTH = 255;

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
    const { t } = useTranslation("plants");
    const [resolvedOn, setResolvedOn] = useState("");
    const [note, setNote] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);

    // A treatment never predates its inspection, so the last one, if any, is the earliest date allowed.
    const earliest = finding.lastTreatedOn ?? finding.inspectedOn;
    const inspection = formatDate(finding.inspectedOn);
    const dateNote = finding.lastTreatedOn
        ? t("health.resolveForm.dateNoteWithTreatment", { inspection, treatment: formatDate(finding.lastTreatedOn) })
        : t("health.resolveForm.dateNote", { inspection });

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    function validate() {
        const errors = {};
        if (!resolvedOn) {
            errors.resolvedOn = t("health.resolveForm.errors.dateRequired");
        } else if (resolvedOn > today) {
            errors.resolvedOn = t("health.resolveForm.errors.dateInFuture");
        } else if (resolvedOn < finding.inspectedOn) {
            errors.resolvedOn = t("health.resolveForm.errors.beforeInspection", { date: inspection });
        } else if (finding.lastTreatedOn && resolvedOn < finding.lastTreatedOn) {
            errors.resolvedOn = t("health.resolveForm.errors.beforeTreatment", {
                date: formatDate(finding.lastTreatedOn),
            });
        }
        if (note.length > NOTE_MAX_LENGTH) {
            errors.note = t("health.resolveForm.errors.noteTooLong");
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
        <Modal open onClose={onClose} size="xl" title={t("health.resolveForm.title")}>
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                <FindingSummary finding={finding} />

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <Field
                        id="resolution-date"
                        label={t("health.resolveForm.date")}
                        error={fieldError("resolvedOn")}
                        hint={dateNote}
                    >
                        <Input
                            type="date"
                            autoFocus
                            min={earliest}
                            max={today}
                            value={resolvedOn}
                            onChange={(event) => setResolvedOn(event.target.value)}
                        />
                    </Field>

                    <Field
                        id="resolution-note"
                        label={t("health.resolveForm.note")}
                        error={fieldError("note")}
                        hint={t("health.resolveForm.noteHint")}
                    >
                        <Input type="text" value={note} onChange={(event) => setNote(event.target.value)} />
                    </Field>
                </div>

                <HealthFormActions
                    onCancel={onClose}
                    isSubmitting={isSubmitting}
                    submitLabel={t("health.resolveForm.submit")}
                    submittingLabel={t("health.resolveForm.submitting")}
                />
            </form>
        </Modal>
    );
}
