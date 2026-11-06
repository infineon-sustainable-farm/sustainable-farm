import { describe, expect, it } from "vitest";
import { alertPlace, alertTypeLabel, formatRelativeDays } from "./overviewLabels";

describe("alertTypeLabel", () => {
    it("words each known type and points at its screen", () => {
        expect(alertTypeLabel("OPEN_HEALTH_ISSUE")).toEqual({
            issue: "Health issue still open",
            screen: "Diseases & Alerts",
            path: "/plants/diseases-alerts",
        });
        expect(alertTypeLabel("LOW_STOCK").path).toBe("/plants/fertilizer-inventory");
        expect(alertTypeLabel("LOW_STOCK").undated).toBe("Now");
        expect(alertTypeLabel("NURSERY_READY").undated).toBeUndefined();
        expect(alertTypeLabel("NURSERY_READY").path).toBe("/plants/nursery");
        expect(alertTypeLabel("PRE_HARVEST_INTERVAL").issue).toBe("Pre-harvest interval running");
        expect(alertTypeLabel("HARVEST_APPROACHING").path).toBe("/plants/harvest-maturity");
    });

    it("shows a type it does not know in sentence case", () => {
        expect(alertTypeLabel("FROST_RISK")).toEqual({
            issue: "Frost risk",
            screen: "Diseases & Alerts",
            path: "/plants/diseases-alerts",
        });
    });
});

describe("alertPlace", () => {
    it("names the inventory for a stock alert", () => {
        expect(alertPlace({ type: "LOW_STOCK", blockCode: null, fertilizerId: 1 })).toBe("Inventory");
    });

    it("names the batch when its code is known, the nursery otherwise", () => {
        const alert = { type: "NURSERY_READY", blockCode: "B", batchId: 1 };
        expect(alertPlace(alert, { batchCode: "P1" })).toBe("Batch P1");
        expect(alertPlace(alert)).toBe("Nursery");
    });

    it("names the block, and the tree when it is known", () => {
        const alert = { type: "OPEN_HEALTH_ISSUE", blockCode: "A", findingId: 3 };
        expect(alertPlace(alert, { treeLabel: "12" })).toBe("Block A · Tree 12");
        expect(alertPlace(alert)).toBe("Block A");
        expect(alertPlace({ type: "PRE_HARVEST_INTERVAL", blockCode: "A" })).toBe("Block A");
    });

    it("keeps the block alone for a pre-harvest interval, even when its finding has a tree", () => {
        const alert = { type: "PRE_HARVEST_INTERVAL", blockCode: "A", findingId: 3, treatmentId: 1 };
        expect(alertPlace(alert, { treeLabel: "12" })).toBe("Block A");
    });
});

describe("formatRelativeDays", () => {
    it("words days ahead, today and days past", () => {
        expect(formatRelativeDays(9)).toBe("in 9 days");
        expect(formatRelativeDays(1)).toBe("in 1 day");
        expect(formatRelativeDays(0)).toBe("today");
        expect(formatRelativeDays(-7)).toBe("7 days ago");
        expect(formatRelativeDays(-1)).toBe("1 day ago");
        expect(formatRelativeDays(1076)).toBe("in 1,076 days");
    });

    it("gives nothing for an alert without a date", () => {
        expect(formatRelativeDays(null)).toBeNull();
        expect(formatRelativeDays(undefined)).toBeNull();
        expect(formatRelativeDays(1.5)).toBeNull();
    });
});
