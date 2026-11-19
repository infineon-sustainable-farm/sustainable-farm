import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal, Select } from "../../../shared/ui";
import { useHealthIssues } from "../hooks/useHealthIssues";
import { formatDate, formatUnit } from "../utils/format";
import { issueOptions, OTHER_ISSUE_CODE, todayAsIsoDate } from "../utils/healthIssues";
import FindingSummary from "./FindingSummary";
import FormAlert from "./FormAlert";
import HealthFormActions from "./HealthFormActions";
import TreatmentTargetFields from "./TreatmentTargetFields";

// The API codes, sent as they are; only their symbols are shown.
const UNITS = ["KG", "L"];

/**
 * Same rules as the API: a quantity above 0 with at most 3 decimals, a whole number of days, and a name of at most 60 characters
 * for "Other".
 */
const QUANTITY_PATTERN = /^\d{1,9}(\.\d{1,3})?$/;
const DAYS_PATTERN = /^\d{1,9}$/;
const OTHER_LABEL_MAX_LENGTH = 60;
const TEXT_MAX_LENGTH = 255;

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

/**
 * A required text of at most 255 characters, as the API checks it. `messages` names the field:
 * health.treatmentForm.errors.<messages>Required and <messages>TooLong.
 */
function checkText(value, messages, t) {
    if (!value.trim()) return t(`health.treatmentForm.errors.${messages}Required`);
    if (value.length > TEXT_MAX_LENGTH) return t(`health.treatmentForm.errors.${messages}TooLong`);
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
export default function TreatmentModal({ finding, isSubmitting, error, onSubmit, onClose }) {
    const { t } = useTranslation("plants");
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
        catalogueNote = t("health.issues.loading");
    } else if (isPreventive && issues.isError) {
        catalogueNote = t("health.catalogue.loadError");
    } else if (isPreventive && options.groups.length === 0) {
        catalogueNote = t("health.catalogue.empty");
    }
    const dateNote = isPreventive
        ? null
        : t("health.resolveForm.dateNote", { inspection: formatDate(finding.inspectedOn) });

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

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
            if (!values.blockCode) {
                errors.blockCode = t("health.errors.blockRequired");
            }
            if (!values.targetIssueCode) {
                errors.targetIssueCode = t("health.treatmentForm.errors.targetRequired");
            } else if (values.targetIssueCode === OTHER_ISSUE_CODE) {
                if (!values.targetOtherLabel.trim()) {
                    errors.targetOtherLabel = t("health.errors.otherLabelRequired");
                } else if (values.targetOtherLabel.length > OTHER_LABEL_MAX_LENGTH) {
                    errors.targetOtherLabel = t("health.errors.otherLabelTooLong", { max: OTHER_LABEL_MAX_LENGTH });
                }
            }
        }
        if (!values.treatedOn) {
            errors.treatedOn = t("health.treatmentForm.errors.dateRequired");
        } else if (values.treatedOn > today) {
            errors.treatedOn = t("health.treatmentForm.errors.dateInFuture");
        } else if (!isPreventive && values.treatedOn < finding.inspectedOn) {
            errors.treatedOn = t("health.treatmentForm.errors.beforeInspection", {
                date: formatDate(finding.inspectedOn),
            });
        }
        const days = values.preHarvestIntervalDays.trim();
        if (!unreadable.preHarvestIntervalDays && days === "") {
            errors.preHarvestIntervalDays = t("health.treatmentForm.errors.intervalRequired");
        } else if (unreadable.preHarvestIntervalDays || !DAYS_PATTERN.test(days)) {
            errors.preHarvestIntervalDays = t("health.treatmentForm.errors.intervalFormat");
        }
        const productError = checkText(values.productName, "product", t);
        if (productError) errors.productName = productError;
        const ingredientError = checkText(values.activeIngredient, "ingredient", t);
        if (ingredientError) errors.activeIngredient = ingredientError;
        const quantity = values.quantity.trim();
        if (unreadable.quantity || !QUANTITY_PATTERN.test(quantity) || Number(quantity) <= 0) {
            errors.quantity = t("health.treatmentForm.errors.quantityFormat");
        }
        if (!values.unit) {
            errors.unit = t("health.treatmentForm.errors.unitRequired");
        }
        const applicatorError = checkText(values.applicator, "applicator", t);
        if (applicatorError) errors.applicator = applicatorError;
        if (values.equipment.length > TEXT_MAX_LENGTH) {
            errors.equipment = t("health.treatmentForm.errors.equipmentTooLong");
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

    const title = isPreventive ? t("health.treatmentForm.preventiveTitle") : t("health.treatmentForm.findingTitle");
    const unitOptions = UNITS.map((code) => ({ value: code, label: formatUnit(code) }));
    // Props of the plain text fields, by field name.
    const textInputs = Object.fromEntries(
        ["productName", "activeIngredient", "applicator", "equipment"].map((field) => [
            field,
            { type: "text", value: values[field], onChange: (event) => setValue(field, event.target.value) },
        ]),
    );

    return (
        <Modal open onClose={onClose} size="xl" title={title}>
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                {!isPreventive && <FindingSummary finding={finding} />}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    {isPreventive && (
                        <TreatmentTargetFields
                            values={values}
                            options={options}
                            catalogueNote={catalogueNote}
                            fieldError={fieldError}
                            onChange={setValue}
                            onTargetChange={changeTarget}
                        />
                    )}

                    <Field
                        id="treatment-date"
                        label={t("health.treatmentForm.date")}
                        error={fieldError("treatedOn")}
                        hint={dateNote}
                    >
                        <Input
                            type="date"
                            autoFocus={!isPreventive}
                            min={isPreventive ? undefined : finding.inspectedOn}
                            max={today}
                            value={values.treatedOn}
                            onChange={(event) => setValue("treatedOn", event.target.value)}
                        />
                    </Field>

                    <Field
                        id="treatment-interval"
                        label={t("health.treatmentForm.interval")}
                        error={fieldError("preHarvestIntervalDays")}
                        hint={t("health.treatmentForm.intervalNote")}
                    >
                        <Input
                            type="number"
                            min="0"
                            step="1"
                            inputMode="numeric"
                            value={values.preHarvestIntervalDays}
                            onChange={(event) => setNumber("preHarvestIntervalDays", event)}
                        />
                    </Field>

                    <Field
                        id="treatment-product"
                        label={t("health.treatmentForm.product")}
                        error={fieldError("productName")}
                        hint={t("health.treatmentForm.productNote")}
                    >
                        <Input {...textInputs.productName} />
                    </Field>

                    <Field
                        id="treatment-ingredient"
                        label={t("health.treatmentForm.ingredient")}
                        error={fieldError("activeIngredient")}
                    >
                        <Input {...textInputs.activeIngredient} />
                    </Field>

                    <Field id="treatment-quantity" label={t("health.treatmentForm.quantity")} error={fieldError("quantity")}>
                        <Input
                            type="number"
                            min="0"
                            step="any"
                            inputMode="decimal"
                            value={values.quantity}
                            onChange={(event) => setNumber("quantity", event)}
                        />
                    </Field>

                    <Field id="treatment-unit" label={t("health.treatmentForm.unit")} error={fieldError("unit")}>
                        <Select value={values.unit} onChange={(event) => setValue("unit", event.target.value)}>
                            {/* Empty value: sending without a choice gives "Unit is required." */}
                            <option value="">{t("health.treatmentForm.selectUnit")}</option>
                            {unitOptions.map((option) => (
                                <option key={option.value} value={option.value}>
                                    {option.label}
                                </option>
                            ))}
                        </Select>
                    </Field>

                    <Field
                        id="treatment-applicator"
                        label={t("health.treatmentForm.applicator")}
                        error={fieldError("applicator")}
                    >
                        <Input {...textInputs.applicator} />
                    </Field>

                    <Field
                        id="treatment-equipment"
                        label={t("health.treatmentForm.equipment")}
                        error={fieldError("equipment")}
                        hint={t("health.optional")}
                    >
                        <Input {...textInputs.equipment} />
                    </Field>
                </div>

                <HealthFormActions
                    onCancel={onClose}
                    isSubmitting={isSubmitting}
                    submitLabel={t("health.treatmentForm.submit")}
                    submittingLabel={t("health.inspectionForm.submitting")}
                />
            </form>
        </Modal>
    );
}
