import { afterEach, describe, expect, it } from "vitest";
import i18n from "../../../shared/i18n/i18n";
import { formatReturn, formatSignedTrees, formatYears, SCENARIO_MESSAGES, validateScenarioForm } from "./scenarioRequests";

afterEach(async () => {
    await i18n.changeLanguage("en");
});

describe("the scenario figures and messages in another language", () => {
    it("follow the active language at call time", async () => {
        await i18n.changeLanguage("de");
        expect(formatYears(5.2)).toBe("5,2 Jahre");
        expect(formatSignedTrees(1)).toBe("+1 Baum");
        expect(SCENARIO_MESSAGES.nameRequired).toBe("Der Name ist erforderlich.");
        expect(validateScenarioForm({ name: "" , varietyName: "", currency: "", treeCount: "", yieldPerTreeKg: "",
            lossRate: "", pricePerKg: "", annualFixedCost: "", variableCostPerKg: "", initialInvestment: "",
            costPerAdditionalTree: "", treeCountSource: "", yieldSource: "", lossRateSource: "", priceSource: "",
            annualFixedCostSource: "", variableCostSource: "", investmentSource: "", costPerAdditionalTreeSource: "" }).name)
            .toBe("Der Name ist erforderlich.");
        await i18n.changeLanguage("fr");
        expect(formatReturn(19.4)).toBe("19,4 %");
    });
});
