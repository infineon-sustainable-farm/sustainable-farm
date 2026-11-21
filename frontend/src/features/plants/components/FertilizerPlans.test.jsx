// @vitest-environment jsdom
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../../shared/api/client";
import FertilizerPlansSection from "./FertilizerPlansSection";

const PLANNED = {
    id: 5, fertilizerId: 7, fertilizerName: "NPK 15-15-15", unit: "KG", farmId: 1, blockCode: "B",
    plannedDate: "2026-11-24", plannedQuantity: 30, status: "PLANNED", overdue: false, daysFromToday: 10,
    unitPriceXof: 500, unitPriceEur: 0.76, priceDate: "2026-11-03", plannedCostXof: 15000, plannedCostEur: 22.87,
    movementId: null, actualDate: null, actualQuantity: null, note: "Before the rains", source: "user_entry",
};
const LATE = { ...PLANNED, id: 6, blockCode: "A", plannedDate: "2026-11-11", overdue: true, daysFromToday: -3, note: null };
const DONE = {
    ...PLANNED, id: 7, status: "DONE", daysFromToday: null, movementId: 77, actualDate: "2026-11-12", actualQuantity: 25.5,
    note: null, plannedCostXof: null, plannedCostEur: null,
};

const RESPONSES = {
    "/api/plants/fertilizer-plans": [PLANNED, LATE, DONE],
    "/api/plants/fertilizer-plans/comparison": {
        from: "2026-01-01",
        to: "2026-12-31",
        rows: [{
            blockCode: "A", fertilizerId: 7, fertilizerName: "NPK 15-15-15", unit: "KG", plannedQuantity: 60,
            remainingQuantity: 20, appliedQuantity: 40, unplannedQuantity: 5, gapQuantity: -20, gapPercent: -33.3,
            plannedCostXof: 30000, plannedCostEur: 45.73, appliedCostXof: 20000, appliedCostEur: 30.49,
            priceDate: "2026-11-03",
        }],
    },
    "/api/plants/fertilizer-plans/stock-check": {
        days: 30, today: "2026-11-14", until: "2026-12-14",
        warnings: [{ fertilizerId: 7, fertilizerName: "NPK 15-15-15", unit: "KG", plannedQuantity: 70,
            overdueQuantity: 30, currentStock: 50, shortfall: 20 }],
    },
    "/api/plants/fertilizers": [{ id: 7, name: "NPK 15-15-15", unit: "KG", currentStock: 50 }],
    "/api/farm/fields": [{ id: 1, farmId: 1, code: "A", name: "North" }, { id: 2, farmId: 1, code: "B", name: "B" }],
    "/api/staff/persons": [{ id: 4, fullName: "Field team" }],
};

vi.mock("../../../shared/api/client", () => ({
    apiClient: {
        get: vi.fn((endpoint) => Promise.resolve(RESPONSES[endpoint])),
        post: vi.fn(() => Promise.resolve({})),
    },
}));

function renderSection() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
        <QueryClientProvider client={client}>
            <MemoryRouter>
                <FertilizerPlansSection />
            </MemoryRouter>
        </QueryClientProvider>
    );
}

describe("the planned applications of Fertilizer Inventory", () => {
    beforeEach(() => {
        apiClient.post.mockClear();
    });

    it("lists the plans with their delay, cost and state, the stock warning and the comparison", async () => {
        renderSection();
        const table = await screen.findByRole("table", { name: "Planned applications" });
        expect(within(table).getByText("in 10 days")).toBeInTheDocument();
        expect(within(table).getByText("3 days late")).toBeInTheDocument();
        expect(within(table).getByText("Overdue")).toBeInTheDocument();
        expect(within(table).getAllByText("15,000 FCFA")).toHaveLength(2);
        expect(within(table).getByText("No price yet")).toBeInTheDocument();
        expect(within(table).getByText("Applied 25.5 kg on 12 Nov 2026")).toBeInTheDocument();
        expect(within(table).getByText("Before the rains")).toBeInTheDocument();

        expect(await screen.findByText("Planned applications exceed the stock in the next 30 days")).toBeInTheDocument();
        expect(screen.getByText("NPK 15-15-15: 70 kg planned in the next 30 days, 50 kg in stock, 20 kg short. "
            + "30 kg of it is overdue.")).toBeInTheDocument();

        const comparison = await screen.findByRole("table", { name: "Planned and applied by block" });
        expect(within(comparison).getByText("20 kg still to do")).toBeInTheDocument();
        expect(within(comparison).getByText("5 kg not planned")).toBeInTheDocument();
        expect(within(comparison).getByText("-20 kg")).toBeInTheDocument();
        expect(within(comparison).getByText("-33.3%")).toBeInTheDocument();
        expect(screen.getByText("1 Jan 2026 – 31 Dec 2026 · costs at last purchase price")).toBeInTheDocument();
    });

    it("plans an application on a block picked from the farm referential", async () => {
        renderSection();
        fireEvent.click(await screen.findByRole("button", { name: "Plan an application" }));
        const dialog = screen.getByRole("dialog", { name: "Plan an application" });
        await within(dialog).findByRole("option", { name: "A · North" });

        fireEvent.click(within(dialog).getByRole("button", { name: "Plan application" }));
        expect(within(dialog).getByText("Block is required.")).toBeInTheDocument();
        expect(apiClient.post).not.toHaveBeenCalled();

        fireEvent.change(within(dialog).getByLabelText("Fertilizer"), { target: { value: "7" } });
        fireEvent.change(within(dialog).getByLabelText(/^Block/), { target: { value: "A" } });
        fireEvent.change(within(dialog).getByLabelText("Planned date"), { target: { value: "2026-12-01" } });
        fireEvent.change(within(dialog).getByLabelText("Planned quantity (kg)"), { target: { value: "40" } });
        fireEvent.click(within(dialog).getByRole("button", { name: "Plan application" }));

        await waitFor(() => expect(apiClient.post).toHaveBeenCalledWith("/api/plants/fertilizer-plans", {
            fertilizerId: 7, blockCode: "A", plannedDate: "2026-12-01", plannedQuantity: 40,
        }));
    });

    it("marks a plan as done with an applicator of the people directory", async () => {
        renderSection();
        fireEvent.click(await screen.findByRole("button", { name: "Mark done: NPK 15-15-15, Block B, 24 Nov 2026" }));
        const dialog = screen.getByRole("dialog", { name: "Mark as done" });
        expect(within(dialog).getByLabelText("Date applied")).toHaveValue("2026-11-14");
        expect(within(dialog).getByLabelText("Quantity applied (kg)")).toHaveValue(30);
        await within(dialog).findByRole("option", { name: "Field team" });

        fireEvent.click(within(dialog).getByRole("button", { name: "Mark as done" }));
        expect(within(dialog).getByText("Applicator is required.")).toBeInTheDocument();

        fireEvent.change(within(dialog).getByLabelText(/^Applicator/), { target: { value: "4" } });
        fireEvent.change(within(dialog).getByLabelText("Quantity applied (kg)"), { target: { value: "25.5" } });
        fireEvent.click(within(dialog).getByRole("button", { name: "Mark as done" }));

        await waitFor(() => expect(apiClient.post).toHaveBeenCalledWith("/api/plants/fertilizer-plans/5/complete", {
            applicationDate: "2026-11-14", quantity: 25.5, applicatorPersonId: 4,
        }));
    });

    it("cancels a plan after a confirmation", async () => {
        renderSection();
        fireEvent.click(await screen.findByRole("button", { name: "Cancel: NPK 15-15-15, Block B, 24 Nov 2026" }));
        const dialog = screen.getByRole("dialog", { name: "Cancel this planned application?" });
        expect(within(dialog).getByText("NPK 15-15-15 · Block B · 30 kg on 24 Nov 2026")).toBeInTheDocument();
        fireEvent.click(within(dialog).getByRole("button", { name: "Cancel application" }));
        await waitFor(() => expect(apiClient.post).toHaveBeenCalledWith("/api/plants/fertilizer-plans/5/cancel"));
    });
});
