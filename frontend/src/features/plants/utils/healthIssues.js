/** Code of "Other": a problem outside the catalogue, named by the label typed with it. */
export const OTHER_ISSUE_CODE = "OTHER";

// The catalogue kinds offered in groups, in the order of the API.
const GROUPS = [
    { kind: "PEST", label: "Pests" },
    { kind: "DISEASE", label: "Diseases" },
];

/**
 * The problems a form offers, from the catalogue as the API orders it: the
 * pests, then the diseases, then "Other", exactly once and always last.
 *
 * "Other" is never taken from the catalogue. Outside the dev profile the API
 * lists it only once an inspection or a treatment has used it, so every row
 * with its code is dropped and one "Other" is added after the groups, whether
 * the catalogue had it or not, and wherever it had it.
 *
 * @param catalogue the issues of the API, or undefined while they load
 * @returns {{ groups: { label: string, issues: object[] }[], other: { code: string, name: string } }}
 *          the non-empty groups, in order, and "Other"
 */
export function issueOptions(catalogue) {
    const listed = (catalogue ?? []).filter((issue) => issue.code !== OTHER_ISSUE_CODE);
    return {
        groups: GROUPS
            .map(({ kind, label }) => ({ label, issues: listed.filter((issue) => issue.kind === kind) }))
            .filter((group) => group.issues.length > 0),
        other: { code: OTHER_ISSUE_CODE, name: "Other" },
    };
}
