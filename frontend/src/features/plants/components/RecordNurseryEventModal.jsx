import { useState } from "react";
import { X } from "lucide-react";
import { useNurseryBatches } from "../hooks/useNurseryBatches";
import { useVarietyBlocks } from "../hooks/useVarieties";
import { formatCode, formatDate, formatNumber } from "../utils/format";
import {
    EVENT_DATE_FIELDS,
    eventRequestBody,
    NURSERY_EVENT_TYPES,
    NURSERY_STAGES,
} from "../utils/nurseryRequests";
import PlantsModal from "./PlantsModal";

const LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none disabled:cursor-not-allowed disabled:bg-gray-50";
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

// The fields each event adds to the batch and the date.
const TYPE_FIELDS = {
    STAGE_CHANGE: ["stage"],
    LOSS: ["quantity", "reason"],
    TRANSPLANT: ["quantity", "blockCode"],
};

const SUBMIT_LABELS = {
    STAGE_CHANGE: "Record stage change",
    LOSS: "Record loss",
    TRANSPLANT: "Record transplant",
};

const INITIAL_VALUES = {
    batchId: "",
    date: "",
    stage: "",
    quantity: "",
    reason: "",
    blockCode: "",
};

/** Same rules as the API: a whole quantity of up to 9 digits, a block as for a planting. */
const QUANTITY_PATTERN = /^\d{1,9}$/;
const BLOCK_CODE_PATTERN = /^\s*[A-Za-z0-9]{1,10}\s*$/;
const REASON_MAX_LENGTH = 255;

/** Today as "YYYY-MM-DD", in local time: the latest event date the form accepts. */
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
 * The checks mirror the API's 400 rules; what the API still refuses is shown
 * the same way as for a planting:
 * - a 400 with fieldErrors: each message under its field;
 * - a 409 (the variety is already planted on that block, or another planting
 *   of the block was being recorded at the same time), a 422 (a date before
 *   the start or the last stage change, the stage the batch is already at,
 *   more plants than are left), or any other error: one message above the form.
 */
export default function RecordNurseryEventModal({ isSubmitting, error, onSubmit, onResetError, onClose }) {
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
        batchNote = "Loading batches…";
    } else if (batches.isError) {
        batchNote = "Batches could not be loaded";
    } else if (batchList.length === 0) {
        batchNote = "No nursery batch yet";
    }
    const canChooseBatch = batches.isSuccess && batchList.length > 0;

    const stageNote = batch && batch.currentStage
        ? `Current stage: ${formatCode(batch.currentStage)}, since ${formatDate(batch.currentStageSince)}.`
        : null;
    const quantityNote = batch ? `In nursery: ${formatNumber(batch.currentCount)}` : null;
    const blockNote = (batch?.plannedBlockCode ? `Planned block: ${batch.plannedBlockCode}. ` : "")
        + "A variety can be planted only once on a block.";
    const transplantNote = `The transplant also records the planting of ${batch ? batch.varietyName : "the batch's variety"}`
        + " on this block: it then appears in Varieties, Growth Calendar and the forecast.";

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) =>
        fieldErrors[field] ?? serverFieldErrors?.[field === "date" ? EVENT_DATE_FIELDS[eventType] : field];
    const fieldProps = (field, id, note = null) => ({
        "aria-invalid": fieldError(field) ? true : undefined,
        "aria-describedby":
            [fieldError(field) ? `${id}-error` : null, note ? `${id}-note` : null].filter(Boolean).join(" ")
            || undefined,
    });

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

    function validate() {
        const errors = {};
        if (!values.batchId) {
            errors.batchId = "Batch is required.";
        }
        if (!values.date) {
            errors.date = "Date is required.";
        } else if (values.date > today) {
            errors.date = "Date cannot be in the future.";
        }
        if (eventType === "STAGE_CHANGE" && !values.stage) {
            errors.stage = "Stage is required.";
        }
        if (eventType !== "STAGE_CHANGE") {
            const quantity = values.quantity.trim();
            if (isQuantityUnreadable || !QUANTITY_PATTERN.test(quantity) || Number(quantity) < 1) {
                errors.quantity = "Quantity must be a whole number, 1 or more.";
            }
        }
        if (eventType === "LOSS") {
            if (!values.reason.trim()) {
                errors.reason = "Reason is required.";
            } else if (values.reason.length > REASON_MAX_LENGTH) {
                errors.reason = `Reason must be at most ${REASON_MAX_LENGTH} characters.`;
            }
        }
        if (eventType === "TRANSPLANT") {
            if (!values.blockCode.trim()) {
                errors.blockCode = "Block is required.";
            } else if (!BLOCK_CODE_PATTERN.test(values.blockCode)) {
                errors.blockCode = "Block must be a short code such as A or B2, without prefix or space.";
            }
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit({ eventType, batchId: Number(values.batchId), body: eventRequestBody(eventType, values) });
    }

    return (
        <PlantsModal labelledBy="record-event-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h3 id="record-event-title" className="font-heading text-xl font-bold text-gray-900">
                    Record a nursery event
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

                <fieldset className="mb-5">
                    <legend className={LABEL_CLASS}>Event</legend>
                    <div className="mt-2 flex flex-wrap gap-x-6 gap-y-2">
                        {NURSERY_EVENT_TYPES.map((type, index) => (
                            <label key={type} className="flex items-center gap-2 text-sm text-gray-800">
                                <input
                                    type="radio"
                                    name="nursery-event-type"
                                    value={type}
                                    autoFocus={index === 0}
                                    checked={eventType === type}
                                    onChange={() => changeType(type)}
                                    className="accent-primary"
                                />
                                {formatCode(type)}
                            </label>
                        ))}
                    </div>
                    {eventType === "TRANSPLANT" && <p className="mt-3 text-sm text-gray-600">{transplantNote}</p>}
                </fieldset>

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <FormField id="event-batch" label="Batch" error={fieldError("batchId")} note={batchNote}>
                        <select
                            id="event-batch"
                            value={values.batchId}
                            disabled={!canChooseBatch}
                            onChange={(event) => setValue("batchId", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("batchId", "event-batch", batchNote)}
                        >
                            {/* Empty value: sending without a choice gives "Batch is required." */}
                            <option value="">Select a batch</option>
                            {batchList.map((item) => (
                                <option key={item.id} value={item.id}>
                                    {`${item.batchCode} · ${item.varietyName}`}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField id="event-date" label="Date" error={fieldError("date")}>
                        <input
                            id="event-date"
                            type="date"
                            max={today}
                            value={values.date}
                            onChange={(event) => setValue("date", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("date", "event-date")}
                        />
                    </FormField>

                    {eventType === "STAGE_CHANGE" && (
                        <FormField id="event-stage" label="Stage" error={fieldError("stage")} note={stageNote}>
                            <select
                                id="event-stage"
                                value={values.stage}
                                onChange={(event) => setValue("stage", event.target.value)}
                                className={INPUT_CLASS}
                                {...fieldProps("stage", "event-stage", stageNote)}
                            >
                                {/* Empty value: sending without a choice gives "Stage is required." */}
                                <option value="">Select a stage</option>
                                {NURSERY_STAGES.map((stage) => (
                                    <option key={stage} value={stage}>
                                        {formatCode(stage)}
                                    </option>
                                ))}
                            </select>
                        </FormField>
                    )}

                    {eventType !== "STAGE_CHANGE" && (
                        <FormField id="event-quantity" label="Quantity" error={fieldError("quantity")} note={quantityNote}>
                            <input
                                id="event-quantity"
                                type="number"
                                min="1"
                                step="1"
                                inputMode="numeric"
                                value={values.quantity}
                                onChange={changeQuantity}
                                className={INPUT_CLASS}
                                {...fieldProps("quantity", "event-quantity", quantityNote)}
                            />
                        </FormField>
                    )}

                    {eventType === "LOSS" && (
                        <FormField id="event-reason" label="Reason" error={fieldError("reason")}>
                            <input
                                id="event-reason"
                                type="text"
                                value={values.reason}
                                onChange={(event) => setValue("reason", event.target.value)}
                                className={INPUT_CLASS}
                                {...fieldProps("reason", "event-reason")}
                            />
                        </FormField>
                    )}

                    {eventType === "TRANSPLANT" && (
                        <FormField id="event-block" label="Block" error={fieldError("blockCode")} note={blockNote}>
                            <input
                                id="event-block"
                                type="text"
                                list="event-block-options"
                                value={values.blockCode}
                                onChange={(event) => setValue("blockCode", event.target.value)}
                                className={INPUT_CLASS}
                                {...fieldProps("blockCode", "event-block", blockNote)}
                            />
                            <datalist id="event-block-options">
                                {(blocks ?? []).map((block) => (
                                    <option key={block} value={block} />
                                ))}
                            </datalist>
                        </FormField>
                    )}
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
                        disabled={isSubmitting || !canChooseBatch}
                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                    >
                        {isSubmitting ? "Recording…" : SUBMIT_LABELS[eventType]}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
