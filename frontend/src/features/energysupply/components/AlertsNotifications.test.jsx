// @vitest-environment jsdom
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import AlertsNotifications from "./AlertsNotifications";
import { getAllAlerts, updateAlertStatus } from "../api/energyApi";

vi.mock("../api/energyApi", () => ({
  getAllAlerts: vi.fn(() =>
    Promise.resolve([
      { alertId: 1, severity: "CRITICAL", componentName: "Inverter", message: "Overheat", timestamp: "2026-11-18T10:15:00", status: "OPEN" },
      { alertId: 2, severity: "INFO", componentId: "ENG-001", message: "Cleaned", timestamp: "2026-11-17T08:00:00", status: "RESOLVED" },
    ])
  ),
  updateAlertStatus: vi.fn(() => Promise.resolve()),
}));

describe("AlertsNotifications", () => {
  it("counts open and critical alerts and filters them by status", async () => {
    render(<AlertsNotifications />);
    expect(await screen.findByText("1 open · 1 critical requiring attention across the energy system.")).toBeInTheDocument();
    const table = screen.getByRole("table", { name: "System Alerts" });
    expect(within(table).getByText("Overheat")).toBeInTheDocument();
    expect(within(table).getByText("Cleaned")).toBeInTheDocument();
    expect(within(table).getByText("2026-11-18T10:15:00")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: "Open" }));
    expect(within(table).queryByText("Cleaned")).not.toBeInTheDocument();
    expect(within(table).getByText("Overheat")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: "Resolved" }));
    expect(within(table).queryByText("Overheat")).not.toBeInTheDocument();
    expect(within(table).getByText("Cleaned")).toBeInTheDocument();
  });

  it("resolves an open alert", async () => {
    render(<AlertsNotifications />);
    fireEvent.click(await screen.findByRole("button", { name: "Resolve" }));
    await waitFor(() => expect(updateAlertStatus).toHaveBeenCalledWith(1, "RESOLVED"));
  });

  it("loads the alerts again after resolving one and shows the new status", async () => {
    const open = { alertId: 1, severity: "WARNING", componentName: "Battery", message: "Battery charge is below reserve.", timestamp: "2026-11-10T09:00:00", status: "OPEN" };
    let alerts = [open];
    getAllAlerts.mockImplementation(async () => alerts);
    updateAlertStatus.mockImplementation(async () => {
      alerts = [{ ...open, status: "RESOLVED" }];
    });
    getAllAlerts.mockClear();

    render(<AlertsNotifications />);
    const row = (await screen.findByText(open.message)).closest("tr");
    expect(within(row).getByText("Battery")).toBeInTheDocument();
    expect(within(row).getByText("Open")).toBeInTheDocument();

    fireEvent.click(within(row).getByRole("button", { name: "Resolve" }));

    await waitFor(() => expect(getAllAlerts).toHaveBeenCalledTimes(2));
    await waitFor(() => {
      const updatedRow = screen.getByText(open.message).closest("tr");
      expect(within(updatedRow).getByText("Resolved")).toBeInTheDocument();
      expect(within(updatedRow).queryByRole("button", { name: "Resolve" })).toBeNull();
    });
  });

  it("shows an explicit error when the API cannot load the alerts", async () => {
    getAllAlerts.mockRejectedValueOnce(new Error("Service unavailable"));

    render(<AlertsNotifications />);

    expect(await screen.findByText("Failed to load alerts: Service unavailable")).toBeInTheDocument();
  });
});
