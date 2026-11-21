import { describe, expect, it } from "vitest";
import { enumKey, statusTone, translateEnum } from "./status";

describe("status helpers", () => {
  it("turns a server value into a translation key", () => {
    expect(enumKey("Open Access")).toBe("openAccess");
    expect(enumKey("Locked Down")).toBe("lockedDown");
    expect(enumKey("Approved")).toBe("approved");
  });

  it("colours known statuses and keeps unknown ones neutral", () => {
    expect(statusTone("Denied")).toBe("error");
    expect(statusTone("Approved")).toBe("success");
    expect(statusTone("Something new")).toBe("neutral");
  });

  it("falls back to the server value when no translation exists", () => {
    const t = (key, { defaultValue }) => (key === "enums.status.approved" ? "Genehmigt" : defaultValue);
    expect(translateEnum(t, "status", "Approved")).toBe("Genehmigt");
    expect(translateEnum(t, "status", "Brand new")).toBe("Brand new");
    expect(translateEnum(t, "status", null)).toBe("");
  });
});
