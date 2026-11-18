import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Input, Modal, Select } from "../../../shared/ui";
import { useVarietyBlocks } from "../hooks/useVarieties";
import { useVarietyReferences } from "../hooks/useVarietyReferences";
import { formatEnum } from "../utils/format";
import { batchRequestBody, NURSERY_STAGES, PURCHASED } from "../utils/nurseryRequests";
import { todayAsIsoDate, validateBatch } from "../utils/nurseryValidation";
import FormAlert from "./FormAlert";
import NurseryField from "./NurseryField";
import { noteId } from "./nurseryForm";
import NurseryOriginField from "./NurseryOriginField";

// The fields that carry a note under them, with the id of their control.
const NOTE_FIELD_IDS = {
    batchCode: "batch-code",
    varietyName: "batch-variety",
    startedOn: "batch-start",
    plannedBlockCode: "batch-planned-block",
};

// The fields of a purchase only.
const SUPPLIER_FIELDS = ["supplier", "supplierLotNumber"];

const INITIAL_VALUES = {
    batchCode: "",
    varietyName: "",
    origin: "",
    supplier: "",
    supplierLotNumber: "",
    startedOn: "",
    initialCount: "",
    initialStage: "",
    plannedTransplantOn: "",
    plannedBlockCode: "",
};

/**
 * Form of a new nursery batch: its code, variety and origin, its start date,
 * count and stage, and when and where it is planned to be transplanted.
 *
 * The supplier and its lot number are asked for an external purchase only,
 * as the API requires the supplier then and refuses both otherwise. Going
 * back to in-house production empties them and drops their errors, the API's
 * included. The variety names of the reference and the blocks of the recorded
 * varieties are offered as suggestions; both stay free text.
 *
 * The checks mirror the API's 400 rules (utils/nurseryValidation); what the API
 * still refuses is shown under its field: a 400 under each field it names, a
 * 409 (the code is taken) under the code. A 422 (the planned date is before the
 * start date), or any other error, is shown above the form.
 */
export default function AddNurseryBatchModal({ isSubmitting, error, onSubmit, onResetError, onClose }) {
    const { t } = useTranslation("plants");
    const [values, setValues] = useState(INITIAL_VALUES);
    // A number field holds "" for text it cannot read, such as "1e"; this
    // keeps such a value from passing as an empty one.
    const [isCountUnreadable, setIsCountUnreadable] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);

    const { data: references } = useVarietyReferences();
    const { data: blocks } = useVarietyBlocks();

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const codeConflict = error?.status === 409 ? error.message : null;
    const formError = error && !serverFieldErrors && !codeConflict ? error.message : null;
    const fieldError = (field) =>
        fieldErrors[field] ?? serverFieldErrors?.[field] ?? (field === "batchCode" ? codeConflict : null);
    const notes = Object.fromEntries(Object.keys(NOTE_FIELD_IDS).map((field) => [field, t(`nursery.addForm.notes.${field}`)]));
    const describedBy = Object.fromEntries(Object.entries(NOTE_FIELD_IDS).map(([field, id]) => [field, noteId(id)]));
    const stageOptions = NURSERY_STAGES.map((stage) => ({ value: stage, label: formatEnum("nurseryStage", stage) }));
    const isPurchase = values.origin === PURCHASED;

    function setValue(field, value) {
        setValues((current) => ({ ...current, [field]: value }));
    }

    function changeCount(event) {
        setValue("initialCount", event.target.value);
        setIsCountUnreadable(event.target.validity.badInput);
    }

    function changeOrigin(nextOrigin) {
        if (!isPurchase) {
            setValue("origin", nextOrigin);
            return;
        }
        setValues((current) => ({ ...current, origin: nextOrigin, supplier: "", supplierLotNumber: "" }));
        setFieldErrors((current) =>
            Object.fromEntries(Object.entries(current).filter(([field]) => !SUPPLIER_FIELDS.includes(field))));
        // The API's answer may be about the supplier fields, which are gone.
        onResetError();
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validateBatch(values, { today, isCountUnreadable });
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit(batchRequestBody(values));
    }

    return (
        <Modal open onClose={onClose} title={t("nursery.addForm.title")} size="xl">
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <NurseryField
                        id="batch-code"
                        label={t("nursery.addForm.batchCode")}
                        error={fieldError("batchCode")}
                        note={notes.batchCode}
                    >
                        <Input
                            type="text"
                            value={values.batchCode}
                            onChange={(event) => setValue("batchCode", event.target.value)}
                            aria-describedby={describedBy.batchCode}
                        />
                    </NurseryField>

                    <NurseryField
                        id="batch-variety"
                        label={t("nursery.addForm.variety")}
                        error={fieldError("varietyName")}
                        note={notes.varietyName}
                    >
                        <Input
                            type="text"
                            list="batch-variety-options"
                            value={values.varietyName}
                            onChange={(event) => setValue("varietyName", event.target.value)}
                            aria-describedby={describedBy.varietyName}
                        />
                        <datalist id="batch-variety-options">
                            {(references ?? []).map((reference) => (
                                <option key={reference.id} value={reference.varietyName} />
                            ))}
                        </datalist>
                    </NurseryField>

                    <NurseryOriginField value={values.origin} error={fieldError("origin")} onChange={changeOrigin} />

                    {isPurchase && (
                        <>
                            <NurseryField
                                id="batch-supplier"
                                label={t("nursery.addForm.supplier")}
                                error={fieldError("supplier")}
                            >
                                <Input
                                    type="text"
                                    value={values.supplier}
                                    onChange={(event) => setValue("supplier", event.target.value)}
                                />
                            </NurseryField>

                            <NurseryField
                                id="batch-supplier-lot"
                                label={t("nursery.addForm.supplierLot")}
                                error={fieldError("supplierLotNumber")}
                            >
                                <Input
                                    type="text"
                                    value={values.supplierLotNumber}
                                    onChange={(event) => setValue("supplierLotNumber", event.target.value)}
                                />
                            </NurseryField>
                        </>
                    )}

                    <NurseryField
                        id="batch-start"
                        label={t("nursery.addForm.startDate")}
                        error={fieldError("startedOn")}
                        note={notes.startedOn}
                    >
                        <Input
                            type="date"
                            max={today}
                            value={values.startedOn}
                            onChange={(event) => setValue("startedOn", event.target.value)}
                            aria-describedby={describedBy.startedOn}
                        />
                    </NurseryField>

                    <NurseryField
                        id="batch-count"
                        label={t("nursery.addForm.initialCount")}
                        error={fieldError("initialCount")}
                    >
                        <Input
                            type="number"
                            min="1"
                            step="1"
                            inputMode="numeric"
                            value={values.initialCount}
                            onChange={changeCount}
                        />
                    </NurseryField>

                    <NurseryField
                        id="batch-stage"
                        label={t("nursery.addForm.initialStage")}
                        error={fieldError("initialStage")}
                    >
                        <Select
                            value={values.initialStage}
                            onChange={(event) => setValue("initialStage", event.target.value)}
                        >
                            {/* Empty value: sending without a choice gives "Initial stage is required." */}
                            <option value="">{t("nursery.addForm.selectStage")}</option>
                            {stageOptions.map((option) => (
                                <option key={option.value} value={option.value}>
                                    {option.label}
                                </option>
                            ))}
                        </Select>
                    </NurseryField>

                    <NurseryField
                        id="batch-planned-date"
                        label={t("nursery.addForm.plannedDate")}
                        error={fieldError("plannedTransplantOn")}
                    >
                        <Input
                            type="date"
                            value={values.plannedTransplantOn}
                            onChange={(event) => setValue("plannedTransplantOn", event.target.value)}
                        />
                    </NurseryField>

                    <NurseryField
                        id="batch-planned-block"
                        label={t("nursery.addForm.plannedBlock")}
                        error={fieldError("plannedBlockCode")}
                        note={notes.plannedBlockCode}
                    >
                        <Input
                            type="text"
                            list="batch-block-options"
                            value={values.plannedBlockCode}
                            onChange={(event) => setValue("plannedBlockCode", event.target.value)}
                            aria-describedby={describedBy.plannedBlockCode}
                        />
                        <datalist id="batch-block-options">
                            {(blocks ?? []).map((block) => (
                                <option key={block} value={block} />
                            ))}
                        </datalist>
                    </NurseryField>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button variant="secondary" onClick={onClose}>
                        {t("common:actions.cancel")}
                    </Button>
                    <Button type="submit" disabled={isSubmitting}>
                        {isSubmitting ? t("nursery.addForm.submitting") : t("nursery.addForm.submit")}
                    </Button>
                </div>
            </form>
        </Modal>
    );
}
