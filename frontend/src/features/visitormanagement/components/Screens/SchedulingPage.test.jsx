// @vitest-environment jsdom
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import SchedulingPage from "./SchedulingPage";
import { addDays, startOfWeek, toIsoDate } from "../../utils/format";
import {
    cancelTimeSlot,
    createTimeSlot,
    fetchStaff,
    fetchTimeSlots,
} from "../../api/visitormanagementApi";

vi.mock("../../api/visitormanagementApi", () => ({
    fetchTimeSlots: vi.fn(),
    fetchStaff: vi.fn(),
    fetchRegistrationsBySlot: vi.fn(),
    createTimeSlot: vi.fn(),
    updateTimeSlot: vi.fn(),
    cancelTimeSlot: vi.fn(),
}));

// A Saturday slot of the current week, so it is always on the displayed grid.
const SLOT = {
    id: 3,
    date: toIsoDate(addDays(startOfWeek(new Date()), 5)),
    startTime: "09:00:00",
    endTime: "11:00:00",
    maxCapacity: 10,
    booked: 0,
    status: "AVAILABLE",
    guideId: null,
};

function renderPage() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={client}>
            <SchedulingPage />
        </QueryClientProvider>,
    );
}

describe("SchedulingPage", () => {
    beforeEach(() => {
        vi.mocked(fetchTimeSlots).mockResolvedValue([SLOT]);
        vi.mocked(fetchStaff).mockResolvedValue([]);
        vi.mocked(createTimeSlot).mockResolvedValue({ id: 4 });
        vi.mocked(cancelTimeSlot).mockResolvedValue(undefined);
    });

    it("shows the week grid with the slot and its booked figure", async () => {
        renderPage();
        expect(screen.getByRole("heading", { level: 1, name: "Farm tour scheduling" })).toBeInTheDocument();
        expect(await screen.findByText("0/10")).toBeInTheDocument();
        expect(screen.getAllByText("9–11am").length).toBeGreaterThan(0);
        expect(screen.getByText("No visitors yet")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Prev" })).toBeDisabled();
    });

    it("creates a slot from the new slot dialog", async () => {
        renderPage();
        await screen.findByText("0/10");
        fireEvent.click(screen.getByRole("button", { name: "New slot" }));
        const dialog = screen.getByRole("dialog", { name: "New slot" });
        fireEvent.change(within(dialog).getByLabelText("Capacity"), { target: { value: "1.5" } });
        fireEvent.submit(within(dialog).getByRole("button", { name: "Create slot" }).closest("form"));
        expect(within(dialog).getByText("Capacity must be a whole number of at least 1.")).toBeInTheDocument();

        fireEvent.change(within(dialog).getByLabelText("Capacity"), { target: { value: "25" } });
        fireEvent.click(within(dialog).getByRole("button", { name: "Create slot" }));
        await waitFor(() =>
            expect(vi.mocked(createTimeSlot).mock.calls[0]?.[0]).toEqual({
                date: toIsoDate(startOfWeek(new Date())),
                startTime: "09:00:00",
                endTime: "11:00:00",
                maxCapacity: 25,
                guideId: null,
            }),
        );
    });

    it("confirms before cancelling a slot", async () => {
        renderPage();
        fireEvent.click(await screen.findByRole("button", { name: "cancel" }));
        const dialog = screen.getByRole("dialog", { name: "Cancel slot?" });
        expect(
            within(dialog).getByText("Cancelling a slot also cancels its registrations and bookings."),
        ).toBeInTheDocument();
        fireEvent.click(within(dialog).getByRole("button", { name: "Yes" }));
        await waitFor(() => expect(vi.mocked(cancelTimeSlot).mock.calls[0]?.[0]).toEqual(3));
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    });
});
