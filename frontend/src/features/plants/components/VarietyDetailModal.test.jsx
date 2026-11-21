// @vitest-environment jsdom
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import VarietyDetailModal from "./VarietyDetailModal";

const VARIETY = {
    id: 1,
    name: "Keitt",
    blockCode: "A",
    treeCount: 40,
    currentTreeCount: 38,
    rowSpacingM: 8,
    treeSpacingM: 7.5,
    treeDensityPerHa: 167,
    vigor: "GOOD",
    plantOrigin: null,
    farmId: 1,
    source: "Field survey",
    lastUpdated: null,
};

describe("the variety detail dialog", () => {
    it("is named by the variety, with its block and every detail", () => {
        render(<VarietyDetailModal variety={VARIETY} onClose={vi.fn()} />);
        const dialog = screen.getByRole("dialog", { name: "Keitt" });
        expect(dialog).toHaveAccessibleDescription("Block A");
        expect(screen.getByText("Trees planted").nextSibling).toHaveTextContent("40");
        expect(screen.getByText("Trees now").nextSibling).toHaveTextContent("38");
        expect(screen.getByText("Spacing").nextSibling).toHaveTextContent("8 × 7.5 m");
        expect(screen.getByText("Tree density").nextSibling).toHaveTextContent("167 trees/ha");
        expect(screen.getByText("Vigor").nextSibling).toHaveTextContent("Good");
        expect(screen.getByText("Plant origin").nextSibling).toHaveTextContent("—");
        expect(screen.getByText("A dash means no value has been recorded for this field yet.")).toBeInTheDocument();
    });

    it("closes with its Close button", () => {
        const onClose = vi.fn();
        render(<VarietyDetailModal variety={VARIETY} onClose={onClose} />);
        fireEvent.click(screen.getByRole("button", { name: "Close" }));
        expect(onClose).toHaveBeenCalled();
    });
});
