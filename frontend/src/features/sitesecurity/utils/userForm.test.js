import { describe, expect, it } from "vitest";
import {
    PERMISSIONS,
    buildEditPayload,
    buildInvitePayload,
    editFormErrors,
    editFormFromUser,
    emptyInviteForm,
    inviteFormErrors,
    isFormValid,
    toggleValue,
} from "./userForm";

const TODAY = "2026-11-14";

function filled(overrides = {}) {
    return {
        ...emptyInviteForm(),
        fullName: " Ada Lovelace ",
        email: "ada@example.test ",
        emailConfirmation: "ADA@example.test",
        ...overrides,
    };
}

describe("invite form", () => {
    it("starts with no permission and no zone", () => {
        const form = emptyInviteForm();
        expect(form.permissions).toEqual([]);
        expect(form.zoneIds).toEqual([]);
        expect(form.role).toBe("user");
    });

    it("never offers legacy_read_all", () => {
        expect(PERMISSIONS.map((p) => p.value)).not.toContain("legacy_read_all");
    });

    it("accepts a complete form", () => {
        expect(inviteFormErrors(filled(), TODAY)).toEqual({});
        expect(isFormValid(inviteFormErrors(filled(), TODAY))).toBe(true);
    });

    it("requires a name and a valid email", () => {
        const errors = inviteFormErrors(filled({ fullName: "  ", email: "nope", emailConfirmation: "nope" }), TODAY);
        expect(Object.keys(errors).sort()).toEqual(["email", "fullName"]);
    });

    it("requires the confirmation to match", () => {
        expect(inviteFormErrors(filled({ emailConfirmation: "eve@example.test" }), TODAY).emailConfirmation).toMatch(/do not match/);
        expect(inviteFormErrors(filled({ emailConfirmation: "" }), TODAY).emailConfirmation).toMatch(/again/);
    });

    it("refuses an unknown category or role", () => {
        const errors = inviteFormErrors(filled({ category: "boss", role: "root" }), TODAY);
        expect(errors.category).toBeTruthy();
        expect(errors.role).toBeTruthy();
    });

    it("accepts an empty, a future or today's date, not a past one", () => {
        expect(inviteFormErrors(filled({ validUntil: "" }), TODAY).validUntil).toBeUndefined();
        expect(inviteFormErrors(filled({ validUntil: TODAY }), TODAY).validUntil).toBeUndefined();
        expect(inviteFormErrors(filled({ validUntil: "2026-11-13" }), TODAY).validUntil).toMatch(/past/);
    });

    it("builds the API body", () => {
        const payload = buildInvitePayload(filled({ permissions: ["view_zones", "legacy_read_all"], zoneIds: ["open", "moon"] }));
        expect(payload).toEqual({
            fullName: "Ada Lovelace",
            email: "ada@example.test",
            emailConfirmation: "ADA@example.test",
            role: "user",
            category: "worker",
            permissions: ["view_zones"],
            zoneIds: ["open"],
            validUntil: null,
        });
    });

    it("sends no permission for an admin", () => {
        expect(buildInvitePayload(filled({ role: "admin", permissions: ["view_zones"] })).permissions).toEqual([]);
    });
});

describe("edit form", () => {
    const legacyUser = {
        fullName: "Old Op",
        role: "user",
        category: "security",
        permissions: ["legacy_read_all"],
        zoneIds: ["open"],
        validUntil: null,
    };

    it("keeps legacy access unless it is unticked", () => {
        const form = editFormFromUser(legacyUser);
        expect(form.permissions).toEqual([]);
        expect(buildEditPayload(form).permissions).toEqual(["legacy_read_all"]);
        expect(buildEditPayload({ ...form, keepLegacyAccess: false }).permissions).toEqual([]);
    });

    it("never adds legacy access to another account", () => {
        const form = editFormFromUser({ ...legacyUser, permissions: ["view_zones"] });
        expect(buildEditPayload({ ...form, keepLegacyAccess: true }).permissions).toEqual(["view_zones"]);
    });

    it("builds the PUT body without email fields", () => {
        expect(buildEditPayload(editFormFromUser({ ...legacyUser, validUntil: "2027-01-01" }))).toEqual({
            fullName: "Old Op",
            role: "user",
            category: "security",
            permissions: ["legacy_read_all"],
            zoneIds: ["open"],
            validUntil: "2027-01-01",
        });
    });

    it("validates like the invite form, without email", () => {
        expect(editFormErrors(editFormFromUser(legacyUser), TODAY)).toEqual({});
        expect(editFormErrors({ ...editFormFromUser(legacyUser), fullName: "" }, TODAY).fullName).toBeTruthy();
    });
});

describe("toggleValue", () => {
    it("adds and removes", () => {
        expect(toggleValue(["a"], "b")).toEqual(["a", "b"]);
        expect(toggleValue(["a", "b"], "a")).toEqual(["b"]);
    });
});

describe("summaries", () => {
    it("summarizes permissions and zones", async () => {
        const { permissionsSummary, zonesSummary } = await import("./userForm");
        expect(permissionsSummary({ role: "admin" })).toBe("All (admin)");
        expect(permissionsSummary({ role: "user", permissions: ["view_dashboard", "view_access_log"] })).toBe("Dashboard, Log");
        expect(permissionsSummary({ role: "user", permissions: [] })).toBe("None");
        expect(zonesSummary({ zoneIds: ["open", "critical"] })).toBe("Open, Critical");
        expect(zonesSummary({ zoneIds: [] })).toBe("No zone");
    });
});
