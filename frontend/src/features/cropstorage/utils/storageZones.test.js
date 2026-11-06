import { describe, expect, it } from "vitest";
import { getOverallUsagePercent, withZoneUsage } from "./storageZones";

describe("storage zone usage", () => {
    it("treats a zone with no linked batches as genuinely empty", () => {
        const [zone] = withZoneUsage([{ id: 1, capacityKg: 100 }], []);

        expect(zone.usedKg).toBe(0);
        expect(zone.usedPercent).toBe(0);
        expect(getOverallUsagePercent([zone])).toBe(0);
    });

    it("keeps a linked batch with missing or invalid quantity unknown", () => {
        const zone = { id: 1, capacityKg: 100 };
        const missing = withZoneUsage([zone], [{ storageZoneId: 1 }]);
        const invalid = withZoneUsage([zone], [{ storageZoneId: 1, currentQuantityKg: "bad" }]);

        expect(missing[0].usedKg).toBeNull();
        expect(missing[0].usedPercent).toBeNull();
        expect(getOverallUsagePercent(missing)).toBeNull();
        expect(invalid[0].usedPercent).toBeNull();
        expect(getOverallUsagePercent(invalid)).toBeNull();
    });

    it("does not calculate an overall percentage from partial zone capacities", () => {
        const zones = withZoneUsage(
            [{ id: 1, capacityKg: 100 }, { id: 2 }],
            [{ storageZoneId: 1, currentQuantityKg: 25 }],
        );

        expect(zones[0].usedPercent).toBe(25);
        expect(zones[1].usedPercent).toBeNull();
        expect(getOverallUsagePercent(zones)).toBeNull();
    });

    it("links a batch's nested snake-case zone code to a zone with a numeric ID", () => {
        const [zone] = withZoneUsage(
            [{ id: 1, zoneCode: "COLD-A", capacityKg: 100 }],
            [{ storage_zone: { zone_code: "COLD-A" }, current_quantity_kg: 35 }],
        );

        expect(zone.usedKg).toBe(35);
        expect(zone.usedPercent).toBe(35);
    });

    it("keeps a zone with no identifying key unknown", () => {
        const [zone] = withZoneUsage([{ capacityKg: 100 }], []);

        expect(zone.usedKg).toBeNull();
        expect(zone.usedPercent).toBeNull();
    });
});
