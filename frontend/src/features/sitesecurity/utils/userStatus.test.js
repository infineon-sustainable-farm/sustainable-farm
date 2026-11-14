import { describe, expect, it } from "vitest";
import {
    actorLabel,
    canConfirmDelivery,
    codeSignInConditions,
    emailDeliveryLabel,
    emailSubmissionLabel,
    formatDate,
    formatInstant,
    hasEmailInFlight,
    historyActionLabel,
    summaryCards,
    userActions,
    userStatusLabel,
} from "./userStatus";

describe("userStatusLabel", () => {
    it.each([
        ["pending", "Pending"],
        ["activation_started", "Activation started (link opened)"],
        ["active", "Active"],
        ["expired", "Expired"],
        ["revoked", "Revoked"],
        ["disabled", "Disabled"],
        ["weird", "Unknown"],
    ])("labels %s", (status, label) => {
        expect(userStatusLabel(status)).toBe(label);
    });
});

describe("userActions", () => {
    it.each([
        ["pending", ["resend", "copyLink", "revoke", "edit", "disable", "history"]],
        ["activation_started", ["copyLink", "revoke", "edit", "disable", "history"]],
        ["expired", ["resend", "copyLink", "revoke", "edit", "disable", "history"]],
        ["active", ["edit", "disable", "history"]],
        ["disabled", ["edit", "enable", "history"]],
        ["revoked", ["history"]],
    ])("offers the right actions for %s", (status, actions) => {
        expect(userActions({ status, email: "a@b.c", emailDelivery: null })).toEqual(actions);
    });

    it("offers the password setup link to an active legacy account", () => {
        expect(userActions({ status: "active", legacyCodeSignIn: true })).toContain("setupLink");
        expect(userActions({ status: "pending", legacyCodeSignIn: true })).not.toContain("setupLink");
    });

    it("offers delivery confirmation only for a submitted email not yet confirmed", () => {
        expect(userActions({ status: "active", emailDelivery: { deliveryId: "d1", submission: "submitted", delivery: "unknown" } })).toContain("confirmDelivery");
        expect(userActions({ status: "active", emailDelivery: { deliveryId: "d1", submission: "submitted", delivery: "confirmed_by_admin" } })).not.toContain("confirmDelivery");
        for (const submission of ["queued", "sending", "retry_scheduled", "failed", "local_console", "mail_disabled"]) {
            expect(userActions({ status: "active", emailDelivery: { deliveryId: "d1", submission, delivery: "unknown" } })).not.toContain("confirmDelivery");
        }
    });
});

describe("email labels", () => {
    it("never calls a submission a delivery", () => {
        expect(emailSubmissionLabel("submitted")).toBe("Submitted to provider");
        for (const s of ["queued", "sending", "retry_scheduled", "submitted", "failed", "local_console", "mail_disabled"]) {
            expect(emailSubmissionLabel(s)).not.toMatch(/deliver/i);
        }
    });

    it("labels the delivery state", () => {
        expect(emailDeliveryLabel("unknown")).toBe("Delivery not confirmed");
        expect(emailDeliveryLabel(undefined)).toBe("Delivery not confirmed");
        expect(emailDeliveryLabel("confirmed_by_admin")).toBe("Delivery confirmed by admin");
    });

    it("needs a delivery id to confirm", () => {
        expect(canConfirmDelivery(null)).toBe(false);
        expect(canConfirmDelivery({ delivery: "unknown" })).toBe(false);
    });

    it("detects emails still on their way", () => {
        expect(hasEmailInFlight([{ emailDelivery: { submission: "queued" } }])).toBe(true);
        expect(hasEmailInFlight([{ emailDelivery: { submission: "submitted" } }, { emailDelivery: null }])).toBe(false);
    });
});

describe("history", () => {
    it("labels actors", () => {
        expect(actorLabel({ kind: "admin", email: "a@b.c" })).toBe("Admin a@b.c");
        expect(actorLabel({ kind: "admin", email: null })).toBe("An admin");
        expect(actorLabel({ kind: "system" })).toBe("System");
        expect(actorLabel(null)).toBe("Unknown");
    });

    it("humanizes actions", () => {
        expect(historyActionLabel("INVITATION_RESENT")).toBe("Invitation resent");
        expect(historyActionLabel(null)).toBe("Event");
        expect(historyActionLabel("email_delivery_confirmed")).toBe("Delivery attested by admin (manual)");
    });
});

describe("dates", () => {
    it("shows a dash for nothing or garbage", () => {
        expect(formatInstant(null)).toBe("—");
        expect(formatInstant("nope")).toBe("—");
        expect(formatDate("")).toBe("—");
    });

    it("formats a date", () => {
        expect(formatDate("2026-11-14")).toContain("2026");
        expect(formatInstant("2026-11-14T10:00:00Z")).toContain("2026");
    });
});

describe("codeSignInConditions", () => {
    it("checks the three conditions", () => {
        const checks = codeSignInConditions({
            conditions: { realDeliveryVerified: true, adminsWithPasswordSignIn: 1, requiredAdminsWithPasswordSignIn: 2, allActiveAdminsHavePassword: false },
        });
        expect(checks.map((c) => c.ok)).toEqual([true, false, false]);
        expect(checks[1].label).toContain("now 1");
    });

    it("tolerates missing settings", () => {
        expect(codeSignInConditions(null).every((c) => !c.ok)).toBe(true);
    });
});

describe("summaryCards", () => {
    it("counts started activations as pending and groups revoked with disabled", () => {
        const cards = summaryCards({ pending: 2, activationStarted: 1, active: 5, expired: 1, revoked: 1, disabled: 2 });
        expect(cards.map((c) => [c.id, c.value])).toEqual([["pending", 3], ["active", 5], ["expired", 1], ["closed", 3]]);
    });

    it("shows zeros without a summary", () => {
        expect(summaryCards(null).every((c) => c.value === 0)).toBe(true);
    });
});
