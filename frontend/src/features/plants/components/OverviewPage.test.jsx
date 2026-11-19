// @vitest-environment jsdom
import { render, screen, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import i18n from "../../../shared/i18n/i18n";
import OverviewPage from "./OverviewPage";

const RESPONSES = {
    "/api/plants/varieties": [{ id: 1, blockCode: "A" }],
    "/api/plants/overview": {
        today: "2026-11-06",
        totalPlants: 120,
        inGrowthStage: 30,
        readyForHarvest: 0,
        activeAlerts: 1,
        criticalAlerts: 1,
        maturity: [
            { maturityStatus: "IMMATURE", treeCount: 80, sharePct: 66.7 },
            { maturityStatus: "RIPE", treeCount: 40, sharePct: 33.3 },
            { maturityStatus: "READY_FOR_HARVEST", treeCount: 0, sharePct: 0 },
        ],
    },
    "/api/plants/alerts": {
        today: "2026-11-06",
        alerts: [
            {
                type: "LOW_STOCK",
                severity: "CRITICAL",
                message: "Stock of NPK is low.",
                date: null,
                daysFromToday: null,
                fertilizerId: 4,
                blockCode: null,
            },
        ],
    },
    "/api/plants/nursery-batches": [],
    "/api/plants/health-findings": [],
    "/api/plants/fertilizers": [],
    "/api/plants/yield-comparison": { months: 12, from: "2026-01", blocks: [], notCompared: [] },
};

vi.mock("../../../shared/api/client", () => ({
    apiClient: { get: vi.fn((endpoint) => Promise.resolve(RESPONSES[endpoint])) },
}));

function renderPage() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
        <QueryClientProvider client={client}>
            <MemoryRouter>
                <OverviewPage />
            </MemoryRouter>
        </QueryClientProvider>
    );
}

describe("the plants overview", () => {
    it("shows the figures, the alerts and the charts of the day, in English as before", async () => {
        renderPage();
        expect(screen.getByRole("heading", { level: 1, name: "Overview" })).toBeInTheDocument();
        expect(await screen.findByText("As of 6 Nov 2026", { exact: false })).toBeInTheDocument();

        const figures = screen.getByRole("region", { name: "Figures of the day" });
        expect(within(figures).getByText("Total Plants")).toBeInTheDocument();
        expect(within(figures).getByText("120")).toBeInTheDocument();
        // The Active Alerts note counts the listed alerts of the "Act now" group.
        expect(await within(figures).findByText("1 Act now")).toBeInTheDocument();

        const alerts = screen.getByRole("region", { name: "Alerts" });
        expect(await within(alerts).findByText("Low stock")).toBeInTheDocument();
        const actNow = within(alerts).getByRole("heading", { level: 3, name: "Act now · 1 alert" });
        expect(within(alerts).getByRole("list", { name: "Act now · 1 alert" })).toContainElement(
            within(alerts).getByText("Low stock"));
        expect(actNow).toBeInTheDocument();
        expect(within(alerts).queryByRole("heading", { name: /Plan ahead/ })).not.toBeInTheDocument();
        expect(within(alerts).getByText("Inventory")).toBeInTheDocument();
        expect(within(alerts).getByText("Now")).toBeInTheDocument();
        expect(within(alerts).getByRole("link", { name: "View: Low stock, Inventory, on Fertilizer Inventory" }))
            .toHaveAttribute("href", "/plants/fertilizer-inventory");

        expect(await screen.findByText("No nursery batch yet.")).toBeInTheDocument();
        expect(screen.getByText("No fertilizer in the catalogue yet.")).toBeInTheDocument();
        expect(screen.getByText("Expected and harvested over 12 months from Jan 2026.")).toBeInTheDocument();
    });

    it("reads in French with the active language", async () => {
        await i18n.changeLanguage("fr");
        renderPage();
        expect(screen.getByRole("heading", { level: 1, name: "Vue d’ensemble" })).toBeInTheDocument();
        const alerts = screen.getByRole("region", { name: "Alertes" });
        expect(await within(alerts).findByText("Stock bas")).toBeInTheDocument();
        expect(within(alerts).getByText("Critique")).toBeInTheDocument();
    });
});
