import { describe, expect, it } from "vitest";
import {
    OUTLOOK,
    SCENARIO_ASSUMPTIONS,
    SCENARIO_PRESETS,
    buildScenarioSummary,
    computeScenario,
    formatBreakEven,
    formatFcfa,
    matchingPreset,
    normalizeScenarioInputs,
} from "./scenario";

// Synthetic prototype reference results, not farm data.
const REFERENCE = {
    small: {
        investment: 7750000, added: 75, used: 20, cost: 205000,
        gross: 3725000, net: 3520000, months: 27, fiveYear: 9850000,
        outlook: OUTLOOK.PROFITABLE,
    },
    full: {
        investment: 28750000, added: 225, used: 20, cost: 1285000,
        gross: 5415500, net: 4130500, months: 84, fiveYear: -8097500,
        outlook: OUTLOOK.BEYOND,
    },
    solar: {
        investment: 3500000, added: 0, used: 0, cost: 70000,
        gross: 1200000, net: 1130000, months: 38, fiveYear: 2150000,
        outlook: OUTLOOK.PROFITABLE,
    },
};

describe("computeScenario reproduces the prototype presets", () => {
    it.each(Object.keys(REFERENCE))("%s preset", (key) => {
        const expected = REFERENCE[key];
        const result = computeScenario(SCENARIO_PRESETS[key]);

        expect(result.investment).toBeCloseTo(expected.investment, 6);
        expect(result.addedCapacityTonnes).toBeCloseTo(expected.added, 6);
        expect(result.usedAddedCapacityTonnes).toBeCloseTo(expected.used, 6);
        expect(result.annualCost).toBeCloseTo(expected.cost, 6);
        expect(result.annualGrossBenefit).toBeCloseTo(expected.gross, 4);
        expect(result.annualNetBenefit).toBeCloseTo(expected.net, 4);
        expect(result.breakEvenMonths).toBe(expected.months);
        expect(result.fiveYearNetGain).toBeCloseTo(expected.fiveYear, 4);
        expect(result.outlook).toBe(expected.outlook);
    });

    it("keeps the presets exactly as specified", () => {
        expect(SCENARIO_PRESETS).toEqual({
            small: { storageM2: 50, sensors: 10, cooling: false, solar: false },
            full: { storageM2: 150, sensors: 30, cooling: true, solar: true },
            solar: { storageM2: 0, sensors: 0, cooling: false, solar: true },
        });
    });
});

describe("scenario rules", () => {
    it("caps the sensor effect at 10 effective sensors but still costs every sensor", () => {
        const ten = computeScenario({ storageM2: 0, sensors: 10, cooling: false, solar: false });
        const fifty = computeScenario({ storageM2: 0, sensors: 50, cooling: false, solar: false });

        expect(ten.lossReductionPts).toBeCloseTo(5, 10);
        expect(fifty.lossReductionPts).toBeCloseTo(5, 10);
        expect(fifty.investment).toBe(50 * SCENARIO_ASSUMPTIONS.costPerSensor);
        expect(fifty.annualCost).toBeCloseTo(50 * 25000 * 0.1, 6);
    });

    it("applies cooling as a 30 % relative cut on the remaining loss, reported in points", () => {
        const result = computeScenario({ storageM2: 0, sensors: 0, cooling: true, solar: false });

        expect(result.lossRatePct).toBeCloseTo(5.6, 10);
        expect(result.lossReductionPts).toBeCloseTo(2.4, 10);
    });

    it("clamps slider values to their range and step", () => {
        expect(normalizeScenarioInputs({ storageM2: 999, sensors: -4 })).toEqual({
            storageM2: 200, sensors: 0, cooling: false, solar: false,
        });
        expect(normalizeScenarioInputs({ storageM2: 44, sensors: 7.6 }).storageM2).toBe(40);
        expect(normalizeScenarioInputs({ storageM2: "x", sensors: null }).storageM2).toBe(0);
    });

    it("handles the slider bounds 0 and 200 m² / 50 sensors", () => {
        const max = computeScenario({ storageM2: 200, sensors: 50, cooling: true, solar: true });
        expect(max.investment).toBe(200 * 150000 + 50 * 25000 + 2000000 + 3500000);
        expect(max.usedAddedCapacityTonnes).toBe(20);
    });

    it("gives no break-even and a non-viable outlook when nothing is invested", () => {
        const result = computeScenario({ storageM2: 0, sensors: 0, cooling: false, solar: false });

        expect(result.investment).toBe(0);
        expect(result.annualNetBenefit).toBe(0);
        expect(result.breakEvenMonths).toBeNull();
        expect(result.outlook).toBe(OUTLOOK.NOT_VIABLE);
        expect(formatBreakEven(result.breakEvenMonths)).toBe("No break-even");
    });

    it("reports a non-viable model when annual net benefit is negative", () => {
        // With the prototype values cooling alone stays positive (+588,000 FCFA/year),
        // so the operating cost is raised to exercise the negative branch.
        const assumptions = { ...SCENARIO_ASSUMPTIONS, coolingOperatingCost: 2000000 };
        const result = computeScenario(
            { storageM2: 0, sensors: 0, cooling: true, solar: false },
            assumptions,
        );

        expect(result.annualNetBenefit).toBeLessThan(0);
        expect(result.breakEvenMonths).toBeNull();
        expect(result.outlook).toBe(OUTLOOK.NOT_VIABLE);
        for (const value of Object.values(result)) {
            if (typeof value === "number") expect(Number.isFinite(value)).toBe(true);
        }
    });

    it("returns an immediate break-even when net benefit is positive without investment", () => {
        const assumptions = { ...SCENARIO_ASSUMPTIONS, solarCost: 0 };
        const result = computeScenario(
            { storageM2: 0, sensors: 0, cooling: false, solar: true },
            assumptions,
        );
        expect(result.investment).toBe(0);
        expect(result.breakEvenMonths).toBe(0);
        expect(formatBreakEven(0)).toBe("Immediate");
    });

    it("projects five cumulative years", () => {
        const result = computeScenario(SCENARIO_PRESETS.small);

        expect(result.projection.map((row) => row.year)).toEqual([1, 2, 3, 4, 5]);
        expect(result.projection[0].cumulativeCost).toBeCloseTo(7750000 + 205000, 6);
        expect(result.projection[4].cumulativeBenefit).toBeCloseTo(3725000 * 5, 4);
        expect(result.projection[4].cumulativeNet).toBeCloseTo(result.fiveYearNetGain, 6);
    });

    it("recognises presets and manual combinations", () => {
        expect(matchingPreset(SCENARIO_PRESETS.full)).toBe("full");
        expect(matchingPreset({ storageM2: 60, sensors: 10, cooling: false, solar: false })).toBeNull();
    });

    it("formats FCFA to the unit and builds a text summary", () => {
        expect(formatFcfa(9849999.6)).toBe("9,850,000 FCFA");
        expect(formatFcfa(-8097500)).toBe("−8,097,500 FCFA");
        expect(formatBreakEven(27)).toBe("27 months (2.3 years)");

        const summary = buildScenarioSummary(computeScenario(SCENARIO_PRESETS.small));
        expect(summary).toContain("Investment: 7,750,000 FCFA");
        expect(summary).toContain("Break-even: 27 months (2.3 years)");
        expect(summary).toContain("5-year net: +9,850,000 FCFA");
        expect(summary).toContain("Outlook: Profitable within 5 years");
    });
});
