import { describe, expect, it } from "vitest";
import { rainfallState, rainfallWindow } from "./rainfall";

describe("rainfallWindow", () => {
    it("covers the last 30 days, today included, in UTC", () => {
        expect(rainfallWindow(new Date("2026-11-14T10:00:00Z"))).toEqual({ from: "2026-10-16", to: "2026-11-14" });
    });

    it("crosses a year and takes the UTC day near midnight", () => {
        expect(rainfallWindow(new Date("2027-01-10T23:59:59Z"))).toEqual({ from: "2026-12-12", to: "2027-01-10" });
        expect(rainfallWindow(new Date("2026-03-01T00:00:00Z"))).toEqual({ from: "2026-01-31", to: "2026-03-01" });
    });
});

describe("rainfallState", () => {
    const answered = (data) => ({ isPending: false, isError: false, data });
    const failed = (status) => ({ isPending: false, isError: true, error: { status } });

    it("waits while the answer is pending", () => {
        expect(rainfallState({ isPending: true, isError: false })).toEqual({ kind: "loading" });
    });

    it("gives the rain measured by the gauges, 0 mm included", () => {
        expect(rainfallState(answered({ rainfallMm: 12.5, gaugeCount: 2 }))).toEqual({ kind: "value", rainfallMm: 12.5 });
        expect(rainfallState(answered({ rainfallMm: 0, gaugeCount: 1 }))).toEqual({ kind: "value", rainfallMm: 0 });
    });

    it("reads no data when the block is linked to no gauge (404)", () => {
        expect(rainfallState(failed(404))).toEqual({ kind: "none" });
    });

    it("reads no data when the answer is empty, has no rainfall or counts no gauge", () => {
        expect(rainfallState(answered(null))).toEqual({ kind: "none" });
        expect(rainfallState(answered(""))).toEqual({ kind: "none" });
        expect(rainfallState(answered({}))).toEqual({ kind: "none" });
        expect(rainfallState(answered({ rainfallMm: null, gaugeCount: 1 }))).toEqual({ kind: "none" });
        expect(rainfallState(answered({ rainfallMm: 0, gaugeCount: 0 }))).toEqual({ kind: "none" });
    });

    it("says the rain is unavailable for any other failure", () => {
        expect(rainfallState(failed(500))).toEqual({ kind: "unavailable" });
        expect(rainfallState(failed(undefined))).toEqual({ kind: "unavailable" });
    });

    it("reads no data for a row without a query", () => {
        expect(rainfallState(undefined)).toEqual({ kind: "none" });
    });
});
