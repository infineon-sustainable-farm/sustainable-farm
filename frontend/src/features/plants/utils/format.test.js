import { describe, expect, it } from "vitest";
import { formatPhaseBand } from "./format";

describe("the growth phase of a calendar row", () => {
    it("is named as the API names it, with its year band", () => {
        expect(formatPhaseBand("full production", "6+ yrs")).toBe("Full production · 6+ yrs");
        expect(formatPhaseBand("establishment", "0–2 yrs")).toBe("Establishment · 0–2 yrs");
    });

    it("shows the part the API sent when the other one is missing", () => {
        expect(formatPhaseBand("gradual production", null)).toBe("Gradual production");
        expect(formatPhaseBand(null, "3–5 yrs")).toBe("3–5 yrs");
        expect(formatPhaseBand(" full production ", " 6+ yrs ")).toBe("Full production · 6+ yrs");
    });

    it("is a dash without planting date, when the API sends neither", () => {
        expect(formatPhaseBand(null, null)).toBe("—");
        expect(formatPhaseBand(undefined, "")).toBe("—");
    });
});
