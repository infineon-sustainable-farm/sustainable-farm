import { describe, expect, it } from "vitest";
import { accessLabelKey, canOpen, visibleScreens } from "./accessRights";

const user = (permissions) => ({ role: "user", permissions });

describe("visibleScreens", () => {
  it("shows every operational screen to an admin", () => {
    expect(visibleScreens({ role: "admin", permissions: [] })).toEqual(["dashboard", "access", "zones", "log"]);
  });

  it("shows the four views to a legacy operator", () => {
    expect(visibleScreens(user(["legacy_read_all"]))).toEqual(["dashboard", "access", "zones", "log"]);
  });

  it("shows only the permitted views to a user", () => {
    expect(visibleScreens(user(["view_access_log", "view_zones"]))).toEqual(["zones", "log"]);
    expect(visibleScreens(user([]))).toEqual([]);
    expect(canOpen(user(["view_dashboard"]), "users")).toBe(false);
  });

  it("shows nothing without an authorization", () => {
    expect(visibleScreens(null)).toEqual([]);
  });
});

describe("accessLabelKey", () => {
  it.each([
    [{ role: "admin", permissions: [] }, "access.admin"],
    [user(["legacy_read_all"]), "access.legacyRead"],
    [user(["view_zones"]), "access.user"],
  ])("labels %j", (authorization, key) => {
    expect(accessLabelKey(authorization)).toBe(key);
  });
});
