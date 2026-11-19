// @vitest-environment jsdom
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import HarvestMaturityPage from "./HarvestMaturityPage";
import { LocationProbe } from "../../../test/LocationProbe";

// The sections that read the forecast and the harvests are not what this file tests.
vi.mock("./HarvestStatusSection", () => ({ default: () => null }));
vi.mock("./UpcomingDeadlinesSection", () => ({ default: () => null }));
vi.mock("./YieldForecastSection", () => ({ default: () => null }));
vi.mock("./HarvestsSection", () => ({ default: () => null }));
vi.mock("../hooks/useVarieties", () => ({
    useVarietyBlocks: () => ({ data: ["A"] }),
    useVarieties: () => ({ data: undefined, isSuccess: false, isError: false }),
}));
vi.mock("../api/plantsApi", async (importOriginal) => ({
    ...(await importOriginal()),
    fetchVarietyReferences: vi.fn().mockResolvedValue([]),
    fetchGrowthPhaseShares: vi.fn().mockResolvedValue([]),
}));

function renderAt(url) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={[url]}>
                <HarvestMaturityPage />
                <LocationProbe />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

const location = () => screen.getByTestId("location").textContent;

describe("Harvest & maturity create links", () => {
    it('opens the record a harvest dialog on "?create=1" and removes the link on close', async () => {
        renderAt("/plants/harvest-maturity?create=1");
        const dialog = await screen.findByRole("dialog", { name: "Record a harvest" });

        fireEvent.click(within(dialog).getByRole("button", { name: "Close" }));
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(location()).toBe("/plants/harvest-maturity");
    });

    it('opens the add a variety dialog on "?create=variety" and removes the link on close', async () => {
        renderAt("/plants/harvest-maturity?create=variety");
        const dialog = await screen.findByRole("dialog", { name: "Add a variety" });
        expect(screen.getAllByRole("dialog")).toHaveLength(1);

        fireEvent.click(within(dialog).getByRole("button", { name: "Close" }));
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(location()).toBe("/plants/harvest-maturity");
    });

    it("opens no dialog without the link", async () => {
        renderAt("/plants/harvest-maturity");
        expect(await screen.findByRole("heading", { name: "Reference values" })).toBeInTheDocument();
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
});
