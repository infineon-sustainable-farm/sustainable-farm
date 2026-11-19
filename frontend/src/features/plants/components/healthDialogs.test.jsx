// @vitest-environment jsdom
import { fireEvent, render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import AddHealthIssueModal from "./AddHealthIssueModal";
import RecordInspectionModal from "./RecordInspectionModal";
import ResolveFindingModal from "./ResolveFindingModal";
import TreatmentModal from "./TreatmentModal";

// The catalogue of the forms, without a request: one pest and one disease.
vi.mock("../hooks/useHealthIssues", () => ({
    useHealthIssues: () => ({
        isPending: false,
        isError: false,
        data: [
            { id: 1, code: "FRUIT_FLY", name: "Fruit flies", kind: "PEST" },
            { id: 5, code: "ANTHRACNOSE", name: "Anthracnose", kind: "DISEASE" },
        ],
    }),
}));

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

const FINDING = {
    id: 7,
    blockCode: "A",
    treeLabel: "12",
    issueName: "Anthracnose",
    otherLabel: null,
    inspectedOn: "2026-01-10",
    lastTreatedOn: null,
};

function change(label, value) {
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

function submit(name) {
    fireEvent.click(screen.getByRole("button", { name }));
}

describe("the dialog adding a pest or disease", () => {
    it("opens as a named dialog and checks the required fields", () => {
        const onSubmit = vi.fn();
        render(<AddHealthIssueModal isSubmitting={false} error={null} onSubmit={onSubmit} onClose={vi.fn()} />);

        expect(screen.getByRole("dialog", { name: "Add a pest or disease" })).toBeInTheDocument();
        submit("Add to catalogue");

        expect(screen.getByText("Name is required.")).toBeInTheDocument();
        expect(screen.getByText("Kind is required.")).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("sends the name and kind, without the optional fields left empty", () => {
        const onSubmit = vi.fn();
        render(<AddHealthIssueModal isSubmitting={false} error={null} onSubmit={onSubmit} onClose={vi.fn()} />);

        change("Name", "Powdery mildew");
        change("Kind", "DISEASE");
        submit("Add to catalogue");

        expect(onSubmit).toHaveBeenCalledWith({ name: "Powdery mildew", kind: "DISEASE" });
    });

    it("shows a name conflict of the API under the name", () => {
        const error = { status: 409, message: "A health issue with code POWDERY_MILDEW already exists." };
        render(<AddHealthIssueModal isSubmitting={false} error={error} onSubmit={vi.fn()} onClose={vi.fn()} />);

        expect(screen.getByLabelText("Name")).toHaveAttribute("aria-invalid", "true");
        expect(screen.getByText(error.message)).toBeInTheDocument();
    });

    it("closes with Cancel", () => {
        const onClose = vi.fn();
        render(<AddHealthIssueModal isSubmitting={false} error={null} onSubmit={vi.fn()} onClose={onClose} />);

        submit("Cancel");

        expect(onClose).toHaveBeenCalled();
    });
});

describe("the dialog resolving a finding", () => {
    it("recalls the finding and refuses a date before its inspection", () => {
        const onSubmit = vi.fn();
        render(<ResolveFindingModal finding={FINDING} isSubmitting={false} error={null} onSubmit={onSubmit} onClose={vi.fn()} />);

        const dialog = screen.getByRole("dialog", { name: "Resolve a finding" });
        expect(within(dialog).getByText("Block A, tree 12")).toBeInTheDocument();
        expect(within(dialog).getByText("Block A, tree 12").parentElement).toHaveTextContent(
            "Block A, tree 12 · Anthracnose · seen on 10 Jan 2026"
        );

        change("Resolution date", "2026-01-09");
        submit("Resolve finding");

        expect(screen.getByText("Resolution date cannot be before the inspection, on 10 Jan 2026.")).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("sends the date, and the note only when one is typed", () => {
        const onSubmit = vi.fn();
        render(<ResolveFindingModal finding={FINDING} isSubmitting={false} error={null} onSubmit={onSubmit} onClose={vi.fn()} />);

        change("Resolution date", "2026-01-12");
        submit("Resolve finding");

        expect(onSubmit).toHaveBeenCalledWith({ resolvedOn: "2026-01-12" });
    });
});

describe("the dialog recording an inspection", () => {
    function renderForm(onSubmit = vi.fn()) {
        render(
            <RecordInspectionModal
                isSubmitting={false}
                error={null}
                onSubmit={onSubmit}
                onResetError={vi.fn()}
                onClose={vi.fn()}
            />,
            { wrapper: MemoryRouter }
        );
        return onSubmit;
    }

    it("checks the block, the date and the score", () => {
        const onSubmit = renderForm();

        expect(screen.getByRole("dialog", { name: "Record an inspection" })).toBeInTheDocument();
        submit("Record inspection");

        expect(screen.getByText("Block is required.")).toBeInTheDocument();
        expect(screen.getByText("Inspection date is required.")).toBeInTheDocument();
        expect(screen.getByText("Health score is required.")).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("sends the inspection with a finding named Other, the catalogue grouped by kind", () => {
        const onSubmit = renderForm();

        change("Block", "A");
        change("Inspection date", "2026-01-10");
        change("Health score (%)", "80");
        change("Method", "VISUAL");
        submit("Add a finding");

        const problem = screen.getByLabelText("Problem");
        expect(within(problem).getByRole("group", { name: "Pests" })).toBeInTheDocument();
        expect(within(problem).getByRole("group", { name: "Diseases" })).toBeInTheDocument();
        fireEvent.change(problem, { target: { value: "OTHER" } });
        change("Name of the problem", "Sunburn");
        change("Tree", "R3-12");
        submit("Record inspection");

        expect(onSubmit).toHaveBeenCalledWith({
            blockCode: "A",
            inspectedOn: "2026-01-10",
            healthScorePct: 80,
            method: "VISUAL",
            findings: [{ issueCode: "OTHER", otherLabel: "Sunburn", treeLabel: "R3-12" }],
        });
    });

    it("removes a finding by its numbered button", () => {
        renderForm();

        submit("Add a finding");
        expect(screen.getByRole("group", { name: "Finding 1" })).toBeInTheDocument();
        submit("Remove finding 1");

        expect(screen.queryByRole("group", { name: "Finding 1" })).not.toBeInTheDocument();
    });
});

describe("the dialog recording a treatment", () => {
    function fillCommonFields() {
        change("Treatment date", "2026-01-11");
        change("Pre-harvest interval (days)", "14");
        change("Product", "Copper");
        change("Active ingredient", "Copper hydroxide");
        change("Quantity", "2.5");
        change("Unit", "KG");
        change("Applicator", "Field team");
    }

    it("treats a finding: the block and the problem come from the finding", () => {
        const onSubmit = vi.fn();
        render(<TreatmentModal finding={FINDING} isSubmitting={false} error={null} onSubmit={onSubmit} onClose={vi.fn()} />, {
            wrapper: MemoryRouter,
        });

        expect(screen.getByRole("dialog", { name: "Treat a finding" })).toBeInTheDocument();
        expect(screen.queryByLabelText("Block")).not.toBeInTheDocument();
        fillCommonFields();
        submit("Record treatment");

        expect(onSubmit).toHaveBeenCalledWith({
            treatedOn: "2026-01-11",
            productName: "Copper",
            activeIngredient: "Copper hydroxide",
            quantity: 2.5,
            unit: "KG",
            preHarvestIntervalDays: 14,
            applicator: "Field team",
        });
    });

    it("asks a preventive treatment for its block and problem", () => {
        const onSubmit = vi.fn();
        render(<TreatmentModal finding={null} isSubmitting={false} error={null} onSubmit={onSubmit} onClose={vi.fn()} />, {
            wrapper: MemoryRouter,
        });

        expect(screen.getByRole("dialog", { name: "Record a preventive treatment" })).toBeInTheDocument();
        submit("Record treatment");
        expect(screen.getByText("Block is required.")).toBeInTheDocument();
        expect(screen.getByText("Problem targeted is required.")).toBeInTheDocument();
        expect(screen.getByText("Product is required.")).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();

        change("Block", "A");
        change("Problem targeted", "FRUIT_FLY");
        fillCommonFields();
        submit("Record treatment");

        expect(onSubmit).toHaveBeenCalledWith(
            expect.objectContaining({ blockCode: "A", targetIssueCode: "FRUIT_FLY", productName: "Copper" })
        );
    });
});
