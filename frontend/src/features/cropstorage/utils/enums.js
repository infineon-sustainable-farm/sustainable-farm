// Fixed values of the Crop Storage API (backend enums). Labels come from
// cropstorage:enums.<group>.<VALUE>.
/** Translation groups of the enum labels (cropstorage:enums.<group>). */
export const GROUP = Object.freeze({
    batchStatus: "batchStatus",
    operationalStatus: "operationalStatus",
    productType: "productType",
    productStage: "productStage",
    movementType: "movementType",
    alertStatus: "alertStatus",
    alertType: "alertType",
    severity: "severity",
});

export const BATCH_STATUSES = ["STORED", "DISPATCHED"];
export const OPERATIONAL_STATUSES = ["ACTIVE", "MAINTENANCE", "INACTIVE"];
export const PRODUCT_TYPES = ["FRESH_MANGO", "DRIED_MANGO"];
export const PRODUCT_STAGES = ["FRESH", "DRIED", "PACKAGED", "WASTE", "RETURNED"];
export const MOVEMENT_TYPES = ["IN", "OUT", "TRANSFER"];
export const ALERT_STATUSES = ["ACTIVE", "ACKNOWLEDGED", "RESOLVED"];

/** Translated label of an enum value; unknown values are shown as received. */
export function enumLabel(t, group, value) {
    if (value == null || value === "") return "—";
    return t(`enums.${group}.${value}`, { defaultValue: String(value) });
}

/** Options of a kit Select for an enum group. */
export function enumOptions(t, group, values) {
    return values.map((value) => ({ value, label: enumLabel(t, group, value) }));
}
