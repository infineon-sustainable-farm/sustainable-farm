// @vitest-environment jsdom
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import RecordMovementModal from "./RecordMovementModal";

const fertilizersQuery = vi.hoisted(() => ({ current: null }));

vi.mock("../hooks/useFertilizers", () => ({ useFertilizers: () => fertilizersQuery.current }));

vi.mock("../hooks/useFarmBlocks", () => ({
    useFarmBlocks: () => ({
        data: [
            { id: 1, farmId: 1, code: "A", name: "North block" },
            { id: 2, farmId: 1, code: "B2", name: "B2" },
            { id: 3, farmId: 1, code: "E", name: "East" },
        ],
        isPending: false,
        isError: false,
        refetch: () => {},
    }),
}));

vi.mock("../../../shared/api/persons", () => ({
    fetchPersons: vi.fn(() => Promise.resolve([{ id: 4, fullName: "Field team" }])),
}));

const CATALOGUE = [{ id: 7, name: "NPK 15-15-15", unit: "KG", currentStock: 1000 }];

function renderModal(props = {}) {
    const handlers = { onSubmit: vi.fn(), onClose: vi.fn(), onResetError: vi.fn() };
    render(<RecordMovementModal isSubmitting={false} error={null} {...handlers} {...props} />, { wrapper: MemoryRouter });
    return handlers;
}

describe("the Record a movement dialog", () => {
    beforeEach(() => {
        fertilizersQuery.current = { data: CATALOGUE, isSuccess: true, isError: false };
    });

    it("opens as a named dialog on a purchase", () => {
        renderModal();
        expect(screen.getByRole("dialog", { name: "Record a movement" })).toBeInTheDocument();
        expect(screen.getByRole("radio", { name: "Purchase" })).toBeChecked();
        expect(screen.getByLabelText("Supplier")).toBeInTheDocument();
    });

    it("refuses an empty purchase", () => {
        const { onSubmit } = renderModal();
        fireEvent.click(screen.getByRole("button", { name: "Record purchase" }));
        expect(onSubmit).not.toHaveBeenCalled();
        expect(screen.getByText("Fertilizer is required.")).toBeInTheDocument();
        expect(screen.getByText("Date is required.")).toBeInTheDocument();
        expect(screen.getByText("Quantity must be a number greater than 0, with at most 3 decimals.")).toBeInTheDocument();
        expect(screen.getByText("Supplier is required.")).toBeInTheDocument();
    });

    it("sends a purchase with its cost and currency", () => {
        const { onSubmit } = renderModal();
        fireEvent.change(screen.getByLabelText("Fertilizer"), { target: { value: "7" } });
        fireEvent.change(screen.getByLabelText("Date"), { target: { value: "2026-01-05" } });
        fireEvent.change(screen.getByLabelText("Quantity (kg)"), { target: { value: "50" } });
        fireEvent.change(screen.getByLabelText("Supplier"), { target: { value: "Agro" } });
        fireEvent.change(screen.getByLabelText("Total cost"), { target: { value: "120.5" } });
        fireEvent.change(screen.getByLabelText("Currency"), { target: { value: "EUR" } });
        fireEvent.click(screen.getByRole("button", { name: "Record purchase" }));
        expect(onSubmit).toHaveBeenCalledWith({
            movementType: "PURCHASE",
            fertilizerId: 7,
            body: { purchaseDate: "2026-01-05", quantity: 50, supplier: "Agro", totalCost: 120.5, currency: "EUR" },
        });
    });

    it("sends an application, with the stock in the quantity note", async () => {
        const { onSubmit, onResetError } = renderModal();
        fireEvent.click(screen.getByRole("radio", { name: "Application" }));
        expect(onResetError).toHaveBeenCalled();
        fireEvent.change(screen.getByLabelText("Fertilizer"), { target: { value: "7" } });
        expect(screen.getByLabelText("Quantity (kg)")).toHaveAccessibleDescription("In stock: 1,000 kg");
        fireEvent.change(screen.getByLabelText("Date"), { target: { value: "2026-01-05" } });
        fireEvent.change(screen.getByLabelText("Quantity (kg)"), { target: { value: "2" } });
        await screen.findByRole("option", { name: "Field team" });
        fireEvent.change(screen.getByLabelText(/^Applicator/), { target: { value: "4" } });
        fireEvent.click(screen.getByRole("button", { name: "Record application" }));
        expect(onSubmit).not.toHaveBeenCalled();
        expect(screen.getByText("Block is required.")).toBeInTheDocument();
        fireEvent.change(screen.getByLabelText("Block"), { target: { value: "B2" } });
        fireEvent.click(screen.getByRole("button", { name: "Record application" }));
        expect(onSubmit).toHaveBeenCalledWith({
            movementType: "APPLICATION",
            fertilizerId: 7,
            body: { applicationDate: "2026-01-05", quantity: 2, blockCode: "B2", applicatorPersonId: 4 },
        });
    });

    it("sends a loss with its reason", () => {
        const { onSubmit } = renderModal();
        fireEvent.click(screen.getByRole("radio", { name: "Loss" }));
        fireEvent.change(screen.getByLabelText("Fertilizer"), { target: { value: "7" } });
        fireEvent.change(screen.getByLabelText("Date"), { target: { value: "2026-01-05" } });
        fireEvent.change(screen.getByLabelText("Quantity (kg)"), { target: { value: "1" } });
        fireEvent.change(screen.getByLabelText("Reason"), { target: { value: "Rain" } });
        fireEvent.click(screen.getByRole("button", { name: "Record loss" }));
        expect(onSubmit).toHaveBeenCalledWith({
            movementType: "LOSS",
            fertilizerId: 7,
            body: { lossDate: "2026-01-05", quantity: 1, reason: "Rain" },
        });
    });

    it("blocks the submission while the catalogue is empty", () => {
        fertilizersQuery.current = { data: [], isSuccess: true, isError: false };
        renderModal();
        expect(screen.getByLabelText("Fertilizer")).toHaveAccessibleDescription("No fertilizer in the catalogue yet");
        expect(screen.getByRole("button", { name: "Record purchase" })).toBeDisabled();
    });
});
