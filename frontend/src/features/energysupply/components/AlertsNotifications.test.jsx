// @vitest-environment jsdom
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import AlertsNotifications from "./AlertsNotifications";
import { updateAlertStatus } from "../api/energyApi";

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
});
