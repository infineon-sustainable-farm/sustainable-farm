import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Input, Modal, Select } from "../../../shared/ui";
import { useNurseryBatches } from "../hooks/useNurseryBatches";
import { useVarietyBlocks } from "../hooks/useVarieties";
import { formatDate, formatEnum, formatNumber } from "../utils/format";
import {
    EVENT_DATE_FIELDS,
    eventRequestBody,
    NURSERY_EVENT_TYPES,
    NURSERY_STAGES,
} from "../utils/nurseryRequests";
import { todayAsIsoDate, validateEvent } from "../utils/nurseryValidation";
import FormAlert from "./FormAlert";
import NurseryField from "./NurseryField";
import { noteId, RADIO_CLASS } from "./nurseryForm";

// The fields each event adds to the batch and the date.
const TYPE_FIELDS = {
    STAGE_CHANGE: ["stage"],
    LOSS: ["quantity", "reason"],
    TRANSPLANT: ["quantity", "blockCode"],
};

// Ids of the controls that carry a note.
const FIELD_IDS = { batch: "event-batch", stage: "event-stage", quantity: "event-quantity", block: "event-block" };

const INITIAL_VALUES = {
    batchId: "",
    date: "",
    stage: "",
    quantity: "",
    reason: "",
    blockCode: "",
};

/**
 * Form of what happened to a nursery batch: a stage change, a loss or a
 * transplant onto a block.
 *
 * The three share the batch and the date. Changing the event empties the
 * fields of the previous one and drops their errors, the API's included, and
 * only the fields of the chosen event are sent. The notes give what the API
 * reports of the batch, its current stage, the plants left in the nursery and
 * its planned block; whether a date or a quantity fits them is left to the
 * API. A transplant also records the planting of the batch's variety on the
 * block, and a variety is planted only once on a block: the form says both.
 *
 * The checks mirror the API's 400 rules (utils/nurseryValidation); what the API
 * still refuses is shown the same way as for a planting:
 * - a 400 with fieldErrors: each message under its field;
 * - a 409 (the variety is already planted on that block, or another planting
 *   of the block was being recorded at the same time), a 422 (a date before
 *   the start or the last stage change, the stage the batch is already at,
 *   more plants than are left), or any other error: one message above the form.
 */
export default function RecordNurseryEventModal({ isSubmitting, error, onSubmit, onResetError, onClose }) {
    const { t } = useTranslation("plants");
    const [eventType, setEventType] = useState("STAGE_CHANGE");
    const [values, setValues] = useState(INITIAL_VALUES);
    // A number field holds "" for text it cannot read, such as "1e"; this
    // keeps such a value from passing as an empty one.
    const [isQuantityUnreadable, setIsQuantityUnreadable] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);

    const batches = useNurseryBatches();
    const { data: blocks } = useVarietyBlocks();
    const batchList = batches.data ?? [];
    const batch = batchList.find((item) => String(item.id) === values.batchId) ?? null;

    let batchNote = null;
    if (batches.isPending) {
        batchNote = t("nursery.batches.loading");
    } else if (batches.isError) {
        batchNote = t("nursery.batches.error");
    } else if (batchList.length === 0) {
        batchNote = t("nursery.batches.empty");
    }
    const canChooseBatch = batches.isSuccess && batchList.length > 0;

    const stageNote = batch && batch.currentStage
        ? t("nursery.eventForm.notes.currentStage", {
            stage: formatEnum("nurseryStage", batch.currentStage),
            date: formatDate(batch.currentStageSince),
        })
        : null;
    const quantityNote = batch
        ? t("nursery.eventForm.notes.inNursery", { count: formatNumber(batch.currentCount) })
        : null;
    const blockNote = (batch?.plannedBlockCode
        ? t("nursery.eventForm.notes.plannedBlock", { block: batch.plannedBlockCode })
        : "") + t("nursery.eventForm.notes.oncePerBlock");
    const transplantNote = t("nursery.eventForm.notes.transplant", {
        variety: batch ? batch.varietyName : t("nursery.eventForm.notes.batchVariety"),
    });

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) =>
        fieldErrors[field] ?? serverFieldErrors?.[field === "date" ? EVENT_DATE_FIELDS[eventType] : field];
    const describedBy = (id, note) => (note ? noteId(id) : undefined);
    const batchDescribedBy = describedBy(FIELD_IDS.batch, batchNote);
    const stageDescribedBy = describedBy(FIELD_IDS.stage, stageNote);
    const quantityDescribedBy = describedBy(FIELD_IDS.quantity, quantityNote);
    const blockDescribedBy = describedBy(FIELD_IDS.block, blockNote);
    const typeOptions = NURSERY_EVENT_TYPES.map((type) => ({ value: type, label: formatEnum("nurseryEvent", type) }));
    const stageOptions = NURSERY_STAGES.map((stage) => ({ value: stage, label: formatEnum("nurseryStage", stage) }));

    function setValue(field, value) {
        setValues((current) => ({ ...current, [field]: value }));
    }

    function changeQuantity(event) {
        setValue("quantity", event.target.value);
        setIsQuantityUnreadable(event.target.validity.badInput);
    }

    function changeType(nextType) {
        const previousFields = TYPE_FIELDS[eventType];
        setValues((current) => ({
            ...current,
            ...Object.fromEntries(previousFields.map((field) => [field, INITIAL_VALUES[field]])),
        }));
        setIsQuantityUnreadable(false);
        setFieldErrors((current) =>
            Object.fromEntries(Object.entries(current).filter(([field]) => !previousFields.includes(field))));
        // The API's answer was about the previous event: none of it applies any more.
        onResetError();
        setEventType(nextType);
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validateEvent(eventType, values, { today, isQuantityUnreadable });
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit({ eventType, batchId: Number(values.batchId), body: eventRequestBody(eventType, values) });
    }

    return (
        <Modal open onClose={onClose} title={t("nursery.eventForm.title")} size="xl">
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                <fieldset className="mb-5">
                    <legend className="text-sm font-medium">{t("nursery.eventForm.event")}</legend>
                    <div className="mt-2 flex flex-wrap gap-x-6 gap-y-2">
                        {typeOptions.map((option) => (
                            <label key={option.value} className="flex items-center gap-2 text-sm text-text">
                                <input
                                    type="radio"
                                    name="nursery-event-type"
                                    value={option.value}
                                    checked={eventType === option.value}
                                    onChange={() => changeType(option.value)}
                                    className={RADIO_CLASS}
                                />
                                {option.label}
                            </label>
                        ))}
                    </div>
                    {eventType === "TRANSPLANT" && <p className="mt-3 text-sm text-text-muted">{transplantNote}</p>}
                </fieldset>

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <NurseryField
                        id="event-batch"
                        label={t("nursery.eventForm.batch")}
                        error={fieldError("batchId")}
                        note={batchNote}
                    >
                        <Select
                            value={values.batchId}
                            disabled={!canChooseBatch}
                            onChange={(event) => setValue("batchId", event.target.value)}
                            aria-describedby={batchDescribedBy}
                        >
                            {/* Empty value: sending without a choice gives "Batch is required." */}
                            <option value="">{t("nursery.eventForm.selectBatch")}</option>
                            {batchList.map((item) => (
                                <option key={item.id} value={item.id}>
                                    {t("nursery.batches.batchOption", { code: item.batchCode, variety: item.varietyName })}
                                </option>
                            ))}
                        </Select>
                    </NurseryField>

                    <NurseryField id="event-date" label={t("nursery.eventForm.date")} error={fieldError("date")}>
                        <Input
                            type="date"
                            max={today}
                            value={values.date}
                            onChange={(event) => setValue("date", event.target.value)}
                        />
                    </NurseryField>

                    {eventType === "STAGE_CHANGE" && (
                        <NurseryField
                            id="event-stage"
                            label={t("nursery.eventForm.stage")}
                            error={fieldError("stage")}
                            note={stageNote}
                        >
                            <Select
                                value={values.stage}
                                onChange={(event) => setValue("stage", event.target.value)}
                                aria-describedby={stageDescribedBy}
                            >
                                {/* Empty value: sending without a choice gives "Stage is required." */}
                                <option value="">{t("nursery.eventForm.selectStage")}</option>
                                {stageOptions.map((option) => (
                                    <option key={option.value} value={option.value}>
                                        {option.label}
                                    </option>
                                ))}
                            </Select>
                        </NurseryField>
                    )}

                    {eventType !== "STAGE_CHANGE" && (
                        <NurseryField
                            id="event-quantity"
                            label={t("nursery.eventForm.quantity")}
                            error={fieldError("quantity")}
                            note={quantityNote}
                        >
                            <Input
                                type="number"
                                min="1"
                                step="1"
                                inputMode="numeric"
                                value={values.quantity}
                                onChange={changeQuantity}
                                aria-describedby={quantityDescribedBy}
                            />
                        </NurseryField>
                    )}

                    {eventType === "LOSS" && (
                        <NurseryField id="event-reason" label={t("nursery.eventForm.reason")} error={fieldError("reason")}>
                            <Input
                                type="text"
                                value={values.reason}
                                onChange={(event) => setValue("reason", event.target.value)}
                            />
                        </NurseryField>
                    )}

                    {eventType === "TRANSPLANT" && (
                        <NurseryField
                            id="event-block"
                            label={t("nursery.eventForm.block")}
                            error={fieldError("blockCode")}
                            note={blockNote}
                        >
                            <Input
                                type="text"
                                list="event-block-options"
                                value={values.blockCode}
                                onChange={(event) => setValue("blockCode", event.target.value)}
                                aria-describedby={blockDescribedBy}
                            />
                            <datalist id="event-block-options">
                                {(blocks ?? []).map((block) => (
                                    <option key={block} value={block} />
                                ))}
                            </datalist>
                        </NurseryField>
                    )}
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button variant="secondary" onClick={onClose}>
                        {t("common:actions.cancel")}
                    </Button>
                    <Button type="submit" disabled={isSubmitting || !canChooseBatch}>
                        {isSubmitting ? t("nursery.eventForm.submitting") : t(`nursery.eventForm.submit.${eventType}`)}
                    </Button>
                </div>
            </form>
        </Modal>
    );
}
