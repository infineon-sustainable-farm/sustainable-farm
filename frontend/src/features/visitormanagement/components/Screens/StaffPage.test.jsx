// @vitest-environment jsdom
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import StaffPage from "./StaffPage";
import { LocationProbe } from "../../../../test/LocationProbe";
import { createStaff, deactivateStaff, fetchStaff } from "../../api/visitormanagementApi";

vi.mock("../../api/visitormanagementApi", () => ({
    fetchStaff: vi.fn(),
    createStaff: vi.fn(),
    updateStaff: vi.fn(),
    deactivateStaff: vi.fn(),
}));

const MEMBER = { id: 7, fullName: "Guide One", role: "GUIDE", email: null, phone: null, active: true };

function renderPage(url = "/visitormanagement/staff") {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={[url]}>
                <StaffPage />
                <LocationProbe />
            </MemoryRouter>
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

        fireEvent.click(screen.getByRole("button", { name: "New person" }));
        const dialog = screen.getByRole("dialog", { name: "New person" });
        fireEvent.click(within(dialog).getByRole("button", { name: "Create person" }));
        expect(within(dialog).getByText("Full name is required.")).toBeInTheDocument();
        expect(createStaff).not.toHaveBeenCalled();

        fireEvent.change(within(dialog).getByLabelText("Full name"), { target: { value: "  New Guide " } });
        fireEvent.change(within(dialog).getByLabelText("Role"), { target: { value: "RECEPTION" } });
        fireEvent.click(within(dialog).getByRole("button", { name: "Create person" }));

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

    it("adds a farm worker without a visitor role and filters the people without one", async () => {
        vi.mocked(fetchStaff).mockResolvedValue([
            MEMBER,
            { id: 9, fullName: "Tractor Driver", role: null, email: null, phone: null, active: true },
        ]);
        renderPage();
        expect(screen.getByText(/farm people directory/)).toBeInTheDocument();
        expect(await screen.findByText("Tractor Driver")).toBeInTheDocument();

        fireEvent.change(screen.getAllByLabelText("Role")[0], { target: { value: "NONE" } });
        expect(screen.queryByText("Guide One")).not.toBeInTheDocument();
        expect(screen.getByText("Tractor Driver")).toBeInTheDocument();

        fireEvent.click(screen.getByRole("button", { name: "New person" }));
        const dialog = screen.getByRole("dialog", { name: "New person" });
        fireEvent.change(within(dialog).getByLabelText("Full name"), { target: { value: "Mechanic" } });
        fireEvent.change(within(dialog).getByLabelText("Role"), { target: { value: "" } });
        fireEvent.click(within(dialog).getByRole("button", { name: "Create person" }));
        await waitFor(() =>
            expect(vi.mocked(createStaff).mock.calls[0]?.[0]).toEqual({
                fullName: "Mechanic",
                role: null,
                email: null,
                phone: null,
                active: true,
            }),
        );
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

    it("opens the new staff member dialog from a create link and removes the link on close", async () => {
        renderPage("/visitormanagement/staff?create=1");
        const dialog = await screen.findByRole("dialog", { name: "New person" });
        expect(screen.getByTestId("location")).toHaveTextContent("/visitormanagement/staff?create=1");

        fireEvent.click(within(dialog).getByRole("button", { name: "Close" }));
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(screen.getByTestId("location").textContent).toBe("/visitormanagement/staff");
    });

    it("removes the create link once the member is created", async () => {
        renderPage("/visitormanagement/staff?create=1");
        const dialog = await screen.findByRole("dialog", { name: "New person" });
        fireEvent.change(within(dialog).getByLabelText("Full name"), { target: { value: "Linked Guide" } });
        fireEvent.click(within(dialog).getByRole("button", { name: "Create person" }));

        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(createStaff).toHaveBeenCalled();
        expect(screen.getByTestId("location").textContent).toBe("/visitormanagement/staff");
    });
});
