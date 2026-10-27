import { describe, expect, it } from "vitest";
import {
    analysisParams,
    benefitLineDetails,
    benefitLineLabel,
    EMPTY_SCENARIO_FORM,
    formatAmount,
    formatMass,
    formatOtherAmount,
    formatReturn,
    formatSignedAmount,
    formatSignedKilograms,
    formatSignedPoints,
    formatUnitPrice,
    formatYears,
    parseAdditionalInvestment,
    parseLossRate,
    parseTreeCountChange,
    parseYieldChangePct,
    readHorizonYears,
    readScenarioId,
    readScenarioView,
    readYieldIncrease,
    sameVarietyName,
    SCENARIO_MESSAGES,
    scenarioFormValues,
    scenarioRequestBody,
    scenarioViewSearch,
    validateScenarioForm,
} from "./scenarioRequests";

// A form filled with every required value and no optional one.
const REQUIRED_FORM = {
    ...EMPTY_SCENARIO_FORM,
    name: "Plan A",
    currency: "EUR",
    treeCount: "200",
    yieldPerTreeKg: "220",
    annualFixedCost: "6707",
    initialInvestment: "171512",
};

describe("scenarioRequestBody", () => {
    it("sends the required values as numbers, and no optional value, variety or source left empty", () => {
        expect(scenarioRequestBody({ ...REQUIRED_FORM, varietyName: "  ", lossRateSource: "packhouse" })).toEqual({
            name: "Plan A",
            currency: "EUR",
            treeCount: 200,
            yieldPerTreeKg: 220,
            annualFixedCost: 6707,
            initialInvestment: 171512,
        });
    });

    it("sends an optional value with its source, as typed", () => {
        const body = scenarioRequestBody({
            ...REQUIRED_FORM,
            varietyName: "Keitt",
            lossRate: " 0.15 ",
            lossRateSource: "packhouse_2026",
            pricePerKg: "0.9072",
            costPerAdditionalTree: "0",
            treeCountSource: " farm plan ",
        });

        expect(body.varietyName).toBe("Keitt");
        expect(body.lossRate).toBe(0.15);
        expect(body.lossRateSource).toBe("packhouse_2026");
        expect(body.pricePerKg).toBe(0.9072);
        expect(body).not.toHaveProperty("priceSource");
        expect(body.costPerAdditionalTree).toBe(0);
        expect(body.treeCountSource).toBe(" farm plan ");
    });
});

describe("scenarioFormValues", () => {
    it("starts from the recorded values, with every source empty", () => {
        const values = scenarioFormValues({
            name: "Zalka 2025",
            varietyName: "Keitt",
            treeCount: 200,
            treeCountSource: "Zalka_2025_C43",
            yieldPerTreeKg: 220,
            lossRate: null,
            pricePerKg: 0.9072,
            priceSource: "derived: Zalka_2025 R19 / D4",
            currency: "EUR",
            annualFixedCost: 6707,
            variableCostPerKg: null,
            initialInvestment: 171512,
            costPerAdditionalTree: null,
        });

        expect(values.treeCount).toBe("200");
        expect(values.pricePerKg).toBe("0.9072");
        expect(values.lossRate).toBe("");
        expect(values.treeCountSource).toBe("");
        expect(values.priceSource).toBe("");
    });

    it("is empty for a new scenario", () => {
        expect(scenarioFormValues(null)).toEqual(EMPTY_SCENARIO_FORM);
    });
});

describe("validateScenarioForm", () => {
    it("accepts the required values alone", () => {
        expect(validateScenarioForm(REQUIRED_FORM)).toEqual({});
    });

    it("asks for every required value", () => {
        expect(Object.keys(validateScenarioForm(EMPTY_SCENARIO_FORM)).sort()).toEqual([
            "annualFixedCost",
            "currency",
            "initialInvestment",
            "name",
            "treeCount",
            "yieldPerTreeKg",
        ]);
    });

    it("refuses a yield per tree of 0, above 1,000 kg or with 2 decimals, as the API does", () => {
        for (const yieldPerTreeKg of ["0", "1001", "220.55", "2,5"]) {
            expect(validateScenarioForm({ ...REQUIRED_FORM, yieldPerTreeKg }).yieldPerTreeKg)
                .toBe(SCENARIO_MESSAGES.yieldPerTreeKg);
        }
        expect(validateScenarioForm({ ...REQUIRED_FORM, yieldPerTreeKg: "1000" })).toEqual({});
    });

    it("refuses a loss rate above 1 or with 4 decimals, and accepts 0 and 1", () => {
        expect(validateScenarioForm({ ...REQUIRED_FORM, lossRate: "1.5" }).lossRate).toBe(SCENARIO_MESSAGES.lossRate);
        expect(validateScenarioForm({ ...REQUIRED_FORM, lossRate: "0.1234" }).lossRate).toBe(SCENARIO_MESSAGES.lossRate);
        expect(validateScenarioForm({ ...REQUIRED_FORM, lossRate: "0" })).toEqual({});
        expect(validateScenarioForm({ ...REQUIRED_FORM, lossRate: "1" })).toEqual({});
    });

    it("refuses a price of 0, an investment of 0 and amounts with too many decimals", () => {
        const errors = validateScenarioForm({
            ...REQUIRED_FORM,
            pricePerKg: "0",
            initialInvestment: "0",
            annualFixedCost: "6707.001",
            variableCostPerKg: "0.00001",
            costPerAdditionalTree: "-5",
        });

        expect(errors).toEqual({
            pricePerKg: SCENARIO_MESSAGES.pricePerKg,
            initialInvestment: SCENARIO_MESSAGES.initialInvestment,
            annualFixedCost: SCENARIO_MESSAGES.annualFixedCost,
            variableCostPerKg: SCENARIO_MESSAGES.variableCostPerKg,
            costPerAdditionalTree: SCENARIO_MESSAGES.costPerAdditionalTree,
        });
    });

    it("refuses 0 trees and more than 1,000,000, and a source of more than 255 characters", () => {
        expect(validateScenarioForm({ ...REQUIRED_FORM, treeCount: "0" }).treeCount).toBe(SCENARIO_MESSAGES.treeCount);
        expect(validateScenarioForm({ ...REQUIRED_FORM, treeCount: "1000001" }).treeCount)
            .toBe(SCENARIO_MESSAGES.treeCount);
        expect(validateScenarioForm({ ...REQUIRED_FORM, yieldSource: "s".repeat(256) }).yieldSource)
            .toBe(SCENARIO_MESSAGES.source);
    });
});

describe("the inputs of a variant", () => {
    it("read a number of trees, negative to remove some", () => {
        expect(parseTreeCountChange(" -20 ")).toEqual({ value: -20, error: null });
        expect(parseTreeCountChange("+20")).toEqual({ value: 20, error: null });
        expect(parseTreeCountChange("")).toEqual({ value: null, error: null });
        expect(parseTreeCountChange("2.5").error).not.toBeNull();
    });

    it("read a change of yield above -100 % and at most 1,000 %, with 1 decimal", () => {
        expect(parseYieldChangePct("12.5")).toEqual({ value: 12.5, error: null });
        expect(parseYieldChangePct("-100").error).not.toBeNull();
        expect(parseYieldChangePct("10.25").error).not.toBeNull();
    });

    it("read a loss rate and an additional investment with the API's bounds", () => {
        expect(parseLossRate("0.05")).toEqual({ value: 0.05, error: null });
        expect(parseLossRate("1.2").error).not.toBeNull();
        expect(parseAdditionalInvestment("5000")).toEqual({ value: 5000, error: null });
        expect(parseAdditionalInvestment("-1").error).not.toBeNull();
        expect(parseAdditionalInvestment("0.001").error).not.toBeNull();
    });

    it("send only the parameters given", () => {
        expect(analysisParams({ yieldPerTreeChangePct: 10, additionalInvestment: null, horizonYears: undefined }))
            .toEqual({ yieldPerTreeChangePct: 10 });
        expect(analysisParams({ treeCountChange: 0 })).toEqual({ treeCountChange: 0 });
    });
});

describe("the address of the screen", () => {
    it("keeps the slider from 0 to 50 and the horizon from 1 to 30, with their defaults otherwise", () => {
        expect(readYieldIncrease("0")).toBe(0);
        expect(readYieldIncrease("51")).toBe(10);
        expect(readYieldIncrease(null)).toBe(10);
        expect(readHorizonYears("30")).toBe(30);
        expect(readHorizonYears("0")).toBe(10);
        expect(readHorizonYears("abc")).toBe(10);
    });

    it("reads a scenario identifier, or none", () => {
        expect(readScenarioId("12")).toBe(12);
        expect(readScenarioId("0")).toBeNull();
        expect(readScenarioId("x")).toBeNull();
        expect(readScenarioId(null)).toBeNull();
    });

    it("keeps every part of a view at once, and only what differs from the defaults", () => {
        const view = readScenarioView(new URLSearchParams("scenario=2&horizon=12&trees=20&yieldPct=5&loss=0.05"));
        expect(view).toEqual({
            scenarioId: 2,
            horizonYears: 12,
            yieldIncrease: 10,
            investment: "",
            trees: "20",
            yieldPct: "5",
            loss: "0.05",
        });
        expect(scenarioViewSearch({ ...view, yieldIncrease: 23 }))
            .toBe("scenario=2&horizon=12&yieldChange=23&trees=20&yieldPct=5&loss=0.05");
        expect(scenarioViewSearch(readScenarioView(new URLSearchParams("")))).toBe("");
    });
});

describe("the figures of an analysis", () => {
    const revenue = { xof: 26183704, eur: 39916.8 };

    it("are written in the currency of the scenario, and in the other one when the API converted them", () => {
        expect(formatAmount(revenue, "EUR")).toBe("€39,916.80");
        expect(formatOtherAmount(revenue, "EUR")).toBe("26,183,704 FCFA");
        expect(formatAmount(revenue, "XOF")).toBe("26,183,704 FCFA");
        expect(formatOtherAmount({ xof: 3600000, eur: null }, "XOF")).toBeNull();
        expect(formatAmount(null, "EUR")).toBe("—");
    });

    it("take a minus sign when negative, and a plus sign as well for a change", () => {
        expect(formatAmount({ xof: -4399504, eur: -6707 }, "EUR")).toBe("−€6,707.00");
        expect(formatSignedAmount({ xof: 2618370, eur: 3991.68 }, "EUR")).toBe("+€3,991.68");
        expect(formatSignedAmount({ xof: 0, eur: 0 }, "EUR")).toBe("€0.00");
        expect(formatSignedKilograms(4400)).toBe("+4,400 kg");
        expect(formatSignedKilograms(-200)).toBe("−200 kg");
        expect(formatMass(0)).toBe("0 kg");
        expect(formatSignedPoints(2.3)).toBe("+2.3 pts");
        expect(formatSignedPoints(-0.4)).toBe("−0.4 pts");
        expect(formatReturn(-2)).toBe("−2%");
    });

    it("write a price with its 4 decimals, and durations in years", () => {
        expect(formatUnitPrice(0.9072, "EUR")).toBe("€0.9072");
        expect(formatUnitPrice(0.5, "EUR")).toBe("€0.50");
        expect(formatUnitPrice(300, "XOF")).toBe("300 FCFA");
        expect(formatYears(5.2)).toBe("5.2 years");
        expect(formatYears(1)).toBe("1 year");
    });

    it("name each cause of a benefit by the direction of the quantity it adds", () => {
        expect(benefitLineLabel({ type: "TREE_COUNT", quantityKg: 3960 })).toBe("Additional trees");
        expect(benefitLineLabel({ type: "TREE_COUNT", quantityKg: -2200 })).toBe("Fewer trees");
        expect(benefitLineLabel({ type: "LOSS_RATE", quantityKg: 2420 })).toBe("Loss reduction");
        expect(benefitLineLabel({ type: "VARIABLE_OPERATING_COST", quantityKg: 5820 })).toBe("Additional operating cost");
        expect(benefitLineDetails({ type: "YIELD_PER_TREE", quantityKg: 4400, unitAmount: 0.9072 }, "EUR"))
            .toBe("+4,400 kg × €0.9072/kg");
        expect(benefitLineDetails({ type: "VARIABLE_OPERATING_COST", quantityKg: 5820, unitAmount: 25 }, "XOF"))
            .toBe("+5,820 kg × 25 FCFA/kg variable cost");
    });

    it("compare variety names ignoring case, accents and spaces", () => {
        expect(sameVarietyName(" amélie ", "Amelie")).toBe(true);
        expect(sameVarietyName("Kent", "Keitt")).toBe(false);
        expect(sameVarietyName("", "")).toBe(false);
    });
});
