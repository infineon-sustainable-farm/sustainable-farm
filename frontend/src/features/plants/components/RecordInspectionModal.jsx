import { useRef, useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Field, Input, Modal, Select } from "../../../shared/ui";
import { useHealthIssues } from "../hooks/useHealthIssues";
import { formatEnum } from "../utils/format";
import { CATALOGUE_NOTE_ID, issueOptions, OTHER_ISSUE_CODE, todayAsIsoDate } from "../utils/healthIssues";
import FormAlert from "./FormAlert";
import HealthFormActions from "./HealthFormActions";
import InspectionFindingFields from "./InspectionFindingFields";

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
const TEXT_MAX_LENGTH = 255;

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
    const { t } = useTranslation("plants");
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
        catalogueNote = t("health.issues.loading");
    } else if (issues.isError) {
        catalogueNote = t("health.catalogue.loadError");
    } else if (options.groups.length === 0) {
        catalogueNote = t("health.catalogue.empty");
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
            errors.blockCode = t("health.errors.blockRequired");
        } else if (!BLOCK_CODE_PATTERN.test(blockCode)) {
            errors.blockCode = t("health.errors.blockFormat");
        }
        if (!inspectedOn) {
            errors.inspectedOn = t("health.inspectionForm.errors.dateRequired");
        } else if (inspectedOn > today) {
            errors.inspectedOn = t("health.inspectionForm.errors.dateInFuture");
        }
        const score = healthScorePct.trim();
        if (!isScoreUnreadable && score === "") {
            errors.healthScorePct = t("health.inspectionForm.errors.scoreRequired");
        } else if (isScoreUnreadable || !SCORE_PATTERN.test(score) || Number(score) > 100) {
            errors.healthScorePct = t("health.inspectionForm.errors.scoreFormat");
        }
        if (observer.length > TEXT_MAX_LENGTH) {
            errors.observer = t("health.inspectionForm.errors.observerTooLong");
        }
        for (const finding of findings) {
            const prefix = `finding-${finding.key}-`;
            if (!finding.issueCode) {
                errors[`${prefix}issueCode`] = t("health.inspectionForm.errors.problemRequired");
            } else if (finding.issueCode === OTHER_ISSUE_CODE) {
                if (!finding.otherLabel.trim()) {
                    errors[`${prefix}otherLabel`] = t("health.errors.otherLabelRequired");
                } else if (finding.otherLabel.length > OTHER_LABEL_MAX_LENGTH) {
                    errors[`${prefix}otherLabel`] = t("health.errors.otherLabelTooLong", {
                        max: OTHER_LABEL_MAX_LENGTH,
                    });
                }
            }
            if (!TREE_LABEL_PATTERN.test(finding.treeLabel)) {
                errors[`${prefix}treeLabel`] = t("health.inspectionForm.errors.treeFormat");
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

    const findingsError = fieldError("findings");
    const methodOptions = METHODS.map((code) => ({ value: code, label: formatEnum("inspectionMethod", code) }));

    return (
        <Modal open onClose={onClose} size="xl" title={t("health.inspectionForm.title")}>
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <Field
                        id="inspection-block"
                        label={t("health.block")}
                        error={fieldError("blockCode")}
                        hint={t("health.blockNote")}
                    >
                        <Input
                            type="text"
                            autoFocus
                            list="inspection-block-options"
                            value={blockCode}
                            onChange={(event) => setBlockCode(event.target.value)}
                        />
                    </Field>
                    <datalist id="inspection-block-options">
                        {blocks.map((block) => (
                            <option key={block} value={block} />
                        ))}
                    </datalist>

                    <Field id="inspection-date" label={t("health.inspectionForm.date")} error={fieldError("inspectedOn")}>
                        <Input
                            type="date"
                            max={today}
                            value={inspectedOn}
                            onChange={(event) => setInspectedOn(event.target.value)}
                        />
                    </Field>

                    <Field
                        id="inspection-score"
                        label={t("health.inspectionForm.score")}
                        error={fieldError("healthScorePct")}
                        hint={t("health.inspectionForm.scoreNote")}
                    >
                        <Input
                            type="number"
                            min="0"
                            max="100"
                            step="1"
                            inputMode="numeric"
                            value={healthScorePct}
                            onChange={changeScore}
                        />
                    </Field>

                    <Field id="inspection-method" label={t("health.inspectionForm.method")} error={fieldError("method")}>
                        <Select value={method} onChange={(event) => setMethod(event.target.value)}>
                            <option value="">{t("health.inspectionForm.notSpecified")}</option>
                            {methodOptions.map((option) => (
                                <option key={option.value} value={option.value}>
                                    {option.label}
                                </option>
                            ))}
                        </Select>
                    </Field>

                    <Field
                        id="inspection-observer"
                        label={t("health.inspectionForm.observer")}
                        error={fieldError("observer")}
                        hint={t("health.optional")}
                        className="sm:col-span-2"
                    >
                        <Input type="text" value={observer} onChange={(event) => setObserver(event.target.value)} />
                    </Field>
                </div>

                <fieldset className="mt-6">
                    <legend className="text-sm font-medium">{t("health.inspectionForm.findings")}</legend>
                    <p className="mt-1 text-xs text-text-muted">
                        {findings.length === 0
                            ? t("health.inspectionForm.noFindings")
                            : t("health.inspectionForm.findingsNote")}
                    </p>
                    {catalogueNote && (
                        <p id={CATALOGUE_NOTE_ID} className="mt-1 text-xs text-text-muted">
                            {catalogueNote}
                        </p>
                    )}
                    {findingsError && <p className="mt-1 text-xs text-error-fg">{findingsError}</p>}

                    {findings.length > 0 && (
                        <div className="mt-3 flex flex-col gap-4">
                            {findings.map((finding, index) => (
                                <InspectionFindingFields
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

                    <Button ref={addButtonRef} size="sm" variant="secondary" icon={Plus} onClick={addFinding} className="mt-3">
                        {t("health.inspectionForm.addFinding")}
                    </Button>
                </fieldset>

                <HealthFormActions
                    onCancel={onClose}
                    isSubmitting={isSubmitting}
                    submitLabel={t("health.inspectionForm.submit")}
                    submittingLabel={t("health.inspectionForm.submitting")}
                />
            </form>
        </Modal>
    );
}
