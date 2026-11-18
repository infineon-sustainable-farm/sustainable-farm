// @vitest-environment jsdom
import { fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import VisitorsPage from "./VisitorsPage";

const createMutate = vi.fn();
const deleteMutate = vi.fn();

function mutation(mutate) {
    return { mutate, reset: vi.fn(), isPending: false, isError: false, error: null, variables: undefined };
}

vi.mock("../../hooks/useVisitors", () => ({
    useVisitors: () => ({
        data: [
            { id: 1, fullName: "Visitor One", type: "GROUP", groupSize: 3, email: null, phone: null, language: "Moore" },
        ],
        isPending: false,
        isError: false,
        refetch: vi.fn(),
        isFetching: false,
    }),
    useCreateVisitor: () => mutation(createMutate),
    useUpdateVisitor: () => mutation(vi.fn()),
    useDeleteVisitor: () => mutation(deleteMutate),
}));

describe("VisitorsPage", () => {
    beforeEach(() => {
        createMutate.mockReset();
        deleteMutate.mockReset();
    });

    it("lists the visitors with translated enum values", () => {
        render(<VisitorsPage />);
        expect(screen.getByRole("heading", { level: 1, name: "Visitors" })).toBeInTheDocument();
        const row = screen.getByRole("row", { name: /Visitor One/ });
        expect(within(row).getByText("Group")).toBeInTheDocument();
        expect(within(row).getByText("Moore")).toBeInTheDocument();
        expect(document.title).toBe("Visitors — Visitor Management");
    });

    it("opens the new visitor dialog, validates and creates the visitor", () => {
        render(<VisitorsPage />);
        fireEvent.click(screen.getByRole("button", { name: "New visitor" }));
        const dialog = screen.getByRole("dialog", { name: "New visitor" });

        fireEvent.click(within(dialog).getByRole("button", { name: "Create visitor" }));
        expect(within(dialog).getByText("Full name is required.")).toBeInTheDocument();
        expect(createMutate).not.toHaveBeenCalled();

        fireEvent.change(within(dialog).getByLabelText("Full name"), { target: { value: " New Person " } });
        fireEvent.change(within(dialog).getByLabelText("Group size"), { target: { value: "2" } });
        fireEvent.click(within(dialog).getByRole("button", { name: "Create visitor" }));
        expect(createMutate).toHaveBeenCalledWith(
            {
                fullName: "New Person",
                groupSize: 2,
                email: null,
                phone: null,
                language: "French",
                type: "INDIVIDUAL",
                specialNeeds: null,
            },
            expect.any(Object),
        );
    });

    it("asks for a confirmation before deleting a visitor", () => {
        render(<VisitorsPage />);
        fireEvent.click(screen.getByRole("button", { name: "delete" }));
        const dialog = screen.getByRole("dialog", { name: "Delete?" });
        expect(within(dialog).getByText("Visitor One will be deleted from the directory.")).toBeInTheDocument();
        fireEvent.click(within(dialog).getByRole("button", { name: "Yes" }));
        expect(deleteMutate).toHaveBeenCalledWith(1, expect.any(Object));
    });
});
