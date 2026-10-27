import { describe, expect, it } from "vitest";
import {
    formatObservationMethod,
    MEASURED_VALUE_ERROR,
    measuredValueError,
    METHOD_OPTIONS,
    methodTakesValue,
    methodUnit,
    observationRequest,
    STATUS_OPTIONS,
} from "./maturityObservations";

const VALUE_ERROR = "Measured value must be above 0 and at most 100, with one decimal at most.";

/** A filled form, as the observation dialog holds it. */
function form(overrides = {}) {
    return {
        observedOn: "2026-10-25",
        blockCode: "A",
        varietyName: "Keitt",
        maturityStatus: "READY_FOR_HARVEST",
        method: "BRIX",
        measuredValue: "14.2",
        observer: "Awa",
        ...overrides,
    };
}

describe("STATUS_OPTIONS", () => {
    it("lists the three statuses of the requirements, in order, without Harvested", () => {
        expect(STATUS_OPTIONS).toEqual([
            { code: "IMMATURE", label: "Immature" },
            { code: "RIPE", label: "Ripe" },
            { code: "READY_FOR_HARVEST", label: "Ready for harvest" },
        ]);
    });
});

describe("METHOD_OPTIONS", () => {
    it("lists the four methods in the order of the form, with their labels", () => {
        expect(METHOD_OPTIONS.map((option) => [option.code, option.label])).toEqual([
            ["FLESH_COLOR", "Flesh color near the stone"],
            ["BRIX", "Brix (°Bx)"],
            ["DRY_MATTER", "Dry matter (%)"],
            ["VISUAL", "Visual (shoulders, skin)"],
        ]);
    });

    it("gives a unit to Brix and dry matter only", () => {
        expect(methodUnit("BRIX")).toBe("°Bx");
        expect(methodUnit("DRY_MATTER")).toBe("%");
        expect(methodUnit("FLESH_COLOR")).toBeNull();
        expect(methodUnit("VISUAL")).toBeNull();
        expect(methodUnit("")).toBeNull();
        expect(methodUnit("FIRMNESS")).toBeNull();
        expect(METHOD_OPTIONS.filter((option) => methodTakesValue(option.code)).map((option) => option.code))
            .toEqual(["BRIX", "DRY_MATTER"]);
    });
});

describe("measuredValueError", () => {
    it("accepts no value, and a value above 0 and up to 100 with one decimal at most", () => {
        for (const text of ["", "   ", "0.1", "12", "12.3", " 14.2 ", "100", "100.0", "15.", ".5"]) {
            expect(measuredValueError(text), text).toBeNull();
        }
    });

    it("refuses 0, a negative value, more than 100, two decimals and anything not a plain number", () => {
        for (const text of ["0", "-1", "100.1", "150", "12.34", "0.05", "1e2", "abc", "."]) {
            expect(measuredValueError(text), text).toBe(VALUE_ERROR);
        }
        expect(MEASURED_VALUE_ERROR).toBe(VALUE_ERROR);
    });
});

describe("observationRequest", () => {
    it("sends the value as a number with Brix, and the observer trimmed", () => {
        expect(observationRequest(form({ observer: "  Awa  " }))).toEqual({
            observedOn: "2026-10-25",
            blockCode: "A",
            varietyName: "Keitt",
            maturityStatus: "READY_FOR_HARVEST",
            method: "BRIX",
            measuredValue: 14.2,
            observer: "Awa",
        });
    });

    it("leaves out an empty value, a value with a method that measures none, and a blank observer", () => {
        expect(observationRequest(form({ measuredValue: "", observer: "" }))).toEqual({
            observedOn: "2026-10-25",
            blockCode: "A",
            varietyName: "Keitt",
            maturityStatus: "READY_FOR_HARVEST",
            method: "BRIX",
        });
        const visual = observationRequest(form({ method: "VISUAL", measuredValue: "14.2", observer: "   " }));
        expect(visual).not.toHaveProperty("measuredValue");
        expect(visual).not.toHaveProperty("observer");
    });
});

describe("formatObservationMethod", () => {
    it("names the method, with the value read and its unit", () => {
        expect(formatObservationMethod("BRIX", 14.2, "°Bx")).toBe("Brix 14.2 °Bx");
        expect(formatObservationMethod("DRY_MATTER", 15, "%")).toBe("Dry matter 15%");
        expect(formatObservationMethod("FLESH_COLOR", null, null)).toBe("Flesh color");
        expect(formatObservationMethod("VISUAL", undefined, undefined)).toBe("Visual");
    });

    it("still shows an unknown method, in sentence case", () => {
        expect(formatObservationMethod("NEAR_INFRARED", null, null)).toBe("Near infrared");
    });
});
