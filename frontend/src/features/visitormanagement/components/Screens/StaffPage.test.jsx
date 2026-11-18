// @vitest-environment jsdom
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import StaffPage from "./StaffPage";
import { createStaff, deactivateStaff, fetchStaff } from "../../api/visitormanagementApi";

vi.mock("../../api/visitormanagementApi", () => ({
    fetchStaff: vi.fn(),
    createStaff: vi.fn(),
    updateStaff: vi.fn(),
    deactivateStaff: vi.fn(),
}));

const MEMBER = { id: 7, fullName: "Guide One", role: "GUIDE", email: null, phone: null, active: true };

function renderPage() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={client}>
            <StaffPage />
        </QueryClientProvider>,
    );
}

describe("StaffPage", () => {
    beforeEach(() => {
        vi.mocked(fetchStaff).mockResolvedValue([MEMBER]);
        vi.mocked(createStaff).mockResolvedValue({ id: 8 });
        vi.mocked(deactivateStaff).mockResolvedValue(undefined);
    });

    it("validates the new staff member dialog and creates the member", async () => {
        renderPage();
        expect(await screen.findByText("Guide One")).toBeInTheDocument();

        fireEvent.click(screen.getByRole("button", { name: "New staff member" }));
        const dialog = screen.getByRole("dialog", { name: "New staff member" });
        fireEvent.click(within(dialog).getByRole("button", { name: "Create staff member" }));
        expect(within(dialog).getByText("Full name is required.")).toBeInTheDocument();
        expect(createStaff).not.toHaveBeenCalled();

        fireEvent.change(within(dialog).getByLabelText("Full name"), { target: { value: "  New Guide " } });
        fireEvent.change(within(dialog).getByLabelText("Role"), { target: { value: "RECEPTION" } });
        fireEvent.click(within(dialog).getByRole("button", { name: "Create staff member" }));

        await waitFor(() =>
            expect(vi.mocked(createStaff).mock.calls[0]?.[0]).toEqual({
                fullName: "New Guide",
                role: "RECEPTION",
                email: null,
                phone: null,
                active: true,
            }),
        );
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    });

    it("asks for confirmation before deactivating a member", async () => {
        renderPage();
        fireEvent.click(await screen.findByRole("button", { name: "deactivate" }));

        const dialog = screen.getByRole("dialog", { name: "Deactivate?" });
        fireEvent.click(within(dialog).getByRole("button", { name: "No" }));
        expect(deactivateStaff).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole("button", { name: "deactivate" }));
        fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Yes" }));
        await waitFor(() => expect(vi.mocked(deactivateStaff).mock.calls[0]?.[0]).toEqual(7));
    });
});
