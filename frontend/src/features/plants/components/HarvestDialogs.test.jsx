// @vitest-environment jsdom
import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import RecordHarvestModal from "./RecordHarvestModal";
import RecordMaturityObservationModal from "./RecordMaturityObservationModal";
import VarietyReferenceModal from "./VarietyReferenceModal";

vi.mock("../../../shared/api/persons", () => ({
    fetchPersons: vi.fn(() => Promise.resolve([{ id: 4, fullName: "Field team" }])),
}));

// Block A has one planted variety (a current tree count) and one without a planting.
vi.mock("../hooks/useVarieties", () => ({
    useVarieties: ({ blockCode }) => ({
        data: blockCode
            ? [
                  { id: 1, name: "Kéitt", currentTreeCount: 120 },
                  { id: 2, name: "Kent", currentTreeCount: null },
              ]
            : undefined,
        isSuccess: Boolean(blockCode),
        isError: false,
    }),
}));

function change(label, value) {
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

describe("the record a harvest dialog", () => {
    it("opens as a named dialog and refuses an empty form, field by field", () => {
        const onSubmit = vi.fn();
        render(<RecordHarvestModal blocks={["A"]} isSubmitting={false} error={null} onSubmit={onSubmit} onClose={vi.fn()} />);
        const dialog = screen.getByRole("dialog", { name: "Record a harvest" });
        fireEvent.click(within(dialog).getByRole("button", { name: "Record harvest" }));
        expect(screen.getByText("Harvest date is required.")).toBeInTheDocument();
        expect(screen.getByText("Block is required.")).toBeInTheDocument();
        expect(screen.getByText("Variety is required.")).toBeInTheDocument();
        expect(screen.getByText("Quantity must be a number greater than 0.")).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("offers the planted varieties of the block only, and sends the same request", () => {
        const onSubmit = vi.fn();
        render(<RecordHarvestModal blocks={["A"]} isSubmitting={false} error={null} onSubmit={onSubmit} onClose={vi.fn()} />);
        change("Harvest date", "2026-01-15");
        change("Block", "A");
        const variety = screen.getByLabelText("Variety");
        expect(within(variety).getAllByRole("option").map((option) => option.textContent)).toEqual([
            "Select a variety",
            "Kéitt",
        ]);
        change("Variety", "Kéitt");
        change("Quantity (kg)", "250.5");
        fireEvent.click(screen.getByRole("button", { name: "Record harvest" }));
        expect(onSubmit).toHaveBeenCalledWith({
            harvestDate: "2026-01-15",
            blockCode: "A",
            varietyName: "Kéitt",
            quantityKg: 250.5,
        });
    });

    it("shows a server error above the form and a field error under its field", () => {
        const { rerender } = render(
            <RecordHarvestModal blocks={["A"]} isSubmitting={false} error={{ status: 422, message: "No planting." }}
                onSubmit={vi.fn()} onClose={vi.fn()} />
        );
        expect(screen.getByRole("alert")).toHaveTextContent("No planting.");
        rerender(
            <RecordHarvestModal blocks={["A"]} isSubmitting={false}
                error={{ status: 400, data: { fieldErrors: { quantityKg: "Too much." } } }}
                onSubmit={vi.fn()} onClose={vi.fn()} />
        );
        expect(screen.getByLabelText("Quantity (kg)")).toHaveAccessibleDescription("Too much.");
    });

    it("closes on Cancel", () => {
        const onClose = vi.fn();
        render(<RecordHarvestModal blocks={["A"]} isSubmitting={false} error={null} onSubmit={vi.fn()} onClose={onClose} />);
        fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
        expect(onClose).toHaveBeenCalled();
    });
});

describe("the maturity observation dialog", () => {
    function renderDialog(onSubmit = vi.fn()) {
        render(
            <RecordMaturityObservationModal blocks={["A"]} isSubmitting={false} error={null} onSubmit={onSubmit}
                onClose={vi.fn()} />
        );
        return onSubmit;
    }

    it("refuses an empty form and a measured value out of range", () => {
        const onSubmit = renderDialog();
        expect(screen.getByRole("dialog", { name: "Record a maturity observation" })).toBeInTheDocument();
        change("Method", "BRIX");
        change("Measured value (°Bx)", "150");
        fireEvent.click(screen.getByRole("button", { name: "Record observation" }));
        expect(screen.getByText("Observation date is required.")).toBeInTheDocument();
        expect(screen.getByText("Maturity status is required.")).toBeInTheDocument();
        expect(
            screen.getByText("Measured value must be above 0 and at most 100, with one decimal at most.")
        ).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("lists the statuses and methods in words, and sends the same request", async () => {
        const onSubmit = renderDialog();
        expect(within(screen.getByLabelText("Maturity status")).getAllByRole("option").map((o) => o.textContent))
            .toEqual(["Select a status", "Immature", "Ripe", "Ready for harvest"]);
        change("Observation date", "2026-01-10");
        change("Block", "A");
        change("Variety", "Kéitt");
        change("Maturity status", "RIPE");
        change("Method", "BRIX");
        change("Measured value (°Bx)", "14.2");
        await screen.findByRole("option", { name: "Field team" });
        change("Observer", "4");
        fireEvent.click(screen.getByRole("button", { name: "Record observation" }));
        expect(onSubmit).toHaveBeenCalledWith({
            observedOn: "2026-01-10",
            blockCode: "A",
            varietyName: "Kéitt",
            maturityStatus: "RIPE",
            method: "BRIX",
            measuredValue: 14.2,
            observerPersonId: 4,
        });
    });
});

describe("the variety reference dialog", () => {
    it("adds a variety: refuses an empty form, then sends the values without empty sources", () => {
        const onSubmit = vi.fn();
        render(<VarietyReferenceModal reference={null} isSubmitting={false} error={null} onSubmit={onSubmit}
            onClose={vi.fn()} />);
        expect(screen.getByRole("dialog", { name: "Add a variety" })).toBeInTheDocument();
        fireEvent.click(screen.getByRole("button", { name: "Add variety" }));
        expect(screen.getByText("Variety is required.")).toBeInTheDocument();
        expect(screen.getByText("Yield per tree is required.")).toBeInTheDocument();
        expect(screen.getByText("First harvest month is required.")).toBeInTheDocument();
        expect(screen.getByText("Last harvest month is required.")).toBeInTheDocument();

        change("Variety", "Amélie");
        change("Yield per tree (kg)", "80.5");
        change("First harvest month", "5");
        change("Last harvest month", "7");
        fireEvent.click(screen.getByRole("button", { name: "Add variety" }));
        expect(onSubmit).toHaveBeenCalledWith({
            varietyName: "Amélie",
            yieldPerTreeKg: 80.5,
            harvestStartMonth: 5,
            harvestEndMonth: 7,
        });
    });

    it("edits a reference: prefilled, the recorded source offered, the name conflict under the name", () => {
        const reference = { id: 4, varietyName: "Kent", yieldPerTreeKg: 90, harvestStartMonth: 11, harvestEndMonth: 2,
            yieldSource: "trial_2024", seasonSource: null };
        render(<VarietyReferenceModal reference={reference} isSubmitting={false}
            error={{ status: 409, message: "This variety already has a reference." }} onSubmit={vi.fn()}
            onClose={vi.fn()} />);
        expect(screen.getByRole("dialog", { name: "Edit Kent" })).toBeInTheDocument();
        expect(screen.getByLabelText("Yield per tree (kg)")).toHaveValue(90);
        expect(screen.getByLabelText("Variety")).toHaveAccessibleDescription("This variety already has a reference.");
        expect(screen.getByLabelText("Yield source")).toHaveAccessibleDescription(
            "Leave empty to keep “trial_2024” if the yield does not change; otherwise user_entry is recorded."
        );
        expect(screen.getByRole("button", { name: "Save" })).toBeInTheDocument();
    });
});
