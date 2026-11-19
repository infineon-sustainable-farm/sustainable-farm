import i18n from "../../../shared/i18n/i18n";

/**
 * Optional details of a planting: the two spacings and the vigor, checked with
 * the rules of the API so that most mistakes are caught before sending.
 */

/**
 * The vigor ratings of the API, as the form offers them; "" is no rating. `labelKey` is the
 * translation key of the label, in the plants namespace.
 */
export const VIGOR_OPTIONS = [
    { code: "", labelKey: "varieties.planting.notAssessed" },
    { code: "GOOD", labelKey: "enums.vigor.GOOD" },
    { code: "AVERAGE", labelKey: "enums.vigor.AVERAGE" },
    { code: "POOR", labelKey: "enums.vigor.POOR" },
];

/** Largest spacing the API accepts, in metres. */
const SPACING_MAX_M = 30;

/** A decimal number with at most one decimal, such as 8, 7.5 or .5. */
const ONE_DECIMAL_PATTERN = /^(\d+(\.\d)?|\.\d)$/;

// Messages in the active language, from plants.json (varieties.validation).
const SPACINGS = [
    { field: "rowSpacingM", key: "rowSpacing", bounds: "rowSpacingBounds", required: "rowSpacingRequired" },
    { field: "treeSpacingM", key: "treeSpacing", bounds: "treeSpacingBounds", required: "treeSpacingRequired" },
];

function message(key, options) {
    return i18n.t(`varieties.validation.${key}`, { ns: "plants", ...options });
}

function isBlank(value) {
    return value === undefined || value === null || String(value).trim() === "";
}

function isValidSpacing(value) {
    const text = String(value).trim();
    if (!ONE_DECIMAL_PATTERN.test(text)) return false;
    const metres = Number(text);
    return metres > 0 && metres <= SPACING_MAX_M;
}

/**
 * The errors of the two spacings, by API field: each one above 0 and at most
 * 30 m with at most one decimal, and both or none.
 *
 * @param {{ rowSpacing: string, treeSpacing: string }} values the spacings as typed
 * @returns {object} the message of each failing field, empty when both are valid
 */
export function spacingErrors(values) {
    const errors = {};
    for (const { field, key, bounds } of SPACINGS) {
        if (!isBlank(values[key]) && !isValidSpacing(values[key])) {
            errors[field] = message(bounds, { max: SPACING_MAX_M });
        }
    }
    const [row, tree] = SPACINGS;
    const rowBlank = isBlank(values[row.key]);
    const treeBlank = isBlank(values[tree.key]);
    if (rowBlank !== treeBlank) {
        const missing = rowBlank ? row : tree;
        errors[missing.field] = message(missing.required);
    }
    return errors;
}

/**
 * The body of POST /plantings. The optional fields left empty are not sent at
 * all; the spacings go out as numbers, the vigor as its code.
 *
 * @param {object} values the form values, spacings and vigor as typed
 * @returns {object} the request body
 */
export function plantingBody({ plantingDate, blockCode, varietyName, treeCount, rowSpacing, treeSpacing, vigor }) {
    const body = { plantingDate, blockCode, varietyName, treeCount: Number(treeCount) };
    if (!isBlank(rowSpacing)) body.rowSpacingM = Number(String(rowSpacing).trim());
    if (!isBlank(treeSpacing)) body.treeSpacingM = Number(String(treeSpacing).trim());
    if (!isBlank(vigor)) body.vigor = vigor;
    return body;
}
