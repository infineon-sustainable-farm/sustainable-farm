import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal, Select } from "../../../shared/ui";
import { monthName } from "../utils/format";
import FormAlert from "./FormAlert";
import { FormButtons } from "./HarvestFormParts";

const MONTHS = Array.from({ length: 12 }, (_, index) => index + 1);

/** Same rule as the API: above 0 and at most 1000 kg, with at most 1 decimal. */
const YIELD_PATTERN = /^\d{1,4}(\.\d)?$/;

function MonthSelect({ value, onChange }) {
    const { t } = useTranslation("plants");
    return (
        <Select value={value} onChange={(event) => onChange(event.target.value)}>
            {/* Empty value: sending without a choice gives the "required" message. */}
            <option value="">{t("harvest.referenceForm.selectMonth")}</option>
            {MONTHS.map((month) => (
                <option key={month} value={month}>
                    {monthName(month)}
                </option>
            ))}
        </Select>
    );
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
    const { t } = useTranslation("plants");
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

    /**
     * Note under a source field. A new reference records an empty source as
     * user_entry; a correction keeps the recorded source while its value stays
     * the same, as the API does.
     */
    function sourceNote(recordedSource, unchangedKey) {
        return recordedSource
            ? t("harvest.referenceForm.keepSource", { source: recordedSource, condition: t(unchangedKey) })
            : t("harvest.references.sourceNote");
    }

    function changeYield(event) {
        setYieldPerTreeKg(event.target.value);
        setIsYieldUnreadable(event.target.validity.badInput);
    }

    function validate() {
        const errors = {};
        if (!varietyName.trim()) {
            errors.varietyName = t("harvest.referenceForm.errors.varietyRequired");
        } else if (varietyName.length > 255) {
            errors.varietyName = t("harvest.referenceForm.errors.varietyTooLong");
        }
        const yieldText = yieldPerTreeKg.trim();
        if (!isYieldUnreadable && yieldText === "") {
            errors.yieldPerTreeKg = t("harvest.referenceForm.errors.yieldRequired");
        } else if (isYieldUnreadable || !YIELD_PATTERN.test(yieldText)
            || Number(yieldText) <= 0 || Number(yieldText) > 1000) {
            errors.yieldPerTreeKg = t("harvest.referenceForm.errors.yieldInvalid");
        }
        if (!harvestStartMonth) {
            errors.harvestStartMonth = t("harvest.referenceForm.errors.startRequired");
        }
        if (!harvestEndMonth) {
            errors.harvestEndMonth = t("harvest.referenceForm.errors.endRequired");
        }
        if (yieldSource.length > 255) {
            errors.yieldSource = t("harvest.referenceForm.errors.yieldSourceTooLong");
        }
        if (seasonSource.length > 255) {
            errors.seasonSource = t("harvest.referenceForm.errors.seasonSourceTooLong");
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

    const userEntry = t("harvest.references.userEntry");
    const title = isEditing
        ? t("harvest.referenceForm.editTitle", { name: reference.varietyName })
        : t("harvest.references.add");

    return (
        <Modal open onClose={onClose} title={title} size="xl">
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <Field id="reference-variety" label={t("harvest.referenceForm.variety")} error={fieldError("varietyName")}
                        hint={t("harvest.referenceForm.varietyHint")} className="sm:col-span-2">
                        <Input type="text" value={varietyName} onChange={(event) => setVarietyName(event.target.value)} />
                    </Field>

                    <Field id="reference-yield" label={t("harvest.referenceForm.yield")} error={fieldError("yieldPerTreeKg")}
                        hint={t("harvest.referenceForm.yieldHint")}>
                        <Input
                            type="number"
                            min="0"
                            max="1000"
                            step="any"
                            inputMode="decimal"
                            value={yieldPerTreeKg}
                            onChange={changeYield}
                        />
                    </Field>

                    <Field id="reference-yield-source" label={t("harvest.referenceForm.yieldSource")}
                        error={fieldError("yieldSource")}
                        hint={sourceNote(reference?.yieldSource, "harvest.referenceForm.yieldUnchanged")}>
                        <Input
                            type="text"
                            value={yieldSource}
                            onChange={(event) => setYieldSource(event.target.value)}
                            placeholder={reference?.yieldSource ?? userEntry}
                        />
                    </Field>

                    <Field id="reference-start-month" label={t("harvest.referenceForm.startMonth")}
                        error={fieldError("harvestStartMonth")}>
                        <MonthSelect value={harvestStartMonth} onChange={setHarvestStartMonth} />
                    </Field>

                    <Field id="reference-end-month" label={t("harvest.referenceForm.endMonth")}
                        error={fieldError("harvestEndMonth")} hint={t("harvest.referenceForm.endMonthHint")}>
                        <MonthSelect value={harvestEndMonth} onChange={setHarvestEndMonth} />
                    </Field>

                    <Field id="reference-season-source" label={t("harvest.referenceForm.seasonSource")}
                        error={fieldError("seasonSource")}
                        hint={sourceNote(reference?.seasonSource, "harvest.referenceForm.monthsUnchanged")}
                        className="sm:col-span-2">
                        <Input
                            type="text"
                            value={seasonSource}
                            onChange={(event) => setSeasonSource(event.target.value)}
                            placeholder={reference?.seasonSource ?? userEntry}
                        />
                    </Field>
                </div>

                <FormButtons
                    onCancel={onClose}
                    isSubmitting={isSubmitting}
                    submitDisabled={false}
                    submitLabel={isEditing ? t("common:actions.save") : t("harvest.referenceForm.addVariety")}
                    submittingLabel={isEditing ? t("harvest.references.saving") : t("harvest.referenceForm.adding")}
                />
            </form>
        </Modal>
    );
}
