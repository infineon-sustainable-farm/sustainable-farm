// @vitest-environment jsdom
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import routes, { nav } from "./routes";

// Every API call stays pending: each screen renders its header and its loading state.
vi.mock("./api/visitormanagementApi", async (importOriginal) => {
    const original = await importOriginal();
    return Object.fromEntries(Object.keys(original).map((name) => [name, () => new Promise(() => {})]));
});

const SCREENS = [
    ["/visitormanagement", "Dashboard Overview"],
    ["/visitormanagement/scheduling", "Farm tour scheduling"],
    ["/visitormanagement/registration", "Visitor registration"],
    ["/visitormanagement/visitors", "Visitors"],
    ["/visitormanagement/education", "Educational program"],
    ["/visitormanagement/safety", "Safety briefing tracker"],
    ["/visitormanagement/booking", "Agritourism booking system"],
    ["/visitormanagement/feedback", "Satisfaction survey"],
    ["/visitormanagement/events", "Events"],
    ["/visitormanagement/staff", "Staff & guides"],
];

describe("visitormanagement module routes", () => {
    it("renders inside the application shell, without a module layout of its own", () => {
        expect(nav.shell).toBe(true);
        const [moduleRoute] = routes;
        expect(moduleRoute.path).toBe("visitormanagement");
        expect(moduleRoute.element).toBeUndefined();
    });

    it("keeps one URL per screen, each one in the module navigation", () => {
        const [moduleRoute] = routes;
        const urls = moduleRoute.children.map((child) =>
            child.path ? `/visitormanagement/${child.path}` : "/visitormanagement",
        );
        expect(urls).toEqual(SCREENS.map(([url]) => url));
        expect(nav.items.map((item) => item.to)).toEqual(urls);
    });

    it.each(SCREENS)("opens %s by its URL", async (url, title) => {
        const router = createMemoryRouter(routes, { initialEntries: [url] });
        const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        render(
            <QueryClientProvider client={client}>
                <RouterProvider router={router} />
            </QueryClientProvider>,
        );
        const heading = await screen.findByRole("heading", { level: 1 });
        expect(heading).toHaveTextContent(title);
        // The screen loads on first visit (React.lazy): its title effect may run after the heading.
        await waitFor(() => expect(document.title).toMatch(/Visitor Management$/));
    });
});
