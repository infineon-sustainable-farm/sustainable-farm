import { describe, expect, it } from "vitest";
import routes, { nav } from "./routes";

describe("sitesecurity module routes", () => {
  it("renders inside the application shell", () => {
    expect(nav.shell).toBe(true);
    expect(nav.nameKey).toBe("sitesecurity:name");
  });

  it("has one URL per screen, each linked from the sidebar", () => {
    const [moduleRoute] = routes;
    expect(moduleRoute.path).toBe("sitesecurity");
    expect(moduleRoute.children.map((child) => child.path ?? "")).toEqual(["", "access", "zones", "log"]);
    expect(nav.items.map((item) => item.to).sort()).toEqual(
      moduleRoute.children.map((child) => (child.path ? `/sitesecurity/${child.path}` : "/sitesecurity")).sort()
    );
  });

  it("has no sign-in, activation or password route", () => {
    const paths = JSON.stringify(routes);
    for (const word of ["login", "sign-in", "signin", "activate", "password", "invitation", "reset"]) {
      expect(paths).not.toContain(word);
    }
  });
});
