import { describe, expect, it } from "vitest";
import { issueOptions, OTHER_ISSUE_CODE } from "./healthIssues";

/** The codes in the order a form lists them: each group, then "Other". */
function listedCodes(options) {
    return [...options.groups.flatMap((group) => group.issues.map((issue) => issue.code)), options.other.code];
}

function issue(id, code, name, kind) {
    return { id, code, name, kind, scientificName: null, eppoCode: null, source: "user_entry" };
}

// The catalogue of the dev profile, in the order of the API.
const DEV_CATALOGUE = [
    issue(1, "FRUIT_FLY", "Fruit flies", "PEST"),
    issue(2, "MANGO_MEALYBUG", "Mango mealybug", "PEST"),
    issue(4, "SCALE_INSECTS", "Scale insects", "PEST"),
    issue(3, "TERMITES", "Termites", "PEST"),
    issue(5, "ANTHRACNOSE", "Anthracnose", "DISEASE"),
    issue(6, "BACTERIAL_BLACK_SPOT", "Bacterial black spot", "DISEASE"),
    issue(7, "MANGO_DECLINE", "Mango decline", "DISEASE"),
    issue(8, "OTHER", "Other", "OTHER"),
];

describe("issueOptions", () => {
    it("offers Other alone for an empty catalogue, as outside the dev profile", () => {
        const options = issueOptions([]);

        expect(options.groups).toEqual([]);
        expect(listedCodes(options)).toEqual([OTHER_ISSUE_CODE]);
        expect(options.other).toEqual({ code: "OTHER", name: "Other" });
    });

    it("offers Other alone while the catalogue loads", () => {
        expect(listedCodes(issueOptions(undefined))).toEqual([OTHER_ISSUE_CODE]);
    });

    it("lists the pests, then the diseases, then Other once, for the dev catalogue", () => {
        const options = issueOptions(DEV_CATALOGUE);

        expect(options.groups.map((group) => group.label)).toEqual(["Pests", "Diseases"]);
        expect(listedCodes(options)).toEqual([
            "FRUIT_FLY",
            "MANGO_MEALYBUG",
            "SCALE_INSECTS",
            "TERMITES",
            "ANTHRACNOSE",
            "BACTERIAL_BLACK_SPOT",
            "MANGO_DECLINE",
            "OTHER",
        ]);
    });

    it("moves Other last when the catalogue does not list it last", () => {
        const options = issueOptions([
            issue(8, "OTHER", "Other", "OTHER"),
            issue(1, "FRUIT_FLY", "Fruit flies", "PEST"),
            issue(5, "ANTHRACNOSE", "Anthracnose", "DISEASE"),
        ]);

        const codes = listedCodes(options);
        expect(codes).toEqual(["FRUIT_FLY", "ANTHRACNOSE", "OTHER"]);
        expect(codes.filter((code) => code === OTHER_ISSUE_CODE)).toHaveLength(1);
    });

    it("lists the pests then Other when the catalogue holds pests only", () => {
        const options = issueOptions([
            issue(1, "FRUIT_FLY", "Fruit flies", "PEST"),
            issue(3, "TERMITES", "Termites", "PEST"),
        ]);

        expect(options.groups.map((group) => group.label)).toEqual(["Pests"]);
        expect(listedCodes(options)).toEqual(["FRUIT_FLY", "TERMITES", "OTHER"]);
    });
});
