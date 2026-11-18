// @vitest-environment jsdom
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import GrowthCalendarPage from "./GrowthCalendarPage";

vi.mock("../hooks/useGrowthCalendar", () => ({
    useGrowthCalendar: () => ({
        isPending: false,
        isError: false,
        data: [
            {
                id: 1,
                blockCode: "A",
                varieties: ["Kent"],
                plantingDate: null,
                datePrecision: "season",
                ageYears: null,
                ageMonths: null,
                growthPhase: null,
                phaseYearsBand: null,
                localRainfallMm: 1100,
                source: "Zalka_2025",
            },
        ],
    }),
    useGrowthCalendarBlocks: () => ({ data: ["A"] }),
}));

describe("the growth calendar screen", () => {
    it("lists the plantings with the legend and the note on missing planting dates", () => {
        render(
            <MemoryRouter>
                <GrowthCalendarPage />
            </MemoryRouter>
        );
        expect(screen.getByRole("heading", { level: 1, name: "Planting Calendar & Growth Stages" })).toBeInTheDocument();
        expect(screen.getByRole("columnheader", { name: "Planting Date" })).toBeInTheDocument();
        expect(screen.getByRole("cell", { name: "1,100 mm" })).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Harvest & Maturity" })).toHaveAttribute("href", "/plants/harvest-maturity");
        expect(screen.getByText(/Planting date not recorded in the reference study/)).toBeInTheDocument();
    });
});
