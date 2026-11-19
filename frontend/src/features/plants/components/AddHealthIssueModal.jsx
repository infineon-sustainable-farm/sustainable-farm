import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal, Select } from "../../../shared/ui";
import { formatEnum } from "../utils/format";
import FormAlert from "./FormAlert";
import HealthFormActions from "./HealthFormActions";

// The API codes, sent as they are; "Other" is not one of them, the API adds it on first use.
const KINDS = ["PEST", "DISEASE"];

/** Same rules as the API: a name of at most 50 characters once trimmed, an EPPO code of 1 to 10 letters or digits. */
const NAME_MAX_LENGTH = 50;
const TEXT_MAX_LENGTH = 255;
const EPPO_CODE_PATTERN = /^\s*([A-Za-z0-9]{1,10})?\s*$/;

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
    const { t } = useTranslation("plants");
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
    const kindOptions = KINDS.map((code) => ({ value: code, label: formatEnum("issueKind", code) }));

    function validate() {
        const errors = {};
        if (!name.trim()) {
            errors.name = t("health.issueForm.errors.nameRequired");
        } else if (name.trim().length > NAME_MAX_LENGTH) {
            errors.name = t("health.issueForm.errors.nameTooLong", { max: NAME_MAX_LENGTH });
        }
        if (!kind) {
            errors.kind = t("health.issueForm.errors.kindRequired");
        }
        if (scientificName.length > TEXT_MAX_LENGTH) {
            errors.scientificName = t("health.issueForm.errors.scientificNameTooLong");
        }
        if (!EPPO_CODE_PATTERN.test(eppoCode)) {
            errors.eppoCode = t("health.issueForm.errors.eppoCode");
        }
        if (source.length > TEXT_MAX_LENGTH) {
            errors.source = t("health.issueForm.errors.sourceTooLong");
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
        <Modal open onClose={onClose} size="xl" title={t("health.issueForm.title")}>
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <Field
                        id="health-issue-name"
                        label={t("health.issueForm.name")}
                        error={fieldError("name")}
                        hint={t("health.issueForm.nameNote")}
                    >
                        <Input type="text" autoFocus value={name} onChange={(event) => setName(event.target.value)} />
                    </Field>

                    <Field id="health-issue-kind" label={t("health.issueForm.kind")} error={fieldError("kind")}>
                        <Select value={kind} onChange={(event) => setKind(event.target.value)}>
                            {/* Empty value: sending without a choice gives "Kind is required." */}
                            <option value="">{t("health.issueForm.selectKind")}</option>
                            {kindOptions.map((option) => (
                                <option key={option.value} value={option.value}>
                                    {option.label}
                                </option>
                            ))}
                        </Select>
                    </Field>

                    <Field
                        id="health-issue-scientific-name"
                        label={t("health.issueForm.scientificName")}
                        error={fieldError("scientificName")}
                        hint={t("health.optional")}
                    >
                        <Input
                            type="text"
                            value={scientificName}
                            onChange={(event) => setScientificName(event.target.value)}
                        />
                    </Field>

                    <Field
                        id="health-issue-eppo-code"
                        label={t("health.issueForm.eppoCode")}
                        error={fieldError("eppoCode")}
                        hint={t("health.issueForm.eppoCodeNote")}
                    >
                        <Input type="text" value={eppoCode} onChange={(event) => setEppoCode(event.target.value)} />
                    </Field>

                    <Field
                        id="health-issue-source"
                        label={t("health.issueForm.source")}
                        error={fieldError("source")}
                        hint={t("health.issueForm.sourceNote")}
                        className="sm:col-span-2"
                    >
                        <Input
                            type="text"
                            value={source}
                            onChange={(event) => setSource(event.target.value)}
                            placeholder={t("health.issueForm.sourcePlaceholder")}
                        />
                    </Field>
                </div>

                <HealthFormActions
                    onCancel={onClose}
                    isSubmitting={isSubmitting}
                    submitLabel={t("health.issueForm.submit")}
                    submittingLabel={t("health.issueForm.submitting")}
                />
            </form>
        </Modal>
    );
}
