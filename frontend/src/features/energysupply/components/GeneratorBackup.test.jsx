// @vitest-environment jsdom
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import GeneratorBackup from "./GeneratorBackup";
import { createGeneratorEvent, deleteGeneratorEvent } from "../api/energyApi";

vi.mock("../api/energyApi", () => ({
  getComponents: vi.fn(() =>
    Promise.resolve([{ componentId: "ENG-004", category: "GENERATOR", capacityValue: 30, status: "OPERATIONAL" }])
  ),
  getGeneratorEvents: vi.fn(() =>
    Promise.resolve([
      { eventId: 7, date: "2026-11-02", triggerReason: "Grid outage", durationHours: 3, fuelLiters: null, costEur: 18.5 },
    ])
  ),
  getDashboardSummary: vi.fn(() => Promise.resolve({ runtimeHoursThisMonth: 3, fuelLitersThisMonth: 12.25 })),
  createGeneratorEvent: vi.fn(() => Promise.resolve({})),
  updateGeneratorEvent: vi.fn(() => Promise.resolve({})),
  deleteGeneratorEvent: vi.fn(() => Promise.resolve()),
}));

describe("GeneratorBackup", () => {
  beforeEach(() => {
    vi.mocked(createGeneratorEvent).mockClear();
    vi.mocked(deleteGeneratorEvent).mockClear();
  });

  it("shows the generator figures and the events as before", async () => {
    render(<GeneratorBackup />);
    expect(await screen.findByRole("heading", { level: 1, name: "Generator Backup Management" })).toBeInTheDocument();
    expect(screen.getByText("Fuel Used (This Month)").closest("div")).toHaveTextContent("12.3L");
    const row = screen.getByText("Grid outage").closest("tr");
    expect(row).toHaveTextContent("2026-11-02");
    expect(row).toHaveTextContent("3.0");
    expect(row).toHaveTextContent("not recorded");
    expect(row).toHaveTextContent("€18.50");
  });

  it("confirms before deleting an event", async () => {
    render(<GeneratorBackup />);
    fireEvent.click(await screen.findByRole("button", { name: "Delete event from 2026-11-02" }));
    const dialog = screen.getByRole("dialog");
    expect(dialog).toHaveTextContent("Delete the backup event from 2026-11-02?");
    fireEvent.click(within(dialog).getByRole("button", { name: "Delete" }));
    await waitFor(() => expect(deleteGeneratorEvent).toHaveBeenCalledWith(7));
  });

  it("validates the event form and saves it", async () => {
    render(<GeneratorBackup />);
    fireEvent.click(await screen.findByRole("button", { name: "Add event" }));
    const dialog = screen.getByRole("dialog", { name: "Add backup event" });

    fireEvent.change(within(dialog).getByLabelText("Date"), { target: { value: "2999-01-01" } });
    fireEvent.change(within(dialog).getByLabelText("Fuel (liters)"), { target: { value: "-2" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "Add event" }));
    expect(within(dialog).getByText("Date must be today or in the past.")).toBeInTheDocument();
    expect(within(dialog).getByText("Enter a value greater than or equal to 0.")).toBeInTheDocument();
    expect(createGeneratorEvent).not.toHaveBeenCalled();

    fireEvent.change(within(dialog).getByLabelText("Date"), { target: { value: "2026-11-01" } });
    fireEvent.change(within(dialog).getByLabelText("Fuel (liters)"), { target: { value: "8" } });
    fireEvent.change(within(dialog).getByLabelText("Trigger reason"), { target: { value: " Low battery " } });
    fireEvent.click(within(dialog).getByRole("button", { name: "Add event" }));

    await waitFor(() =>
      expect(createGeneratorEvent).toHaveBeenCalledWith({
        date: "2026-11-01",
        triggerReason: "Low battery",
        durationHours: null,
        fuelLiters: 8,
        costEur: null,
      })
    );
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });

  it("closes the form with its close button", async () => {
    render(<GeneratorBackup />);
    fireEvent.click(await screen.findByRole("button", { name: "Add event" }));
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Close form" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});
