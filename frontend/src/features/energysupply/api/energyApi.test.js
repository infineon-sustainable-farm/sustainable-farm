import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../../../shared/api/client", () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

import { apiClient } from "../../../shared/api/client";
import * as energyApi from "./energyApi";

beforeEach(() => {
  vi.clearAllMocks();
});

function calledPaths() {
  return Object.values(apiClient).flatMap((method) => method.mock.calls.map(([path]) => path));
}

describe("energy supply api paths", () => {
  it("puts every endpoint under /api/energy-supply", () => {
    energyApi.getDashboardSummary();
    energyApi.getComponents();
    energyApi.getSolarLast7Days();
    energyApi.getAllSolarLogs();
    energyApi.getConsumptionToday();
    energyApi.getAllConsumption();
    energyApi.getConsumptionByCategory("IRRIGATION");
    energyApi.getGeneratorEvents();
    energyApi.getCarbonMetrics();
    energyApi.getAllAlerts();
    energyApi.getOpenAlerts();
    energyApi.simulateWhatIf({ additionalPanels: 1, additionalBatteryKwh: 0, productionScaleKgPerDay: 0 });

    const paths = calledPaths();
    expect(paths).toHaveLength(12);
    for (const path of paths) {
      expect(path.startsWith("/api/energy-supply/")).toBe(true);
    }
  });

  it("addresses a component by its code and the logs, events and alerts by their numeric id", () => {
    energyApi.createComponent({ componentId: "ENG-006" });
    energyApi.updateComponent("ENG-006", { componentId: "ENG-006" });
    energyApi.deleteComponent("ENG-006");
    energyApi.updateSolarLog(3, { kwhProduced: 1 });
    energyApi.deleteSolarLog(3);
    energyApi.updateConsumptionLog(4, { kwhConsumed: 1 });
    energyApi.deleteConsumptionLog(4);
    energyApi.updateGeneratorEvent(5, { costEur: 6.8 });
    energyApi.deleteGeneratorEvent(5);
    energyApi.updateAlertStatus(6, "RESOLVED");

    expect(apiClient.post).toHaveBeenCalledWith("/api/energy-supply/components", { componentId: "ENG-006" });
    expect(apiClient.put).toHaveBeenCalledWith("/api/energy-supply/components/ENG-006", { componentId: "ENG-006" });
    expect(apiClient.delete).toHaveBeenCalledWith("/api/energy-supply/components/ENG-006");
    expect(apiClient.put).toHaveBeenCalledWith("/api/energy-supply/solar/3", { kwhProduced: 1 });
    expect(apiClient.delete).toHaveBeenCalledWith("/api/energy-supply/solar/3");
    expect(apiClient.put).toHaveBeenCalledWith("/api/energy-supply/consumption/4", { kwhConsumed: 1 });
    expect(apiClient.delete).toHaveBeenCalledWith("/api/energy-supply/consumption/4");
    expect(apiClient.put).toHaveBeenCalledWith("/api/energy-supply/generator-events/5", { costEur: 6.8 });
    expect(apiClient.delete).toHaveBeenCalledWith("/api/energy-supply/generator-events/5");
    expect(apiClient.patch).toHaveBeenCalledWith("/api/energy-supply/alerts/6/status", { status: "RESOLVED" });
  });
});
