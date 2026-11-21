import { describe, expect, it } from "vitest";
import {
    completionRequestBody,
    formatSignedPercent,
    formatSignedQuantity,
    planBadge,
    planDelayLabel,
    planRequestBody,
    stockWarningText,
    validateCompletion,
    validatePlan,
} from "./fertilizerPlans";

const TODAY = "2026-11-14";

const PLAN_FORM = {
    fertilizerId: "3",
    blockCode: " b2 ",
    plannedDate: "2026-11-24",
    plannedQuantity: "30.5",
    note: "before the rains",
};

const COMPLETION_FORM = {
    applicationDate: "2026-11-14",
    quantity: "25.5",
    applicatorPersonId: "7",
    method: "",
};

describe("validatePlan", () => {
    it("accepts a filled form, with a planned date of today", () => {
        expect(validatePlan(PLAN_FORM, { today: TODAY })).toEqual({});
        expect(validatePlan({ ...PLAN_FORM, plannedDate: TODAY, note: "" }, { today: TODAY })).toEqual({});
    });

    it("names every missing field of an empty form", () => {
        const errors = validatePlan(
            { fertilizerId: "", blockCode: "  ", plannedDate: "", plannedQuantity: "", note: "" },
            { today: TODAY },
        );

        expect(errors).toEqual({
            fertilizerId: "Fertilizer is required.",
            blockCode: "Block is required.",
            plannedDate: "Planned date is required.",
            plannedQuantity: "Quantity must be a number greater than 0, with at most 3 decimals.",
        });
    });

    it("refuses a date before the day of the API, and leaves the date to the API while that day is unknown", () => {
        const past = { ...PLAN_FORM, plannedDate: "2026-11-13" };

        expect(validatePlan(past, { today: TODAY }).plannedDate).toBe("Planned date cannot be in the past.");
        expect(validatePlan(past)).toEqual({});
    });

    it("refuses a quantity of 0, with 4 decimals, or that the field could not read", () => {
        const message = "Quantity must be a number greater than 0, with at most 3 decimals.";

        expect(validatePlan({ ...PLAN_FORM, plannedQuantity: "0" }).plannedQuantity).toBe(message);
        expect(validatePlan({ ...PLAN_FORM, plannedQuantity: "1.2345" }).plannedQuantity).toBe(message);
        expect(validatePlan({ ...PLAN_FORM, plannedQuantity: "-3" }).plannedQuantity).toBe(message);
        expect(validatePlan(PLAN_FORM, { unreadableQuantity: true }).plannedQuantity).toBe(message);
        expect(validatePlan({ ...PLAN_FORM, plannedQuantity: "0.001" })).toEqual({});
    });

    it("refuses a note over 255 characters; the block is picked from the farm referential", () => {
        const errors = validatePlan({ ...PLAN_FORM, note: "n".repeat(256) });

        expect(errors).toEqual({
            note: "Note must be at most 255 characters.",
        });
    });
});

describe("planRequestBody", () => {
    it("sends the values as typed, with numbers for the fertilizer and the quantity", () => {
        expect(planRequestBody(PLAN_FORM)).toEqual({
            fertilizerId: 3,
            blockCode: " b2 ",
            plannedDate: "2026-11-24",
            plannedQuantity: 30.5,
            note: "before the rains",
        });
    });

    it("sends no note when it is empty, and no farm at all", () => {
        const body = planRequestBody({ ...PLAN_FORM, note: "   " });

        expect(body).not.toHaveProperty("note");
        expect(body).not.toHaveProperty("farmId");
    });
});

describe("validateCompletion", () => {
    it("accepts a filled form, with today as the date applied", () => {
        expect(validateCompletion(COMPLETION_FORM, { today: TODAY })).toEqual({});
    });

    it("names every missing field of an empty form", () => {
        const errors = validateCompletion(
            { applicationDate: "", quantity: "", applicatorPersonId: "", method: "" },
            { today: TODAY },
        );

        expect(errors).toEqual({
            applicationDate: "Date is required.",
            quantity: "Quantity must be a number greater than 0, with at most 3 decimals.",
            applicatorPersonId: "Applicator is required.",
        });
    });

    it("refuses a date after the day of the API, and a long method", () => {
        const errors = validateCompletion(
            { ...COMPLETION_FORM, applicationDate: "2026-11-15", method: "m".repeat(256) },
            { today: TODAY },
        );

        expect(errors).toEqual({
            applicationDate: "Date cannot be in the future.",
            method: "Method must be at most 255 characters.",
        });
    });
});

describe("completionRequestBody", () => {
    it("sends the method only when one was entered", () => {
        expect(completionRequestBody(COMPLETION_FORM)).toEqual({
            applicationDate: "2026-11-14",
            quantity: 25.5,
            applicatorPersonId: 7,
        });
        expect(completionRequestBody({ ...COMPLETION_FORM, method: "around the tree base" }).method)
            .toBe("around the tree base");
    });
});

describe("planBadge", () => {
    it("names the state, and Overdue only for a planned application the API flags as late", () => {
        expect(planBadge({ status: "PLANNED", overdue: false })).toEqual({ tone: "warning", label: "Planned" });
        expect(planBadge({ status: "PLANNED", overdue: true })).toEqual({ tone: "error", label: "Overdue" });
        expect(planBadge({ status: "DONE", overdue: false })).toEqual({ tone: "success", label: "Done" });
        expect(planBadge({ status: "CANCELLED", overdue: false })).toEqual({ tone: "neutral", label: "Cancelled" });
    });

    it("shows an unknown state as received", () => {
        expect(planBadge({ status: "POSTPONED" })).toEqual({ tone: "neutral", label: "POSTPONED" });
    });
});

describe("planDelayLabel", () => {
    it("words the days counted by the API", () => {
        expect(planDelayLabel({ status: "PLANNED", daysFromToday: 10 })).toBe("in 10 days");
        expect(planDelayLabel({ status: "PLANNED", daysFromToday: 1 })).toBe("in 1 day");
        expect(planDelayLabel({ status: "PLANNED", daysFromToday: 0 })).toBe("today");
        expect(planDelayLabel({ status: "PLANNED", daysFromToday: -1 })).toBe("1 day late");
        expect(planDelayLabel({ status: "PLANNED", daysFromToday: -12 })).toBe("12 days late");
    });

    it("says nothing once the plan is done or cancelled", () => {
        expect(planDelayLabel({ status: "DONE", daysFromToday: null })).toBeNull();
        expect(planDelayLabel({ status: "CANCELLED", daysFromToday: null })).toBeNull();
    });
});

describe("formatSignedQuantity and formatSignedPercent", () => {
    it("keeps the sign and the unit of the fertilizer", () => {
        expect(formatSignedQuantity(5, "KG")).toBe("+5 kg");
        expect(formatSignedQuantity(-20, "KG")).toBe("-20 kg");
        expect(formatSignedQuantity(0, "L")).toBe("0 L");
        expect(formatSignedQuantity(-1250.5, "L")).toBe("-1,250.5 L");
        expect(formatSignedQuantity(null, "KG")).toBe("—");
    });

    it("keeps the sign of the percentage, and gives null when nothing was planned", () => {
        expect(formatSignedPercent(-33.3)).toBe("-33.3%");
        expect(formatSignedPercent(12)).toBe("+12%");
        expect(formatSignedPercent(0)).toBe("0%");
        expect(formatSignedPercent(null)).toBeNull();
    });
});

describe("stockWarningText", () => {
    const WARNING = {
        fertilizerName: "NPK 15-15-15",
        unit: "KG",
        plannedQuantity: 70,
        overdueQuantity: 0,
        currentStock: 50,
        shortfall: 20,
    };

    it("gives the planned quantity, the stock and what is missing, in the unit of the fertilizer", () => {
        expect(stockWarningText(WARNING, 30))
            .toBe("NPK 15-15-15: 70 kg planned in the next 30 days, 50 kg in stock, 20 kg short.");
        expect(stockWarningText({ ...WARNING, fertilizerName: "Liquid feed", unit: "L" }, 30))
            .toBe("Liquid feed: 70 L planned in the next 30 days, 50 L in stock, 20 L short.");
    });

    it("adds the overdue part when there is one", () => {
        expect(stockWarningText({ ...WARNING, overdueQuantity: 30 }, 30))
            .toBe("NPK 15-15-15: 70 kg planned in the next 30 days, 50 kg in stock, 20 kg short. "
                + "30 kg of it is overdue.");
    });
});
