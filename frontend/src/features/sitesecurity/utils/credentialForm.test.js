import { describe, expect, it } from "vitest";
import {
    allowedLevels,
    buildCredentialPayload,
    canRevoke,
    defaultValidUntil,
    emailStatusLabel,
    hasPendingEmail,
    isCredentialFormComplete,
    isValidEmail,
    levelForType,
    mergeCredentials,
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
    const form = { name: "Ada", email: "ada@example.com", role: "Engineer", validUntil: "2027-01-01" };

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
            email: " ada@example.com ",
            role: " Engineer ",
            type: "Staff",
            level: "open",
            validUntil: "2026-12-31",
            status: "Expired",
        });

        expect(payload).toEqual({
            name: "Ada Byron",
            email: "ada@example.com",
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

describe("credential email", () => {
    const complete = { name: "Ada", email: "ada@example.com", role: "Guide", validUntil: "2030-01-01" };

    it.each([["ada@example.com"], [" ada@example.com "]])("accepts %j", (email) => {
        expect(isValidEmail(email)).toBe(true);
    });

    it.each([[""], ["ada"], ["ada@"], ["@example.com"], ["a b@example.com"], ["a@b@c"]])("rejects %j", (email) => {
        expect(isValidEmail(email)).toBe(false);
    });

    it("requires a valid email to complete the form", () => {
        expect(isCredentialFormComplete(complete)).toBe(true);
        expect(isCredentialFormComplete({ ...complete, email: "" })).toBe(false);
        expect(isCredentialFormComplete({ ...complete, email: "nope" })).toBe(false);
    });

    it("sends the trimmed email", () => {
        const payload = buildCredentialPayload({ ...complete, email: " ada@example.com ", type: "Staff", level: "open" });
        expect(payload.email).toBe("ada@example.com");
    });

    it("detects an email still being sent", () => {
        expect(hasPendingEmail([{ notificationStatus: "sent" }, { notificationStatus: "pending" }])).toBe(true);
        expect(hasPendingEmail([{ notificationStatus: "failed" }, { notificationStatus: null }])).toBe(false);
    });

    it("updates rows without changing their order or dropping a new one", () => {
        const current = [{ id: "new", notificationStatus: "pending" }, { id: "old", notificationStatus: null }];
        const latest = [{ id: "old", notificationStatus: null }];
        expect(mergeCredentials(current, latest)).toEqual(current);

        const updated = mergeCredentials(current, [{ id: "new", notificationStatus: "failed" }]);
        expect(updated.map((user) => user.id)).toEqual(["new", "old"]);
        expect(updated[0].notificationStatus).toBe("failed");
    });

    it("names the email the status is about", () => {
        expect(emailStatusLabel({ status: "Active", notificationStatus: "failed" })).toBe("Access email not sent");
        expect(emailStatusLabel({ status: "Revoked", notificationStatus: "sent" })).toBe("Revocation email sent");
        expect(emailStatusLabel({ status: "Active", notificationStatus: "pending" })).toBe("Access email sending...");
    });

    it("shows nothing for a credential without email", () => {
        expect(emailStatusLabel({ status: "Active", notificationStatus: null })).toBeNull();
    });
});
