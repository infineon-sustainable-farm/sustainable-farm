import { describe, expect, it } from "vitest";
import routes, { nav } from "./routes";
import LowStockNotifications from "./components/LowStockNotifications";

describe("machinery module routes", () => {
  it("renders inside the application shell, with the low stock notifications in the sidebar footer", () => {
    expect(nav.shell).toBe(true);
    expect(nav.sidebarFooter).toBe(LowStockNotifications);
  });

  it("keeps the same URLs, without a module layout of its own", () => {
    const [moduleRoute] = routes;
    expect(moduleRoute.path).toBe("machinery");
    expect(moduleRoute.element).toBeUndefined();
    expect(moduleRoute.children.map((child) => child.path ?? "")).toEqual([
      "",
      "equipment-registry",
      "operators",
      "spare-parts",
      "maintenance",
      "usage-fuel-log",
      "repair-log",
    ]);
    expect(nav.items.map((item) => item.to).sort()).toEqual(
      moduleRoute.children.map((child) => (child.path ? `/machinery/${child.path}` : "/machinery")).sort()
    );
  });
});
