// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import WhatIfAnalysis from "./WhatIfAnalysis";
import { simulateWhatIf } from "../api/energyApi";

vi.mock("../api/energyApi", () => ({
  simulateWhatIf: vi.fn(),
}));

vi.mock("recharts", () => ({
  BarChart: () => null,
  Bar: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  ResponsiveContainer: ({ children }) => children,
}));

const initialResult = {
  additionalCapexEur: 24000,
  newPaybackYears: 10.71,
  newSystemCapacityKwp: 26.5,
  additionalPvGenerationKwhPerYear: 23360,
  additionalProductionDemandKwhPerYear: 164250,
  netAnnualEnergyBalanceKwh: -140890,
  netAnnualCo2AvoidedKg: -71613,
};

beforeEach(() => {
  simulateWhatIf.mockResolvedValue(initialResult);
});

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

describe("WhatIfAnalysis", () => {
  const displayedValue = (label) =>
    screen.getByText(label).nextElementSibling.textContent.replace(/[\s\u202f]/g, "");

  it("sends changed production inputs and renders the updated result", async () => {
    simulateWhatIf.mockImplementation(async ({ productionScaleKgPerDay }) => ({
      ...initialResult,
      additionalProductionDemandKwhPerYear: productionScaleKgPerDay * 0.9 * 365,
      netAnnualEnergyBalanceKwh:
        initialResult.additionalPvGenerationKwhPerYear - productionScaleKgPerDay * 0.9 * 365,
    }));

    render(<WhatIfAnalysis />);
    await screen.findByText("Additional Production Demand");
    await waitFor(() => {
      expect(displayedValue("Additional Production Demand")).toBe("164250kWh/year");
    });

    fireEvent.change(screen.getByRole("slider", { name: "Production scale-up" }), {
      target: { value: "550" },
    });

    await waitFor(() => {
      expect(simulateWhatIf).toHaveBeenLastCalledWith({
        additionalPanels: 40,
        additionalBatteryKwh: 80,
        productionScaleKgPerDay: 550,
      });
    });
    await waitFor(() => {
      expect(displayedValue("Additional Production Demand")).toBe("180675kWh/year");
    });
  });

  it("labels negative net CO2 impact as an increase", async () => {
    render(<WhatIfAnalysis />);

    expect(await screen.findByText("Net Annual CO₂ Increase")).toBeTruthy();
    expect(displayedValue("Net Annual CO₂ Increase")).toBe("71613kg/year");
  });
});
