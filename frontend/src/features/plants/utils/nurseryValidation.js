import i18n from "../../../shared/i18n/i18n";
import { PURCHASED } from "./nurseryRequests";

/*
 * Checks of the nursery dialogs, the same rules as the API: a batch code of up
 * to 20 letters, digits or hyphens, a block as for a planting, a whole count or
 * quantity of up to 9 digits, and the lengths of the free texts. Spaces around
 * a code are removed by the API. Messages are read in the active language.
 */
const BATCH_CODE_PATTERN = /^\s*[A-Za-z0-9-]{1,20}\s*$/;
const BLOCK_CODE_PATTERN = /^\s*[A-Za-z0-9]{1,10}\s*$/;
const COUNT_PATTERN = /^\d{1,9}$/;
export const TEXT_MAX_LENGTH = 255;
export const LOT_NUMBER_MAX_LENGTH = 50;

function t(key, options) {
    return i18n.t(key, { ns: "plants", ...options });
}

/** Today as "YYYY-MM-DD", in local time: the latest start or event date the forms accept. */
export function todayAsIsoDate() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${now.getFullYear()}-${month}-${day}`;
}

/** Errors of the new batch form, by field; empty when it can be sent. */
export function validateBatch(values, { today, isCountUnreadable }) {
    const errors = {};
    const message = (key, options) => t(`nursery.addForm.errors.${key}`, options);
    if (!values.batchCode.trim()) {
        errors.batchCode = message("batchCodeRequired");
    } else if (!BATCH_CODE_PATTERN.test(values.batchCode)) {
        errors.batchCode = message("batchCodeFormat");
    }
    if (!values.varietyName.trim()) {
        errors.varietyName = message("varietyRequired");
    } else if (values.varietyName.length > TEXT_MAX_LENGTH) {
        errors.varietyName = message("varietyTooLong", { max: TEXT_MAX_LENGTH });
    }
    if (!values.origin) {
        errors.origin = message("originRequired");
    }
    if (values.origin === PURCHASED) {
        if (!values.supplier.trim()) {
            errors.supplier = message("supplierRequired");
        } else if (values.supplier.length > TEXT_MAX_LENGTH) {
            errors.supplier = message("supplierTooLong", { max: TEXT_MAX_LENGTH });
        }
        if (values.supplierLotNumber.trim() && values.supplierLotNumber.length > LOT_NUMBER_MAX_LENGTH) {
            errors.supplierLotNumber = message("lotTooLong", { max: LOT_NUMBER_MAX_LENGTH });
        }
    }
    if (!values.startedOn) {
        errors.startedOn = message("startRequired");
    } else if (values.startedOn > today) {
        errors.startedOn = message("startFuture");
    }
    const count = values.initialCount.trim();
    if (isCountUnreadable || !COUNT_PATTERN.test(count) || Number(count) < 1) {
        errors.initialCount = message("countInvalid");
    }
    if (!values.initialStage) {
        errors.initialStage = message("stageRequired");
    }
    if (!values.plannedTransplantOn) {
        errors.plannedTransplantOn = message("plannedDateRequired");
    }
    if (values.plannedBlockCode.trim() && !BLOCK_CODE_PATTERN.test(values.plannedBlockCode)) {
        errors.plannedBlockCode = message("plannedBlockFormat");
    }
    return errors;
}

/** Errors of the event form, by field; empty when it can be sent. */
export function validateEvent(eventType, values, { today, isQuantityUnreadable }) {
    const errors = {};
    const message = (key, options) => t(`nursery.eventForm.errors.${key}`, options);
    if (!values.batchId) {
        errors.batchId = message("batchRequired");
    }
    if (!values.date) {
        errors.date = message("dateRequired");
    } else if (values.date > today) {
        errors.date = message("dateFuture");
    }
    if (eventType === "STAGE_CHANGE" && !values.stage) {
        errors.stage = message("stageRequired");
    }
    if (eventType !== "STAGE_CHANGE") {
        const quantity = values.quantity.trim();
        if (isQuantityUnreadable || !COUNT_PATTERN.test(quantity) || Number(quantity) < 1) {
            errors.quantity = message("quantityInvalid");
        }
    }
    if (eventType === "LOSS") {
        if (!values.reason.trim()) {
            errors.reason = message("reasonRequired");
        } else if (values.reason.length > TEXT_MAX_LENGTH) {
            errors.reason = message("reasonTooLong", { max: TEXT_MAX_LENGTH });
        }
    }
    if (eventType === "TRANSPLANT") {
        if (!values.blockCode.trim()) {
            errors.blockCode = message("blockRequired");
        } else if (!BLOCK_CODE_PATTERN.test(values.blockCode)) {
            errors.blockCode = message("blockFormat");
        }
    }
    return errors;
}
