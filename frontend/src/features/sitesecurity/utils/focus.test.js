import { describe, expect, it } from "vitest";
import { trappedFocusIndex } from "./focus";

describe("trappedFocusIndex", () => {
    it("wraps from the last element to the first with Tab", () => {
        expect(trappedFocusIndex(3, 4, false)).toBe(0);
    });

    it("wraps from the first element to the last with Shift+Tab", () => {
        expect(trappedFocusIndex(0, 4, true)).toBe(3);
    });

    it("lets the browser move focus between the two ends", () => {
        expect(trappedFocusIndex(1, 4, false)).toBeNull();
        expect(trappedFocusIndex(2, 4, true)).toBeNull();
    });

    it("brings focus back inside when it is elsewhere", () => {
        expect(trappedFocusIndex(-1, 4, false)).toBe(0);
        expect(trappedFocusIndex(-1, 4, true)).toBe(3);
    });

    it("stays on a single element", () => {
        expect(trappedFocusIndex(0, 1, false)).toBe(0);
        expect(trappedFocusIndex(0, 1, true)).toBe(0);
    });

    it("has nothing to do without a focusable element", () => {
        expect(trappedFocusIndex(-1, 0, false)).toBeNull();
    });
});
