// @vitest-environment jsdom
import { afterEach, beforeAll, describe, expect, it, vi } from "vitest";
import { act, cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import cropStorageRoutes, { nav } from "../routes";
import ScenarioAnalysisPage from "./ScenarioAnalysisPage";
import { i18n } from "../../../shared/i18n";

beforeAll(() => {
    // Recharts' ResponsiveContainer needs ResizeObserver, absent from jsdom.
    globalThis.ResizeObserver ??= class {
        observe() {}
        unobserve() {}
        disconnect() {}
    };
});

afterEach(() => {
    cleanup();
    vi.useRealTimers();
});

function renderPage() {
    return render(
        <MemoryRouter>
            <ScenarioAnalysisPage />
        </MemoryRouter>,
    );
}

function result(label) {
    const term = screen.getByText(label, { selector: "dt" });
    return term.parentElement.querySelector("dd").textContent;
}

describe("Scenario Analysis page", () => {
    it("is routed inside the Crop Storage module and listed in its navigation", () => {
        const children = cropStorageRoutes[0].children.map((route) => route.path ?? "index");
        expect(children).toEqual(
            expect.arrayContaining(["index", "zones", "batches", "movements", "alerts", "quality", "scenario-analysis"]),
        );

        expect(cropStorageRoutes[0].path).toBe("crop-storage");
        expect(nav.shell).toBe(true);
        expect(nav.items.map((item) => item.to)).toContain("/crop-storage/scenario-analysis");

        render(
            <MemoryRouter initialEntries={["/crop-storage/scenario-analysis"]}>
                <Routes>
                    <Route path="/crop-storage/scenario-analysis" element={<ScenarioAnalysisPage />} />
                </Routes>
            </MemoryRouter>,
        );
        expect(screen.getByRole("heading", { name: "What-if Scenario Analysis" })).toBeTruthy();
    });

    it("is translated: French labels and French number format", async () => {
        await act(() => i18n.changeLanguage("fr"));
        renderPage();

        expect(await screen.findByRole("heading", { name: "Analyse de scénarios « et si »" })).toBeTruthy();
        expect(screen.getByRole("button", { name: "Petite extension" }).getAttribute("aria-pressed")).toBe("true");
        // fr-FR groups thousands with a narrow no-break space.
        expect(result("Investissement initial").replace(/\s/g, " ")).toBe("7 750 000 FCFA");
    });

    it("starts on the Small expansion preset and states that values are unvalidated", () => {
        renderPage();

        expect(screen.getByRole("button", { name: "Small expansion" }).getAttribute("aria-pressed")).toBe("true");
        expect(screen.getByRole("note").textContent).toMatch(/unvalidated assumptions/);
        expect(screen.getByLabelText("Additional storage capacity").value).toBe("50");
        expect(screen.getByLabelText("Additional sensors").value).toBe("10");
        expect(result("Initial investment")).toBe("7,750,000 FCFA");
        expect(result("Break-even point")).toBe("27 months (2.3 years)");
        expect(result("Net gain after 5 years")).toBe("+9,850,000 FCFA");
    });

    it("exposes slider ranges and steps from the prototype", () => {
        renderPage();
        const storage = screen.getByLabelText("Additional storage capacity");
        const sensors = screen.getByLabelText("Additional sensors");

        expect([storage.min, storage.max, storage.step]).toEqual(["0", "200", "10"]);
        expect([sensors.min, sensors.max, sensors.step]).toEqual(["0", "50", "1"]);
    });

    it("applies a preset to every control and every result", () => {
        renderPage();

        fireEvent.click(screen.getByRole("button", { name: "Full upgrade" }));

        expect(screen.getByRole("button", { name: "Full upgrade" }).getAttribute("aria-pressed")).toBe("true");
        expect(screen.getByRole("button", { name: "Small expansion" }).getAttribute("aria-pressed")).toBe("false");
        expect(screen.getByLabelText("Additional storage capacity").value).toBe("150");
        expect(screen.getByLabelText("Additional sensors").value).toBe("30");
        expect(screen.getByRole("switch", { name: "Cooling upgrade (refrigeration)" }).checked).toBe(true);
        expect(screen.getByRole("switch", { name: "Solar power installation" }).checked).toBe(true);
        expect(result("Initial investment")).toBe("28,750,000 FCFA");
        expect(result("Annual net benefit")).toBe("4,130,500 FCFA");
        expect(result("Break-even point")).toBe("84 months (7.0 years)");
        expect(result("Net gain after 5 years")).toBe("−8,097,500 FCFA");
        expect(result("Financial outlook")).toBe("Break-even after 5 years");

        fireEvent.click(screen.getByRole("button", { name: "Solar power only" }));
        expect(result("Initial investment")).toBe("3,500,000 FCFA");
        expect(result("Break-even point")).toBe("38 months (3.2 years)");
    });

    it("recalculates on manual changes and clears the active preset", () => {
        renderPage();

        fireEvent.change(screen.getByLabelText("Additional storage capacity"), { target: { value: "0" } });
        expect(screen.getByRole("button", { name: "Small expansion" }).getAttribute("aria-pressed")).toBe("false");
        expect(result("Additional capacity")).toBe("0.0 t");
        expect(result("Initial investment")).toBe("250,000 FCFA");

        fireEvent.click(screen.getByRole("switch", { name: "Solar power installation" }));
        expect(result("Initial investment")).toBe("3,750,000 FCFA");

        const table = screen.getByRole("table");
        const rows = within(table).getAllByRole("row");
        expect(rows).toHaveLength(6);
        expect(within(rows[1]).getByRole("rowheader").textContent).toBe("Year 1");
    });

    it("shows a non-viable outlook without NaN when nothing is simulated", () => {
        renderPage();
        fireEvent.change(screen.getByLabelText("Additional storage capacity"), { target: { value: "0" } });
        fireEvent.change(screen.getByLabelText("Additional sensors"), { target: { value: "0" } });

        expect(result("Break-even point")).toBe("No break-even");
        expect(result("Financial outlook")).toBe("Not financially viable");
        expect(document.body.textContent).not.toMatch(/NaN|Infinity/);
    });

    it("shows a temporary on-screen summary and does not download a file", () => {
        vi.useFakeTimers();
        const createObjectURL = vi.fn();
        globalThis.URL.createObjectURL = createObjectURL;
        renderPage();

        fireEvent.click(screen.getByRole("button", { name: "Show summary" }));
        const status = screen.getByRole("status", { name: "Scenario summary" });
        expect(status.textContent).toContain("Investment: 7,750,000 FCFA");
        expect(status.textContent).toContain("Outlook: Profitable within 5 years");
        expect(createObjectURL).not.toHaveBeenCalled();

        act(() => {
            vi.advanceTimersByTime(5600);
        });
        expect(screen.getByRole("status", { name: "Scenario summary" }).textContent).toBe("");
    });
});
