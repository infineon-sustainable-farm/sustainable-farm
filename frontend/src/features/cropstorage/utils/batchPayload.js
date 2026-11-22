/** Request body of POST /api/crop-storage/batches from the form values (strings). */
const NUMERIC_FIELDS = [
    "harvestId",
    "initialQuantityKg",
    "currentQuantityKg",
    "moisturePercent",
    "customerPriority",
    "valuePerKgFcfa",
];

const TEXT_FIELDS = ["origin", "qualityGrade"];

export function toBatchPayload(values) {
    const payload = { ...values };
    for (const [name, value] of Object.entries(payload)) {
        if (value === "") payload[name] = null;
    }
    for (const name of NUMERIC_FIELDS) {
        if (payload[name] != null) payload[name] = Number(payload[name]);
    }
    for (const name of TEXT_FIELDS) {
        if (payload[name] != null) payload[name] = payload[name].trim() || null;
    }
    payload.batchCode = values.batchCode.trim();
    payload.customerPriority = payload.customerPriority ?? 0;
    return payload;
}

