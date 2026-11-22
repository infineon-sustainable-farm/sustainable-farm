import { describe, expect, it } from "vitest";
import { varietyLabel } from "./varieties";
import { findMatchingBatches, getBatchProductLabel } from "./batchSearch";

const FRENCH = { "varieties.OTHER": "Autre", "enums.productType.DRIED_MANGO": "Mangue séchée" };
const t = (key, options) => FRENCH[key] ?? options?.defaultValue ?? key;

describe("mango variety labels", () => {
    it("translates the catalogue entry Other and keeps cultivar names as Plants names them", () => {
        expect(varietyLabel(t, "OTHER", "Other")).toBe("Autre");
        expect(varietyLabel(t, "KENT", "Kent")).toBe("Kent");
        expect(varietyLabel(t, "TOMMY", "Tommy Atkins")).toBe("Tommy Atkins");
    });

    it("shows the Plants name without a translation function and nothing without a variety", () => {
        expect(varietyLabel(undefined, "OTHER", "Other")).toBe("Other");
        expect(varietyLabel(t, null, null)).toBeNull();
    });

    it("shows and finds a batch by its translated variety", () => {
        const batch = { id: 1, batchCode: "DEMO-B-009", varietyKey: "OTHER", mangoVariety: "Other", productType: "DRIED_MANGO" };

        expect(getBatchProductLabel(batch, t)).toBe("Autre · Mangue séchée");
        expect(findMatchingBatches([batch], "autre", t)).toEqual([batch]);
        expect(findMatchingBatches([batch], "other", t)).toEqual([batch]);
    });
});
