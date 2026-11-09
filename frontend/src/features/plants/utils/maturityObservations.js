import { formatCode } from "./format";

/**
 * The statuses an observation reports, in the words and the order of the data
 * requirements (PL-05-01). "Harvested" is not one of them.
 */
export const STATUS_OPTIONS = [
    { code: "IMMATURE", label: "Immature" },
    { code: "RIPE", label: "Ripe" },
    { code: "READY_FOR_HARVEST", label: "Ready for harvest" },
];

/**
 * How an observation is made, in the order the form lists them: the label of
 * the form, the shorter name of the table, and the unit of the value each
 * method measures, none for the flesh color and the visual observation.
 */
export const METHOD_OPTIONS = [
    { code: "FLESH_COLOR", label: "Flesh color near the stone", name: "Flesh color", unit: null },
    { code: "BRIX", label: "Brix (°Bx)", name: "Brix", unit: "°Bx" },
    { code: "DRY_MATTER", label: "Dry matter (%)", name: "Dry matter", unit: "%" },
    { code: "VISUAL", label: "Visual (shoulders, skin)", name: "Visual", unit: null },
];

/** Highest measured value, as the API checks it: °Bx and dry matter are both a percentage. */
const MAX_VALUE = 100;

/** The message of a measured value the API would refuse. */
export const MEASURED_VALUE_ERROR = "Measured value must be above 0 and at most 100, with one decimal at most.";

function methodOption(code) {
    return METHOD_OPTIONS.find((option) => option.code === code) ?? null;
}

/** Unit of the value a method measures; null when it measures none, or for an unknown code. */
export function methodUnit(code) {
    return methodOption(code)?.unit ?? null;
}

/** Whether an observation made this way carries a measured value: Brix and dry matter only. */
export function methodTakesValue(code) {
    return methodUnit(code) !== null;
}

/**
 * The error of a typed measured value, with the rule of the API: above 0, at
 * most 100, one decimal at most. An empty field is no value, which is allowed.
 */
export function measuredValueError(text) {
    const value = String(text ?? "").trim();
    if (value === "") return null;
    const number = Number(value);
    if (!/^\d*(\.\d?)?$/.test(value) || !Number.isFinite(number) || number <= 0 || number > MAX_VALUE) {
        return MEASURED_VALUE_ERROR;
    }
    return null;
}

/**
 * The request body of an observation, from the values of the form. The
 * measured value goes out as a number, and only with a method that measures
 * one; an empty value or observer is left out, and the observer is trimmed.
 */
export function observationRequest({ observedOn, blockCode, varietyName, maturityStatus, method, measuredValue, observer }) {
    const body = { observedOn, blockCode, varietyName, maturityStatus, method };
    const value = String(measuredValue ?? "").trim();
    if (methodTakesValue(method) && value !== "") body.measuredValue = Number(value);
    const name = String(observer ?? "").trim();
    if (name !== "") body.observer = name;
    return body;
}

/**
 * How an observation was made, with the value read when there is one:
 * "Brix 14.2 °Bx", "Dry matter 15%", "Visual". The unit is the one the API
 * sends with the value; an unknown method is shown in sentence case.
 */
export function formatObservationMethod(method, measuredValue, measuredUnit) {
    const name = methodOption(method)?.name ?? formatCode(method);
    if (measuredValue === null || measuredValue === undefined) return name;
    const number = Number(measuredValue).toLocaleString("en-US", { maximumFractionDigits: 1 });
    if (!measuredUnit) return `${name} ${number}`;
    return measuredUnit === "%" ? `${name} ${number}%` : `${name} ${number} ${measuredUnit}`;
}
