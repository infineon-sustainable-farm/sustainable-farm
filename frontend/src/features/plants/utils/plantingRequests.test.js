import { describe, expect, it } from "vitest";
import { plantingBody, spacingErrors, VIGOR_OPTIONS } from "./plantingRequests";

const ROW_BOUNDS = "Inter-row spacing must be above 0 and at most 30 m, with at most 1 decimal.";
const TREE_BOUNDS = "Intra-row spacing must be above 0 and at most 30 m, with at most 1 decimal.";

describe("the spacings of a planting", () => {
    it("may both be left out, or both given within the bounds", () => {
        expect(spacingErrors({ rowSpacing: "", treeSpacing: "" })).toEqual({});
        expect(spacingErrors({ rowSpacing: "8", treeSpacing: "7.5" })).toEqual({});
        expect(spacingErrors({ rowSpacing: "30", treeSpacing: "0.1" })).toEqual({});
    });

    it("are refused at 0, above 30 m or with two decimals", () => {
        expect(spacingErrors({ rowSpacing: "0", treeSpacing: "8" })).toEqual({ rowSpacingM: ROW_BOUNDS });
        expect(spacingErrors({ rowSpacing: "31", treeSpacing: "8" })).toEqual({ rowSpacingM: ROW_BOUNDS });
        expect(spacingErrors({ rowSpacing: "8.25", treeSpacing: "8" })).toEqual({ rowSpacingM: ROW_BOUNDS });
        expect(spacingErrors({ rowSpacing: "8", treeSpacing: "-1" })).toEqual({ treeSpacingM: TREE_BOUNDS });
    });

    it("go together: the missing one is reported", () => {
        expect(spacingErrors({ rowSpacing: "8", treeSpacing: "" })).toEqual({
            treeSpacingM: "Intra-row spacing is required when the inter-row spacing is given.",
        });
        expect(spacingErrors({ rowSpacing: " ", treeSpacing: "8" })).toEqual({
            rowSpacingM: "Inter-row spacing is required when the intra-row spacing is given.",
        });
    });
});

describe("the body of a planting", () => {
    const required = { plantingDate: "2019-09-15", blockCode: "A", varietyName: "Keitt", treeCount: "40" };

    it("leaves the optional fields out when they are empty", () => {
        expect(plantingBody({ ...required, rowSpacing: "", treeSpacing: "", vigor: "" })).toEqual({
            plantingDate: "2019-09-15", blockCode: "A", varietyName: "Keitt", treeCount: 40,
        });
    });

    it("sends the spacings as numbers and the vigor as its code", () => {
        expect(plantingBody({ ...required, rowSpacing: "8", treeSpacing: "7.5", vigor: "GOOD" })).toEqual({
            plantingDate: "2019-09-15", blockCode: "A", varietyName: "Keitt", treeCount: 40,
            rowSpacingM: 8, treeSpacingM: 7.5, vigor: "GOOD",
        });
    });

    it("offers the three ratings of the API after Not assessed", () => {
        expect(VIGOR_OPTIONS.map((option) => option.code)).toEqual(["", "GOOD", "AVERAGE", "POOR"]);
    });
});
