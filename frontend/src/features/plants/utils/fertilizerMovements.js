import i18n from "../../../shared/i18n/i18n";

/*
 * Rules of the "Record a movement" form, apart from its fields: the movement types, the fields each
 * adds, the date field of each API route, the checks (the API's own rules) and the request body.
 */

export const MOVEMENT_TYPES = ["PURCHASE", "APPLICATION", "LOSS"];

// The fields each type adds to the fertilizer, the date and the quantity.
export const TYPE_FIELDS = {
    PURCHASE: ["supplier", "totalCost", "currency"],
    APPLICATION: ["blockCode", "applicatorPersonId", "method"],
    LOSS: ["reason"],
};

// Each API route names the date after its movement.
export const DATE_FIELDS = {
    PURCHASE: "purchaseDate",
    APPLICATION: "applicationDate",
    LOSS: "lossDate",
};

// XOF, the farm's currency, is the default of the API as well. Currency symbols, the same in every language.
export const CURRENCIES = [
    { code: "XOF", label: "FCFA" },
    { code: "EUR", label: "EUR" },
];

export const INITIAL_VALUES = {
    fertilizerId: "",
    date: "",
    quantity: "",
    supplier: "",
    totalCost: "",
    currency: "XOF",
    blockCode: "",
    applicatorPersonId: "",
    method: "",
    reason: "",
};

/** Same rules as the API: a quantity above 0 with at most 3 decimals, a cost with at most 2. */
const QUANTITY_PATTERN = /^\d+(\.\d{1,3})?$/;
const COST_PATTERN = /^\d+(\.\d{1,2})?$/;

/** Same rule as a planting: a short code such as "A" or "B2"; spaces around it are removed. */
const BLOCK_CODE_PATTERN = /^\s*[A-Za-z0-9]{1,10}\s*$/;

/** Today as "YYYY-MM-DD", in local time: the latest movement date the form accepts. */
export function todayAsIsoDate() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${now.getFullYear()}-${month}-${day}`;
}

function isPositiveNumber(text, pattern) {
    const trimmed = text.trim();
    return pattern.test(trimmed) && Number(trimmed) > 0;
}

function message(key) {
    return i18n.t(`plants:fertilizer.movementForm.errors.${key}`);
}

/**
 * Errors of the form by field, in the active language; empty when it can be sent.
 * `unreadable` tells which number fields hold text the browser cannot read.
 */
export function validateMovement(movementType, values, unreadable, today) {
    const errors = {};
    if (!values.fertilizerId) {
        errors.fertilizerId = message("fertilizerRequired");
    }
    if (!values.date) {
        errors.date = message("dateRequired");
    } else if (values.date > today) {
        errors.date = message("dateFuture");
    }
    if (unreadable.quantity || !isPositiveNumber(values.quantity, QUANTITY_PATTERN)) {
        errors.quantity = message("quantityInvalid");
    }
    if (movementType === "PURCHASE") {
        if (!values.supplier.trim()) {
            errors.supplier = message("supplierRequired");
        } else if (values.supplier.length > 255) {
            errors.supplier = message("supplierTooLong");
        }
        if (unreadable.totalCost
            || (values.totalCost.trim() !== "" && !isPositiveNumber(values.totalCost, COST_PATTERN))) {
            errors.totalCost = message("totalCostInvalid");
        }
    }
    if (movementType === "APPLICATION") {
        if (!values.blockCode.trim()) {
            errors.blockCode = message("blockRequired");
        } else if (!BLOCK_CODE_PATTERN.test(values.blockCode)) {
            errors.blockCode = message("blockInvalid");
        }
        if (!values.applicatorPersonId) {
            errors.applicatorPersonId = message("applicatorRequired");
        }
        if (values.method.length > 255) {
            errors.method = message("methodTooLong");
        }
    }
    if (movementType === "LOSS") {
        if (!values.reason.trim()) {
            errors.reason = message("reasonRequired");
        } else if (values.reason.length > 255) {
            errors.reason = message("reasonTooLong");
        }
    }
    return errors;
}

/** The request body of the chosen type; optional fields left empty are not sent. */
export function movementRequestBody(movementType, values) {
    const body = { [DATE_FIELDS[movementType]]: values.date, quantity: Number(values.quantity) };
    if (movementType === "PURCHASE") {
        body.supplier = values.supplier;
        if (values.totalCost.trim()) {
            body.totalCost = Number(values.totalCost);
            body.currency = values.currency;
        }
    } else if (movementType === "APPLICATION") {
        body.blockCode = values.blockCode;
        body.applicatorPersonId = Number(values.applicatorPersonId);
        if (values.method.trim()) body.method = values.method;
    } else {
        body.reason = values.reason;
    }
    return body;
}
