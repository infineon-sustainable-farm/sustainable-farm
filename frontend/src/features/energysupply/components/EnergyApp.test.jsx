// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom/vitest";
import { afterEach, describe, expect, it, vi } from "vitest";
import EnergyApp from "./EnergyApp";

vi.mock("./Dashboard", () => ({ default: () => <div data-testid="dashboard-page" /> }));
vi.mock("./EnergyComponents", () => ({ default: () => <div data-testid="components-page" /> }));
vi.mock("./SolarGeneration", () => ({ default: () => <div data-testid="solar-page" /> }));
vi.mock("./ConsumptionTracking", () => ({ default: () => <div data-testid="consumption-page" /> }));
vi.mock("./GeneratorBackup", () => ({ default: () => <div data-testid="generator-page" /> }));
vi.mock("./CarbonFootprint", () => ({ default: () => <div data-testid="carbon-page" /> }));
vi.mock("./WhatIfAnalysis", () => ({ default: () => <div data-testid="scenario-page" /> }));
vi.mock("./AlertsNotifications", () => ({ default: () => <div data-testid="alerts-page" /> }));

afterEach(cleanup);

describe("Energy Supply navigation", () => {
  it.each([
    ["Dashboard", "dashboard-page"],
    ["Components", "components-page"],
    ["Solar & Generation", "solar-page"],
    ["Consumption Tracking", "consumption-page"],
    ["Generator Backup", "generator-page"],
    ["Carbon Footprint", "carbon-page"],
    ["What-If Analysis", "scenario-page"],
    ["Alerts & Notifications", "alerts-page"],
  ])("opens the %s page", (label, pageTestId) => {
    render(<EnergyApp />);

    fireEvent.click(screen.getByRole("button", { name: label }));

    expect(screen.getByTestId(pageTestId)).toBeInTheDocument();
  });
});
