// @vitest-environment jsdom
import { render, screen, within } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import routes, { nav } from "./routes";
import { collectModules } from "../../app/modules";
import { buildAppRoutes } from "../../app/routes";

vi.mock("./api/energyApi", () => ({
  getDashboardSummary: vi.fn(() => Promise.resolve({})),
  getComponents: vi.fn(() => Promise.resolve([])),
  getSolarLast7Days: vi.fn(() => Promise.resolve([])),
  getAllSolarLogs: vi.fn(() => Promise.resolve([])),
  getConsumptionToday: vi.fn(() => Promise.resolve([])),
  getAllConsumption: vi.fn(() => Promise.resolve([])),
  getGeneratorEvents: vi.fn(() => Promise.resolve([])),
  getCarbonMetrics: vi.fn(() => Promise.resolve([])),
  getAllAlerts: vi.fn(() => Promise.resolve([])),
  simulateWhatIf: vi.fn(() => Promise.resolve(null)),
}));

// URL, sidebar label (the former tab label) and page title of each screen.
const SCREENS = [
  ["/energy-supply", "Dashboard", "System Overview"],
  ["/energy-supply/components", "Components", "Energy Components"],
  ["/energy-supply/solar", "Solar & Generation", "Solar & Generation Monitoring"],
  ["/energy-supply/consumption", "Consumption Tracking", "Energy Consumption Tracking"],
  ["/energy-supply/generator", "Generator Backup", "Generator Backup Management"],
  ["/energy-supply/carbon", "Carbon Footprint", "Carbon Footprint"],
  ["/energy-supply/scenario", "What-If Analysis", "What-If Scenario Analysis"],
  ["/energy-supply/alerts", "Alerts & Notifications", "Alerts & Notifications"],
];

function renderAt(path) {
  const files = { "../features/energysupply/routes.jsx": { default: routes, nav } };
  const router = createMemoryRouter(buildAppRoutes(collectModules(files)), { initialEntries: [path] });
  render(<RouterProvider router={router} />);
}

describe("energy supply routes", () => {
  it("renders inside the application shell, one route per former tab, the dashboard as index", () => {
    expect(nav.shell).toBe(true);
    const [moduleRoute] = routes;
    expect(moduleRoute.path).toBe("energy-supply");
    expect(moduleRoute.element).toBeUndefined();
    expect(moduleRoute.children[0]).toMatchObject({ index: true });
    expect(moduleRoute.children.map((child) => child.path ?? "")).toEqual([
      "",
      "components",
      "solar",
      "consumption",
      "generator",
      "carbon",
      "scenario",
      "alerts",
    ]);
    expect(nav.items.map((item) => item.to)).toEqual(
      moduleRoute.children.map((child) => (child.path ? `/energy-supply/${child.path}` : "/energy-supply"))
    );
  });

  it.each(SCREENS)("opens %s with its screen and its sidebar link marked current", async (path, label, title) => {
    renderAt(path);
    expect(await screen.findByRole("heading", { level: 1, name: title })).toBeInTheDocument();
    const sidebar = screen.getByRole("navigation", { name: "Modules" });
    expect(within(sidebar).getByRole("link", { name: label })).toHaveAttribute("aria-current", "page");
  });

  it("lists the eight screens in the sidebar with the former tab labels", () => {
    renderAt("/energy-supply");
    const sidebar = screen.getByRole("navigation", { name: "Modules" });
    for (const [path, label] of SCREENS) {
      expect(within(sidebar).getByRole("link", { name: label })).toHaveAttribute("href", path);
    }
  });
});
