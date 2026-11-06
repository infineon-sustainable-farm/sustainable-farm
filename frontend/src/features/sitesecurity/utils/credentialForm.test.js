import { describe, expect, it } from "vitest";
import {
    allowedLevels,
    buildCredentialPayload,
    canRevoke,
    defaultValidUntil,
    isCredentialFormComplete,
    levelForType,
    toIsoDate,
} from "./credentialForm";

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

describe("toIsoDate", () => {
    it("formats a date the way the API expects it", () => {
        expect(toIsoDate(new Date(2026, 11, 31))).toBe("2026-12-31");
    });

    it("pads the month and the day", () => {
        expect(toIsoDate(new Date(2027, 0, 5))).toBe("2027-01-05");
    });

    it("keeps the local day late in the evening", () => {
        expect(toIsoDate(new Date(2026, 5, 15, 23, 59))).toBe("2026-06-15");
    });
});

describe("defaultValidUntil", () => {
    it("is 30 days ahead, across a year boundary", () => {
        expect(defaultValidUntil(new Date(2026, 11, 20))).toBe("2027-01-19");
    });

    it("does not change the date it receives", () => {
        const today = new Date(2026, 0, 1);
        defaultValidUntil(today);
        expect(toIsoDate(today)).toBe("2026-01-01");
    });
});

describe("visitor rules", () => {
    it("limits a visitor to the open and controlled zones", () => {
        expect(allowedLevels("Visitor")).toEqual(["open", "controlled"]);
    });

    it.each([["Staff"], ["Service"], ["Security"]])("offers every level to %s", (type) => {
        expect(allowedLevels(type)).toEqual(["open", "controlled", "restricted", "critical"]);
    });

    it("moves a visitor down from a level they cannot have", () => {
        expect(levelForType("Visitor", "critical")).toBe("controlled");
    });

    it("keeps a level that is allowed", () => {
        expect(levelForType("Visitor", "open")).toBe("open");
        expect(levelForType("Staff", "critical")).toBe("critical");
    });
});

describe("credential form", () => {
    const form = { name: "Ada", role: "Engineer", validUntil: "2027-01-01" };

    it("is complete with a name, a role and a date", () => {
        expect(isCredentialFormComplete(form)).toBe(true);
    });

    it.each([
        ["name", { ...form, name: "   " }],
        ["role", { ...form, role: "" }],
        ["date", { ...form, validUntil: "" }],
    ])("is incomplete without a %s", (_field, incomplete) => {
        expect(isCredentialFormComplete(incomplete)).toBe(false);
    });

    it("sends only what the API accepts, with an ISO date", () => {
        const payload = buildCredentialPayload({
            name: "  Ada Byron ",
            role: " Engineer ",
            type: "Staff",
            level: "open",
            validUntil: "2026-12-31",
            status: "Expired",
        });

        expect(payload).toEqual({
            name: "Ada Byron",
            role: "Engineer",
            type: "Staff",
            level: "open",
            validUntil: "2026-12-31",
        });
        expect(payload.validUntil).toMatch(ISO_DATE);
    });
});

describe("canRevoke", () => {
    it.each([["Active"], ["Authorized"]])("allows revoking an %s credential", (status) => {
        expect(canRevoke({ status })).toBe(true);
    });

    it.each([["Revoked"], ["Expired"]])("has nothing to revoke when %s", (status) => {
        expect(canRevoke({ status })).toBe(false);
    });
});
