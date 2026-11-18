// @vitest-environment jsdom
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import RecordPlantingModal from "./RecordPlantingModal";

function renderModal(props = {}) {
    const onSubmit = vi.fn();
    const onClose = vi.fn();
    render(<RecordPlantingModal isSubmitting={false} error={null} onSubmit={onSubmit} onClose={onClose} {...props} />);
    return { onSubmit, onClose };
}

function type(label, value) {
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

describe("the record a planting dialog", () => {
    it("opens as a named dialog, its first field focused", () => {
        renderModal();
        expect(screen.getByRole("dialog", { name: "Record a planting" })).toBeInTheDocument();
        expect(screen.getByLabelText("Planting date")).toHaveFocus();
    });

    it("lists the vigor ratings, Not assessed first", () => {
        renderModal();
        const options = Array.from(screen.getByLabelText("Vigor").querySelectorAll("option")).map((o) => o.textContent);
        expect(options).toEqual(["Not assessed", "Good", "Average", "Poor"]);
    });

    it("refuses an empty form, each message under its field", () => {
        const { onSubmit } = renderModal();
        fireEvent.click(screen.getByRole("button", { name: "Record planting" }));
        expect(onSubmit).not.toHaveBeenCalled();
        expect(screen.getByText("Planting date is required.")).toBeInTheDocument();
        expect(screen.getByText("Block is required.")).toBeInTheDocument();
        expect(screen.getByText("Variety is required.")).toBeInTheDocument();
        expect(screen.getByText("Number of trees must be a whole number of at least 1.")).toBeInTheDocument();
        expect(screen.getByLabelText("Block")).toHaveAttribute("aria-invalid", "true");
    });

    it("refuses a block code with a prefix, and one spacing without the other", () => {
        const { onSubmit } = renderModal();
        type("Planting date", "2019-09-15");
        type("Block", "Block A");
        type("Variety", "Keitt");
        type("Number of trees", "40");
        type("Inter-row spacing (m)", "8");
        fireEvent.click(screen.getByRole("button", { name: "Record planting" }));
        expect(onSubmit).not.toHaveBeenCalled();
        expect(
            screen.getByText("Block must be a short code such as A or B2, without prefix or space.")
        ).toBeInTheDocument();
        expect(
            screen.getByText("Intra-row spacing is required when the inter-row spacing is given.")
        ).toBeInTheDocument();
    });

    it("submits the same body as before", () => {
        const { onSubmit } = renderModal();
        type("Planting date", "2019-09-15");
        type("Block", "A");
        type("Variety", "Keitt");
        type("Number of trees", "40");
        type("Inter-row spacing (m)", "8");
        type("Intra-row spacing (m)", "7.5");
        fireEvent.change(screen.getByLabelText("Vigor"), { target: { value: "GOOD" } });
        fireEvent.click(screen.getByRole("button", { name: "Record planting" }));
        expect(onSubmit).toHaveBeenCalledWith({
            plantingDate: "2019-09-15",
            blockCode: "A",
            varietyName: "Keitt",
            treeCount: 40,
            rowSpacingM: 8,
            treeSpacingM: 7.5,
            vigor: "GOOD",
        });
    });

    it("shows the API's field errors under their fields and other errors above the form", () => {
        const fieldError = { status: 400, message: "Bad request", data: { fieldErrors: { blockCode: "Unknown block." } } };
        const { unmount } = render(
            <RecordPlantingModal isSubmitting={false} error={fieldError} onSubmit={vi.fn()} onClose={vi.fn()} />
        );
        expect(screen.getByText("Unknown block.")).toBeInTheDocument();
        expect(screen.queryByRole("alert")).not.toBeInTheDocument();
        unmount();

        renderModal({ error: { status: 409, message: "Keitt is already planted on block A." } });
        expect(screen.getByRole("alert")).toHaveTextContent("Keitt is already planted on block A.");
    });

    it("closes with Cancel and with Escape", () => {
        const { onClose } = renderModal();
        fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
        fireEvent.keyDown(document, { key: "Escape" });
        expect(onClose).toHaveBeenCalledTimes(2);
    });
});
