// @vitest-environment jsdom
import { fireEvent, render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import AddNurseryBatchModal from "./AddNurseryBatchModal";
import RecordNurseryEventModal from "./RecordNurseryEventModal";

vi.mock("../hooks/useFarmBlocks", () => ({
    useFarmBlocks: () => ({
        data: [
            { id: 1, farmId: 1, code: "A", name: "North block" },
            { id: 2, farmId: 1, code: "B2", name: "B2" },
            { id: 3, farmId: 1, code: "E", name: "East" },
        ],
        isPending: false,
        isError: false,
        refetch: () => {},
    }),
}));

vi.mock("../hooks/useVarietyReferences", () => ({
    useVarietyReferences: () => ({ data: [{ id: 1, varietyName: "Keitt" }] }),
}));
vi.mock("../hooks/useNurseryBatches", () => ({
    useNurseryBatches: () => ({
        isPending: false,
        isError: false,
        isSuccess: true,
        data: [
            {
                id: 7,
                batchCode: "P1",
                varietyName: "Keitt",
                currentStage: "GRAFTED",
                currentStageSince: "2026-10-01",
                currentCount: 120,
                plannedBlockCode: "E",
            },
        ],
    }),
}));

function change(label, value) {
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

function renderAdd(props = {}) {
    const onSubmit = vi.fn();
    const onClose = vi.fn();
    render(
        <AddNurseryBatchModal isSubmitting={false} error={null} onSubmit={onSubmit} onResetError={vi.fn()} onClose={onClose} {...props} />,
        { wrapper: MemoryRouter }
    );
    return { onSubmit, onClose };
}

function renderEvent(props = {}) {
    const onSubmit = vi.fn();
    render(
        <RecordNurseryEventModal isSubmitting={false} error={null} onSubmit={onSubmit} onResetError={vi.fn()} onClose={vi.fn()} {...props} />,
        { wrapper: MemoryRouter }
    );
    return { onSubmit };
}

describe("the add a nursery batch dialog", () => {
    it("opens as a named dialog and closes on Cancel", () => {
        const { onClose } = renderAdd();
        const dialog = screen.getByRole("dialog", { name: "Add a nursery batch" });
        fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
        expect(onClose).toHaveBeenCalled();
    });

    it("shows the errors of the empty form under their fields and sends nothing", () => {
        const { onSubmit } = renderAdd();
        fireEvent.click(screen.getByRole("button", { name: "Add batch" }));
        expect(screen.getByText("Batch code is required.")).toBeInTheDocument();
        expect(screen.getByText("Variety is required.")).toBeInTheDocument();
        expect(screen.getByText("Origin is required.")).toBeInTheDocument();
        expect(screen.getByText("Start date is required.")).toBeInTheDocument();
        expect(screen.getByText("Initial count must be a whole number, 1 or more.")).toBeInTheDocument();
        expect(screen.getByText("Initial stage is required.")).toBeInTheDocument();
        expect(screen.getByText("Planned transplant date is required.")).toBeInTheDocument();
        expect(screen.getByLabelText("Batch code")).toHaveAttribute("aria-invalid", "true");
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("asks for the supplier of a purchase and sends the same body as before", () => {
        const { onSubmit } = renderAdd();
        change("Batch code", "p1");
        change("Variety", "Keitt");
        fireEvent.click(screen.getByLabelText("External purchase"));
        fireEvent.click(screen.getByRole("button", { name: "Add batch" }));
        expect(screen.getByText("Supplier is required for an external purchase.")).toBeInTheDocument();

        change("Supplier", "Pépinière du Sud");
        change("Supplier lot number", "L-12");
        change("Start date", "2026-01-10");
        change("Initial count", "150");
        change("Initial stage", "GERMINATION");
        change("Planned transplant date", "2026-12-01");
        change("Planned block", "E");
        fireEvent.click(screen.getByRole("button", { name: "Add batch" }));
        expect(onSubmit).toHaveBeenCalledWith({
            batchCode: "p1",
            varietyName: "Keitt",
            origin: "PURCHASED",
            supplier: "Pépinière du Sud",
            supplierLotNumber: "L-12",
            startedOn: "2026-01-10",
            initialCount: 150,
            initialStage: "GERMINATION",
            plannedTransplantOn: "2026-12-01",
            plannedBlockCode: "E",
        });
    });

    it("shows a 409 under the batch code and any other error above the form", () => {
        renderAdd({ error: { status: 409, message: "Batch code P1 is taken." } });
        expect(screen.getByLabelText("Batch code")).toHaveAccessibleDescription(
            expect.stringContaining("Batch code P1 is taken.")
        );
    });

    it("shows a server error above the form as an alert", () => {
        renderAdd({ error: { status: 422, message: "Planned date is before the start date." } });
        expect(screen.getByRole("alert")).toHaveTextContent("Planned date is before the start date.");
    });
});

describe("the record a nursery event dialog", () => {
    it("opens on a stage change, with the current stage of the chosen batch", () => {
        renderEvent();
        expect(screen.getByRole("dialog", { name: "Record a nursery event" })).toBeInTheDocument();
        expect(screen.getByLabelText("Stage change")).toBeChecked();
        change("Batch", "7");
        expect(screen.getByText("Current stage: Grafted, since 1 Oct 2026.")).toBeInTheDocument();
    });

    it("validates the fields of the chosen event", () => {
        const { onSubmit } = renderEvent();
        fireEvent.click(screen.getByLabelText("Loss"));
        fireEvent.click(screen.getByRole("button", { name: "Record loss" }));
        expect(screen.getByText("Batch is required.")).toBeInTheDocument();
        expect(screen.getByText("Date is required.")).toBeInTheDocument();
        expect(screen.getByText("Quantity must be a whole number, 1 or more.")).toBeInTheDocument();
        expect(screen.getByText("Reason is required.")).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("sends a transplant with its block and date under the route's name", () => {
        const { onSubmit } = renderEvent();
        fireEvent.click(screen.getByLabelText("Transplant"));
        change("Batch", "7");
        change("Date", "2026-11-01");
        change("Quantity", "40");
        change("Block", "E");
        expect(screen.getByText(/The transplant also records the planting of Keitt on this block/)).toBeInTheDocument();
        fireEvent.click(screen.getByRole("button", { name: "Record transplant" }));
        expect(onSubmit).toHaveBeenCalledWith({
            eventType: "TRANSPLANT",
            batchId: 7,
            body: { transplantedOn: "2026-11-01", quantity: 40, blockCode: "E" },
        });
    });
});
