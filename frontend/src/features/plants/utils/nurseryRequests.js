/*
 * Request bodies of the nursery dialogs, kept apart from them so their rules
 * can be tested: only the fields the API takes for the choice made are sent,
 * and an optional field left empty is not sent at all. Values go out as typed;
 * the API trims them and upper-cases the codes. No farm is sent, as for a
 * planting.
 */

/** The stages the API records, in the order a batch usually goes through them. */
export const NURSERY_STAGES = ["GERMINATION", "ROOTSTOCK_GROWTH", "GRAFTED", "HARDENING", "READY_TO_TRANSPLANT"];

/** Where the plants of a batch come from: sown on the farm, or bought from a supplier. */
export const IN_HOUSE = "IN_HOUSE";
export const PURCHASED = "PURCHASED";
export const NURSERY_ORIGINS = [IN_HOUSE, PURCHASED];

/**
 * Body of a new batch, from the values of its form.
 *
 * The supplier and its lot number go with a purchase only: the API refuses
 * them for plants grown on the farm, so whatever the fields still hold is
 * left out. The lot number and the planned block are optional.
 */
export function batchRequestBody(values) {
    const body = {
        batchCode: values.batchCode,
        varietyName: values.varietyName,
        origin: values.origin,
        startedOn: values.startedOn,
        initialCount: Number(values.initialCount.trim()),
        initialStage: values.initialStage,
        plannedTransplantOn: values.plannedTransplantOn,
    };
    if (values.origin === PURCHASED) {
        body.supplier = values.supplier;
        if (values.supplierLotNumber.trim()) body.supplierLotNumber = values.supplierLotNumber;
    }
    if (values.plannedBlockCode.trim()) body.plannedBlockCode = values.plannedBlockCode;
    return body;
}

/** The events a batch can record, as the API names them. */
export const NURSERY_EVENT_TYPES = ["STAGE_CHANGE", "LOSS", "TRANSPLANT"];

/** Each API route names the date after its event. */
export const EVENT_DATE_FIELDS = {
    STAGE_CHANGE: "changedOn",
    LOSS: "lostOn",
    TRANSPLANT: "transplantedOn",
};

/**
 * Body of a stage change, a loss or a transplant, from the values of the
 * event form: the date under the name its route gives it, and the fields of
 * that event only, whatever the fields of another event still hold.
 */
export function eventRequestBody(eventType, values) {
    const body = { [EVENT_DATE_FIELDS[eventType]]: values.date };
    if (eventType === "STAGE_CHANGE") {
        body.stage = values.stage;
    } else {
        body.quantity = Number(values.quantity.trim());
        if (eventType === "LOSS") {
            body.reason = values.reason;
        } else {
            body.blockCode = values.blockCode;
        }
    }
    return body;
}
