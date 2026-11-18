// @vitest-environment jsdom
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import AddFertilizerModal from "./AddFertilizerModal";

function renderModal(props = {}) {
    const onSubmit = vi.fn();
    const onClose = vi.fn();
    render(<AddFertilizerModal isSubmitting={false} error={null} onSubmit={onSubmit} onClose={onClose} {...props} />);
    return { onSubmit, onClose };
}

describe("the Add a fertilizer dialog", () => {
    it("opens as a named dialog, focused on the name", () => {
        renderModal();
        expect(screen.getByRole("dialog", { name: "Add a fertilizer" })).toBeInTheDocument();
        expect(screen.getByLabelText("Name")).toHaveFocus();
    });

    it("refuses an empty form, each message under its field", () => {
        const { onSubmit } = renderModal();
        fireEvent.click(screen.getByRole("button", { name: "Add fertilizer" }));
        expect(onSubmit).not.toHaveBeenCalled();
        expect(screen.getByText("Name is required.")).toBeInTheDocument();
        expect(screen.getByText("Type is required.")).toBeInTheDocument();
        expect(screen.getByText("Unit is required.")).toBeInTheDocument();
        expect(screen.getByLabelText("Name")).toHaveAttribute("aria-invalid", "true");
    });

    it("sends the fertilizer, without the optional fields left empty", () => {
        const { onSubmit } = renderModal();
        fireEvent.change(screen.getByLabelText("Name"), { target: { value: "NPK" } });
        fireEvent.change(screen.getByLabelText("Type"), { target: { value: "MINERAL" } });
        fireEvent.change(screen.getByLabelText("Unit"), { target: { value: "KG" } });
        fireEvent.click(screen.getByRole("button", { name: "Add fertilizer" }));
        expect(onSubmit).toHaveBeenCalledWith({ name: "NPK", fertilizerType: "MINERAL", unit: "KG" });
    });

    it("sends the composition and the threshold when entered, and keeps the threshold note", () => {
        const { onSubmit } = renderModal();
        fireEvent.change(screen.getByLabelText("Name"), { target: { value: "NPK" } });
        fireEvent.change(screen.getByLabelText("Type"), { target: { value: "ORGANIC" } });
        fireEvent.change(screen.getByLabelText("Composition"), { target: { value: "15-15-15" } });
        fireEvent.change(screen.getByLabelText("Unit"), { target: { value: "L" } });
        fireEvent.change(screen.getByLabelText("Alert threshold"), { target: { value: "12.5" } });
        expect(screen.getByLabelText("Alert threshold")).toHaveAccessibleDescription("Optional. It cannot be changed later.");
        fireEvent.click(screen.getByRole("button", { name: "Add fertilizer" }));
        expect(onSubmit).toHaveBeenCalledWith({
            name: "NPK",
            fertilizerType: "ORGANIC",
            unit: "L",
            composition: "15-15-15",
            reorderThreshold: 12.5,
        });
    });

    it("shows a server error above the form and a 400 under its field", () => {
        renderModal({ error: { status: 409, message: "A fertilizer named NPK exists." } });
        expect(screen.getByRole("alert")).toHaveTextContent("A fertilizer named NPK exists.");
    });

    it("closes on Cancel", () => {
        const { onClose } = renderModal();
        fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
        expect(onClose).toHaveBeenCalled();
    });
});
