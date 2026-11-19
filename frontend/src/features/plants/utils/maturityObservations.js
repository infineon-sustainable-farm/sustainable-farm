import i18n from "../../../shared/i18n/i18n";
import { formatCode, formatShare } from "./format";

/** Text of the harvest screen in the active language, read at each call. */
function t(key, options) {
    return i18n.t(`plants:harvest.${key}`, options);
}

/**
 * The statuses an observation reports, in the words and the order of the data
 * requirements (PL-05-01). "Harvested" is not one of them.
 */
export const STATUS_OPTIONS = ["IMMATURE", "RIPE", "READY_FOR_HARVEST"].map((code) => ({
    code,
    // Read when the option is shown, so the label follows the active language.
    get label() {
        return i18n.t(`plants:enums.maturityStatus.${code}`);
    },
}));

/**
 * How an observation is made, in the order the form lists them: the label of
 * the form, the shorter name of the table, and the unit of the value each
 * method measures, none for the flesh color and the visual observation.
 */
export const METHOD_OPTIONS = [
    { code: "FLESH_COLOR", unit: null },
    { code: "BRIX", unit: "°Bx" },
    { code: "DRY_MATTER", unit: "%" },
    { code: "VISUAL", unit: null },
].map(({ code, unit }) => ({
    code,
    // Label and name are read when shown, so they follow the active language.
    get label() {
        return t(`methods.${code}.label`);
    },
    get name() {
        return t(`methods.${code}.name`);
    },
    unit,
}));

/** Highest measured value, as the API checks it: °Bx and dry matter are both a percentage. */
const MAX_VALUE = 100;

/** The message of a measured value the API would refuse, in the active language. */
export function measuredValueMessage() {
    return t("observation.errors.measuredValue");
}

/** The same message, in the language active when the module was loaded (English in the tests). */
export const MEASURED_VALUE_ERROR = measuredValueMessage();

/** A measured value with one decimal at most, in the active language. */
function formatDecimal(value) {
    const rounded = Math.round(Number(value) * 10) / 10;
    return formatShare(rounded);
}

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
        return measuredValueMessage();
    }
    return null;
}

/**
 * The request body of an observation, from the values of the form. The
 * measured value goes out as a number, and only with a method that measures
 * one; an empty value or observer is left out. The observer is the id of a
 * person of the farm people directory, sent as a number.
 */
export function observationRequest({ observedOn, blockCode, varietyName, maturityStatus, method, measuredValue, observerPersonId }) {
    const body = { observedOn, blockCode, varietyName, maturityStatus, method };
    const value = String(measuredValue ?? "").trim();
    if (methodTakesValue(method) && value !== "") body.measuredValue = Number(value);
    const observer = String(observerPersonId ?? "").trim();
    if (observer !== "") body.observerPersonId = Number(observer);
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
    const number = formatDecimal(measuredValue);
    if (!measuredUnit) return t("methodValue.plain", { name, value: number });
    return measuredUnit === "%"
        ? t("methodValue.percent", { name, value: number })
        : t("methodValue.unit", { name, value: number, unit: measuredUnit });
}
