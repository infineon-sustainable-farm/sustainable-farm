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
            {
                id: 2,
                blockCode: "B",
                varieties: ["Keitt"],
                plantingDate: "2020-07-01",
                datePrecision: "day",
                ageYears: 6,
                ageMonths: 4,
                growthPhase: "Full production",
                phaseYearsBand: "6+ yrs",
                localRainfallMm: null,
                source: "user_entry",
            },
        ],
    }),
    useGrowthCalendarBlocks: () => ({ data: ["A"] }),
}));

// The rain of each block comes from Water Supply; block A has a gauge, block B none.
vi.mock("../hooks/useBlockRainfall", () => ({
    useBlockRainfall: () => ({ A: { kind: "value", rainfallMm: 20 }, B: { kind: "unavailable" } }),
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
        // The rain shown is the one the gauges measured over the last 30 days, not the stored value.
        expect(screen.getByRole("columnheader", { name: "Rainfall (30 days)" })).toBeInTheDocument();
        expect(screen.getByRole("cell", { name: "20 mm" })).toBeInTheDocument();
        expect(screen.getByRole("cell", { name: "Rainfall unavailable" })).toBeInTheDocument();
        expect(screen.queryByRole("cell", { name: "1,100 mm" })).not.toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Harvest & Maturity" })).toHaveAttribute("href", "/plants/harvest-maturity");
        expect(screen.getByText(/Planting date not recorded in the reference study/)).toBeInTheDocument();
    });
});
