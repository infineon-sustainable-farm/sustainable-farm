import i18n from "../../../shared/i18n/i18n";
import { formatBlock } from "./format";

/** Id of the note on the catalogue, under the findings of the inspection form. */
export const CATALOGUE_NOTE_ID = "inspection-catalogue-note";

/** Code of "Other": a problem outside the catalogue, named by the label typed with it. */
export const OTHER_ISSUE_CODE = "OTHER";

// The catalogue kinds offered in groups, in the order of the API; labels from plants.json.
const GROUPS = [
    { kind: "PEST", labelKey: "health.issues.groups.PEST" },
    { kind: "DISEASE", labelKey: "health.issues.groups.DISEASE" },
];

function t(key, options) {
    return i18n.t(key, { ns: "plants", ...options });
}

/**
 * The problems a form offers, from the catalogue as the API orders it: the
 * pests, then the diseases, then "Other", exactly once and always last.
 *
 * "Other" is never taken from the catalogue. Outside the dev profile the API
 * lists it only once an inspection or a treatment has used it, so every row
 * with its code is dropped and one "Other" is added after the groups, whether
 * the catalogue had it or not, and wherever it had it.
 *
 * The labels are those of the active language, read at each call.
 *
 * @param catalogue the issues of the API, or undefined while they load
 * @returns {{ groups: { label: string, issues: object[] }[], other: { code: string, name: string } }}
 *          the non-empty groups, in order, and "Other"
 */
export function issueOptions(catalogue) {
    const listed = (catalogue ?? []).filter((issue) => issue.code !== OTHER_ISSUE_CODE);
    return {
        groups: GROUPS
            .map(({ kind, labelKey }) => ({ label: t(labelKey), issues: listed.filter((issue) => issue.kind === kind) }))
            .filter((group) => group.issues.length > 0),
        other: { code: OTHER_ISSUE_CODE, name: t("health.issues.other") },
    };
}

/** Where a finding was seen: "Block A", or "Block A, tree 12" when it names its tree. */
export function findingPlace(finding) {
    const block = formatBlock(finding.blockCode);
    return finding.treeLabel ? t("health.place.withTree", { block, tree: finding.treeLabel }) : block;
}

/** Today as "YYYY-MM-DD", in local time: the latest date the health forms accept. */
export function todayAsIsoDate() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${now.getFullYear()}-${month}-${day}`;
}
