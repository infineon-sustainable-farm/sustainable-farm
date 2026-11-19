// @vitest-environment jsdom
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import EquipmentRegistry from "./EquipmentRegistry";
import { createEquipment, fetchEquipments } from "../api/equipmentApi";
import { LocationProbe } from "../../../test/LocationProbe";

vi.mock("../api/equipmentApi", () => ({
  fetchEquipments: vi.fn(),
  createEquipment: vi.fn(),
  updateEquipmentStatus: vi.fn(),
  deleteEquipment: vi.fn(),
}));

function renderAt(url) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[url]}>
        <EquipmentRegistry />
        <LocationProbe />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const location = () => screen.getByTestId("location").textContent;

describe("EquipmentRegistry create link", () => {
  beforeEach(() => {
    vi.mocked(fetchEquipments).mockResolvedValue({ content: [], totalPages: 0, totalElements: 0 });
    vi.mocked(createEquipment).mockResolvedValue({ id: 9 });
  });

  it('opens the add equipment dialog on "?create=1" and removes the link on close', async () => {
    renderAt("/machinery/equipment-registry?create=1");
    const dialog = await screen.findByRole("dialog", { name: "Add equipment" });

    fireEvent.click(within(dialog).getByRole("button", { name: "Close" }));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(location()).toBe("/machinery/equipment-registry");
  });

  it("removes the link once the machine is added", async () => {
    renderAt("/machinery/equipment-registry?create=1");
    const dialog = await screen.findByRole("dialog", { name: "Add equipment" });
    fireEvent.change(within(dialog).getByLabelText("Name"), { target: { value: "Dryer C" } });
    fireEvent.submit(dialog.querySelector("form"));

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(vi.mocked(createEquipment).mock.calls[0]?.[0]).toEqual(expect.objectContaining({ name: "Dryer C" }));
    expect(location()).toBe("/machinery/equipment-registry");
  });

  it("opens no dialog without the link", async () => {
    renderAt("/machinery/equipment-registry");
    expect(await screen.findByText(/no equipment/i)).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});
