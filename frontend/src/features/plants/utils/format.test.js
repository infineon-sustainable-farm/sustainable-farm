import { describe, expect, it } from "vitest";
import { formatDate, formatMonth, formatMonthName, formatPhaseBand, formatTimestamp, monthName } from "./format";

describe("a calendar date", () => {
    it("reads day, short month in letters and year, with no leading zero", () => {
        expect(formatDate("2026-11-06")).toBe("6 Nov 2026");
        expect(formatDate("2027-05-01")).toBe("1 May 2027");
        expect(formatDate(" 2026-12-31 ")).toBe("31 Dec 2026");
    });

    it("writes September Sep, whatever the browser language", () => {
        expect(formatDate("2026-09-15")).toBe("15 Sep 2026");
    });

    it("is a dash when missing or malformed", () => {
        expect(formatDate(null)).toBe("—");
        expect(formatDate("")).toBe("—");
        expect(formatDate("06/11/2026")).toBe("—");
        expect(formatDate("2026-13-01")).toBe("—");
        expect(formatDate("2026-11-00")).toBe("—");
    });
});

describe("an instant", () => {
    it("reads as the date and the local time, minutes padded", () => {
        expect(formatTimestamp(new Date(2026, 10, 6, 14, 5).toISOString())).toBe("6 Nov 2026, 14:05");
        expect(formatTimestamp(new Date(2027, 0, 9, 8, 0).toISOString())).toBe("9 Jan 2027, 08:00");
    });

    it("is a dash when missing or not a date", () => {
        expect(formatTimestamp(null)).toBe("—");
        expect(formatTimestamp("not a date")).toBe("—");
    });
});

describe("a month", () => {
    it("reads short or long, with its year", () => {
        expect(formatMonth("2027-04")).toBe("Apr 2027");
        expect(formatMonth("2027-04", "long")).toBe("April 2027");
        expect(formatMonthName("2026-09")).toBe("Sep");
        expect(monthName(9)).toBe("September");
    });

    it("is a dash, or null for a month number, when malformed", () => {
        expect(formatMonth("2027-13")).toBe("—");
        expect(formatMonthName("April")).toBe("—");
        expect(monthName(0)).toBe(null);
    });
});

describe("the growth phase of a calendar row", () => {
    it("is named as the API names it, with its year band", () => {
        expect(formatPhaseBand("full production", "6+ yrs")).toBe("Full production · 6+ yrs");
        expect(formatPhaseBand("establishment", "0–2 yrs")).toBe("Establishment · 0–2 yrs");
    });

    it("shows the part the API sent when the other one is missing", () => {
        expect(formatPhaseBand("gradual production", null)).toBe("Gradual production");
        expect(formatPhaseBand(null, "3–5 yrs")).toBe("3–5 yrs");
        expect(formatPhaseBand(" full production ", " 6+ yrs ")).toBe("Full production · 6+ yrs");
    });

    it("is a dash without planting date, when the API sends neither", () => {
        expect(formatPhaseBand(null, null)).toBe("—");
        expect(formatPhaseBand(undefined, "")).toBe("—");
    });
});
