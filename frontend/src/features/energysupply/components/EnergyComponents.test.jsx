// @vitest-environment jsdom
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import EnergyComponents from "./EnergyComponents";
import { createComponent, deleteComponent, getComponents } from "../api/energyApi";

vi.mock("../api/energyApi", () => ({
  getComponents: vi.fn(),
  createComponent: vi.fn(() => Promise.resolve({})),
  updateComponent: vi.fn(() => Promise.resolve({})),
  deleteComponent: vi.fn(() => Promise.resolve()),
}));

const COMPONENTS = [
  { componentId: "ENG-001", name: "Roof array", category: "PV_ARRAY", capacityValue: 12.5, status: "OPERATIONAL" },
  { componentId: "ENG-002", name: "Diesel unit", category: "GENERATOR", capacityValue: null, status: "UNDER_MAINTENANCE" },
];

describe("EnergyComponents", () => {
  beforeEach(() => {
    vi.mocked(getComponents).mockResolvedValue(COMPONENTS);
    vi.mocked(createComponent).mockClear();
    vi.mocked(deleteComponent).mockClear();
  });

  it("lists the registry with translated enum labels", async () => {
    render(<EnergyComponents />);
    const table = await screen.findByRole("table", { name: "Component registry" });
    const row = within(table).getByText("Roof array").closest("tr");
    expect(row).toHaveTextContent("ENG-001");
    expect(row).toHaveTextContent("PV Array");
    expect(row).toHaveTextContent("12.5");
    expect(row).toHaveTextContent("Operational");
    expect(within(table).getByText("Diesel unit").closest("tr")).toHaveTextContent("—");
  });

  it("asks for confirmation before deleting, and deletes only once confirmed", async () => {
    render(<EnergyComponents />);
    fireEvent.click(await screen.findByRole("button", { name: "Delete ENG-001" }));
    const dialog = screen.getByRole("dialog");
    expect(dialog).toHaveTextContent("Delete component ENG-001? Linked solar logs or alerts may prevent deletion.");

    fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(deleteComponent).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Delete ENG-001" }));
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Delete" }));
    await waitFor(() => expect(deleteComponent).toHaveBeenCalledWith("ENG-001"));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("validates the form and creates the component under the next registry id", async () => {
    render(<EnergyComponents />);
    fireEvent.click(await screen.findByRole("button", { name: "Add component" }));
    const dialog = screen.getByRole("dialog", { name: "Add component" });
    expect(within(dialog).getByLabelText("Registry ID")).toHaveValue("ENG-003");

    fireEvent.click(within(dialog).getByRole("button", { name: "Add component" }));
    expect(within(dialog).getByText("Name is required.")).toBeInTheDocument();
    expect(createComponent).not.toHaveBeenCalled();

    fireEvent.change(within(dialog).getByLabelText("Name"), { target: { value: "Battery bank" } });
    fireEvent.change(within(dialog).getByLabelText("Category"), { target: { value: "BATTERY" } });
    fireEvent.change(within(dialog).getByLabelText("Capacity"), { target: { value: "40" } });
    fireEvent.click(within(dialog).getByRole("button", { name: "Add component" }));

    await waitFor(() =>
      expect(createComponent).toHaveBeenCalledWith({
        componentId: "ENG-003",
        name: "Battery bank",
        category: "BATTERY",
        capacityValue: 40,
        status: "OPERATIONAL",
      })
    );
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });
});
