import { describe, expect, it } from "vitest";
import { accessLabel, canOpen, visibleScreens } from "./accessRights";

const user = (permissions) => ({ role: "user", permissions });

describe("visibleScreens", () => {
    it("shows everything and Users to an admin", () => {
        expect(visibleScreens({ role: "admin", permissions: [] })).toEqual(["dashboard", "access", "zones", "log", "users"]);
    });

    it("shows the four views to a legacy operator", () => {
        expect(visibleScreens(user(["legacy_read_all"]))).toEqual(["dashboard", "access", "zones", "log"]);
    });

    it("shows only the permitted views to a user", () => {
        expect(visibleScreens(user(["view_access_log", "view_zones"]))).toEqual(["zones", "log"]);
        expect(visibleScreens(user([]))).toEqual([]);
        expect(canOpen(user(["view_dashboard"]), "users")).toBe(false);
    });

    it("shows nothing without a session", () => {
        expect(visibleScreens(null)).toEqual([]);
    });
});

describe("accessLabel", () => {
    it.each([
        [{ role: "admin", permissions: [] }, "Admin access"],
        [user(["legacy_read_all"]), "Legacy read-only access"],
        [user(["view_zones"]), "User access"],
    ])("labels %j", (session, label) => {
        expect(accessLabel(session)).toBe(label);
    });
});
