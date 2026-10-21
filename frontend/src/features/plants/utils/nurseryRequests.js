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
