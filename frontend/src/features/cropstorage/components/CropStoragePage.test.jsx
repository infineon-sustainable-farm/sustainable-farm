// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import CropStoragePage from "./CropStoragePage";
import { findMatchingBatches } from "../utils/batchSearch";

vi.mock("@tanstack/react-query", () => ({ useQuery: vi.fn() }));

const batches = [
    { id: 1, batchCode: "B-PRIO-001", productType: "Kent" },
    { id: 2, batchCode: "B-PRIO-002", product_type: "Keitt" },
    { id: 3, batchCode: "B-PRIO-003", productName: "Amelie" },
    { id: 4, batchCode: "B-PRIO-004", cropType: "Tommy Atkins" },
    { id: 5, batchCode: "B-PRIO-005", product: { name: "Ataulfo" } },
    { id: 6, batchCode: "MNG-2026-003", productType: "Kent" },
    { id: 7, batchCode: "MNG-2026-003-A", productType: "Keitt" },
];

const dashboardData = {
    summary: {},
    activeAlerts: [],
    activeAlertCount: 0,
    batches,
    zones: [{ id: 1, name: "Zone 1", capacityKg: 100, currentQuantityKg: 20 }],
    zoneSensors: [],
    latestReadings: [],
    temperatureTrend: [],
    humidityTrend: [],
    overallUsagePercent: 20,
};

function renderDashboard() {
    useQuery.mockReturnValue({
        data: dashboardData,
        isPending: false,
        isError: false,
        isFetching: false,
        refetch: vi.fn(),
    });
    return render(
        <MemoryRouter>
            <CropStoragePage />
        </MemoryRouter>,
    );
}

afterEach(cleanup);

beforeEach(() => {
    vi.clearAllMocks();
    HTMLElement.prototype.scrollIntoView = vi.fn();
});

describe("Crop Storage overview batch search", () => {
    it("matches the displayed ID and product labels across loaded batches", () => {
        expect(findMatchingBatches(batches, "  mng-2026-003 ")).toEqual([batches[5]]);
        expect(findMatchingBatches(batches, "b-prio-00")).toEqual(batches.slice(0, 5));
        expect(findMatchingBatches(batches, " kEnT ")).toEqual([batches[0], batches[5]]);
        expect(findMatchingBatches(batches, "not-a-batch")).toEqual([]);
        expect(findMatchingBatches(batches, "MNG-2026-003")).toEqual([batches[5]]);
    });

    it("suggests and filters an exact batch ID beyond the initial five rows", async () => {
        const user = userEvent.setup();
        renderDashboard();
        const input = screen.getByRole("combobox", { name: "Filter stored batches" });

        await user.type(input, "MNG-2026-003");

        const listbox = screen.getByRole("listbox");
        expect(within(listbox).getByText("MNG-2026-003")).toBeTruthy();
        await user.click(within(listbox).getByRole("option"));

        expect(input.value).toBe("MNG-2026-003");
        const table = screen.getByRole("table");
        expect(within(table).getByText("MNG-2026-003")).toBeTruthy();
        expect(within(table).queryByText("B-PRIO-001")).toBeNull();
        await waitFor(() => {
            expect(HTMLElement.prototype.scrollIntoView).toHaveBeenCalledWith({
                behavior: "smooth",
                block: "start",
            });
        });
    });

    it("matches partial IDs and product names without case sensitivity or surrounding spaces", async () => {
        const user = userEvent.setup();
        renderDashboard();
        const input = screen.getByRole("combobox", { name: "Filter stored batches" });

        await user.type(input, "  b-prio-00 ");
        expect(within(screen.getByRole("listbox")).getAllByRole("option")).toHaveLength(5);

        await user.clear(input);
        await user.type(input, "  kEnT ");
        expect(within(screen.getByRole("listbox")).getAllByRole("option")).toHaveLength(2);
    });

    it("shows a no-match message and supports Escape, keyboard selection, and clearing", async () => {
        const user = userEvent.setup();
        renderDashboard();
        const input = screen.getByRole("combobox", { name: "Filter stored batches" });

        await user.type(input, "not-a-batch");
        expect(screen.getByRole("status").textContent).toBe("No matching batches.");
        await user.keyboard("{Escape}");
        expect(screen.queryByRole("listbox")).toBeNull();

        await user.clear(input);
        await user.type(input, "B-PRIO-002");
        await user.keyboard("{ArrowDown}{Enter}");
        expect(input.value).toBe("B-PRIO-002");
        expect(within(screen.getByRole("table")).getByText("B-PRIO-002")).toBeTruthy();
        expect(within(screen.getByRole("table")).queryByText("B-PRIO-001")).toBeNull();

        await user.clear(input);
        expect(screen.queryByRole("listbox")).toBeNull();
        expect(within(screen.getByRole("table")).getAllByRole("row")).toHaveLength(6);
    });
});
