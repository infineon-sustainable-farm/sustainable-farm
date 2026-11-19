// @vitest-environment jsdom
import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import ScenarioFormModal from "./ScenarioFormModal";

vi.mock("../hooks/useVarietyReferences", () => ({
    useVarietyReferences: () => ({
        data: [{ id: 1, varietyName: "Kent", yieldPerTreeKg: 220, yieldSource: "Zalka_2025" }],
    }),
}));

function renderForm(props = {}) {
    const onSubmit = vi.fn();
    const onClose = vi.fn();
    render(
        <ScenarioFormModal scenario={null} isSubmitting={false} error={null} onSubmit={onSubmit} onClose={onClose} {...props} />
    );
    return { onSubmit, onClose, dialog: screen.getByRole("dialog", { name: "Enter a scenario" }) };
}

function type(label, value) {
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

describe("the scenario form", () => {
    it("opens as a named dialog and closes on Cancel", () => {
        const { dialog, onClose } = renderForm();
        fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
        expect(onClose).toHaveBeenCalled();
    });

    it("refuses an empty form with the API's messages, and sends nothing", () => {
        const { onSubmit } = renderForm();
        fireEvent.click(screen.getByRole("button", { name: "Save scenario" }));
        expect(onSubmit).not.toHaveBeenCalled();
        expect(screen.getByText("Name is required.")).toBeInTheDocument();
        expect(screen.getByText("Currency is required.")).toBeInTheDocument();
        expect(screen.getByText("Investment is required.")).toBeInTheDocument();
        expect(screen.getByLabelText("Name")).toHaveAttribute("aria-invalid", "true");
    });

    it("offers the reference yield of the variety and sends the same body as before", () => {
        const { onSubmit } = renderForm();
        type("Name", "Plan A");
        type("Variety (optional)", "kent");
        fireEvent.click(screen.getByRole("button", { name: "Use this value" }));
        fireEvent.click(screen.getByLabelText("EUR (€)"));
        type("Trees", "200");
        type("Fixed operating cost (per year)", "1500");
        type("Investment", "12000");
        fireEvent.click(screen.getByRole("button", { name: "Save scenario" }));
        expect(onSubmit).toHaveBeenCalledWith({
            name: "Plan A",
            currency: "EUR",
            varietyName: "kent",
            treeCount: 200,
            yieldPerTreeKg: 220,
            yieldSource: "Zalka_2025",
            annualFixedCost: 1500,
            initialInvestment: 12000,
        });
    });

    it("shows a server error above the form, and the API's field errors under their field", () => {
        renderForm({ error: { status: 409, message: "A scenario named Plan A already exists." } });
        expect(screen.getByRole("alert")).toHaveTextContent("A scenario named Plan A already exists.");
    });

    it("titles a correction with the name of the scenario", () => {
        render(
            <ScenarioFormModal
                scenario={{ id: 4, name: "Plan B", currency: "XOF", treeCount: 10, treeCountSource: "farm plan" }}
                isSubmitting={false}
                error={{ status: 400, data: { fieldErrors: { treeCount: "Too many trees." } } }}
                onSubmit={vi.fn()}
                onClose={vi.fn()}
            />
        );
        expect(screen.getByRole("dialog", { name: "Edit Plan B" })).toBeInTheDocument();
        expect(screen.getByText("Too many trees.")).toBeInTheDocument();
        expect(screen.getByText("Recorded: farm plan")).toBeInTheDocument();
    });
});
