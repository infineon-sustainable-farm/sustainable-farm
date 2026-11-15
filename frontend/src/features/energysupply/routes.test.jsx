import { describe, expect, it, vi } from "vitest";

vi.mock("./components/EnergyApp", () => ({
  default: function EnergyApp() {
    return null;
  },
}));

import routes from "./routes";

describe("energy supply routes", () => {
  it("mounts the Energy Supply app at /energy-supply", () => {
    expect(routes).toHaveLength(1);
    expect(routes[0].path).toBe("energy-supply");
    expect(routes[0].element.type.name).toBe("EnergyApp");
  });
});
