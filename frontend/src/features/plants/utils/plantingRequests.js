/**
 * Optional details of a planting: the two spacings and the vigor, checked with
 * the rules of the API so that most mistakes are caught before sending.
 */

/** The vigor ratings of the API, as the form offers them; "" is no rating. */
export const VIGOR_OPTIONS = [
    { code: "", label: "Not assessed" },
    { code: "GOOD", label: "Good" },
    { code: "AVERAGE", label: "Average" },
    { code: "POOR", label: "Poor" },
];

/** Largest spacing the API accepts, in metres. */
const SPACING_MAX_M = 30;

/** A decimal number with at most one decimal, such as 8, 7.5 or .5. */
const ONE_DECIMAL_PATTERN = /^(\d+(\.\d)?|\.\d)$/;

const SPACINGS = [
    { field: "rowSpacingM", key: "rowSpacing", label: "Inter-row spacing" },
    { field: "treeSpacingM", key: "treeSpacing", label: "Intra-row spacing" },
];

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
    for (const { field, key, label } of SPACINGS) {
        if (!isBlank(values[key]) && !isValidSpacing(values[key])) {
            errors[field] = `${label} must be above 0 and at most ${SPACING_MAX_M} m, with at most 1 decimal.`;
        }
    }
    const [row, tree] = SPACINGS;
    const rowBlank = isBlank(values[row.key]);
    const treeBlank = isBlank(values[tree.key]);
    if (rowBlank !== treeBlank) {
        const missing = rowBlank ? row : tree;
        const given = rowBlank ? tree : row;
        errors[missing.field] = `${missing.label} is required when the ${given.label.toLowerCase()} is given.`;
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
