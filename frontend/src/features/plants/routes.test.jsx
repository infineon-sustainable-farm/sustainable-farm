// @vitest-environment jsdom
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import routes, { nav } from "./routes";

// Every request stays pending: the test checks that each screen is reached by its URL, not its data.
vi.mock("../../shared/api/client", () => {
    const pending = () => new Promise(() => {});
    return { apiClient: { get: vi.fn(pending), post: vi.fn(pending), put: vi.fn(pending), patch: vi.fn(pending), delete: vi.fn(pending) } };
});
const SCREENS = [
    ["", "Overview"],
    ["varieties", "Varieties"],
    ["growth-calendar", "Planting Calendar & Growth Stages"],
    ["fertilizer-inventory", "Fertilizer Inventory"],
    ["diseases-alerts", "Diseases & Alerts"],
    ["harvest-maturity", "Harvest & Maturity"],
    ["nursery", "Nursery"],
    ["scenario-analysis", "Scenario Analysis"],
];

function renderAt(path) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const router = createMemoryRouter(routes, { initialEntries: [path] });
    render(
        <QueryClientProvider client={client}>
            <RouterProvider router={router} />
        </QueryClientProvider>
    );
}

describe("plants module routes", () => {
    it("renders inside the application shell, without a module layout of its own", () => {
        const [moduleRoute] = routes;
        expect(nav.shell).toBe(true);
        expect(moduleRoute.path).toBe("plants");
        expect(moduleRoute.element).toBeUndefined();
    });

    it("keeps the same URLs, one sidebar entry per screen", () => {
        const [moduleRoute] = routes;
        expect(moduleRoute.children.map((child) => child.path ?? "")).toEqual(SCREENS.map(([path]) => path));
        expect(nav.items.map((item) => item.to)).toEqual(
            SCREENS.map(([path]) => (path ? `/plants/${path}` : "/plants"))
        );
    });

    it.each(SCREENS)("reaches the screen at /plants/%s, titled %s", (path, title) => {
        renderAt(path ? `/plants/${path}` : "/plants");
        expect(screen.getByRole("heading", { level: 1, name: title })).toBeInTheDocument();
    });
});
