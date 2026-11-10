// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import AlertsNotifications from "./AlertsNotifications";
import { getAllAlerts, updateAlertStatus } from "../api/energyApi";

vi.mock("../api/energyApi", () => ({
  getAllAlerts: vi.fn(),
  updateAlertStatus: vi.fn(),
}));

const openAlert = {
  alertId: "alert-1",
  componentId: "ENG-002",
  componentName: "Battery",
  severity: "WARNING",
  message: "Battery charge is below reserve.",
  timestamp: "2026-11-10T09:00:00",
  status: "OPEN",
};

const resolvedAlert = { ...openAlert, status: "RESOLVED" };
let alerts;

beforeEach(() => {
  alerts = [openAlert];
  getAllAlerts.mockImplementation(async () => alerts);
  updateAlertStatus.mockImplementation(async () => {
    alerts = [resolvedAlert];
    return resolvedAlert;
  });
});

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

describe("AlertsNotifications", () => {
  it("loads alerts from the API and refreshes after resolving one", async () => {
    render(<AlertsNotifications />);

    const message = await screen.findByText(openAlert.message);
    const row = message.closest("tr");
    expect(within(row).getByText("Battery")).toBeTruthy();
    expect(within(row).getByText("Open")).toBeTruthy();

    fireEvent.click(within(row).getByRole("button", { name: "Resolve" }));

    await waitFor(() => {
      expect(updateAlertStatus).toHaveBeenCalledWith("alert-1", "RESOLVED");
      expect(getAllAlerts).toHaveBeenCalledTimes(2);
    });
    await waitFor(() => {
      const updatedRow = screen.getByText(openAlert.message).closest("tr");
      expect(within(updatedRow).getByText("Resolved")).toBeTruthy();
      expect(within(updatedRow).queryByRole("button", { name: "Resolve" })).toBeNull();
    });
  });

  it("shows an explicit error when the API cannot load alerts", async () => {
    getAllAlerts.mockRejectedValueOnce(new Error("Service unavailable"));

    render(<AlertsNotifications />);

    expect((await screen.findByRole("alert")).textContent).toBe(
      "Failed to load alerts: Service unavailable",
    );
  });
});
