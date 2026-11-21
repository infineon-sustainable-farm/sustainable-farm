// @vitest-environment jsdom
import { fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import BookingPage from "./BookingPage";

const mutations = {
    update: vi.fn(),
    action: vi.fn(),
    createActivity: vi.fn(),
    updateActivity: vi.fn(),
    deactivate: vi.fn(),
};

function mutation(mutate) {
    return { mutate, reset: vi.fn(), isPending: false, isError: false, error: null, variables: undefined };
}

const activities = [
    { id: 1, name: "Farm tour", price: 5000, capacity: 15, durationMinutes: 45, active: true, description: null },
    { id: 2, name: "Old workshop", price: 0, capacity: 5, durationMinutes: 30, active: false, description: null },
];

const bookings = [
    {
        id: 10,
        reference: "BK-10",
        activityId: 1,
        activityName: "Farm tour",
        timeSlotId: 7,
        slotDate: "2026-09-21",
        slotStartTime: "09:00:00",
        slotEndTime: "11:00:00",
        visitorFullName: "Visitor One",
        visitorEmail: "one@example.com",
        visitorPhone: null,
        peopleCount: 2,
        paymentMethod: "CASH",
        paymentStatus: "UNPAID",
        status: "PENDING",
    },
    { id: 11, reference: "BK-11", activityName: "Farm tour", status: "CANCELLED", paymentStatus: "REFUNDED" },
];

vi.mock("../../hooks/useBooking", () => ({
    useBookings: () => ({ data: bookings, isPending: false, isError: false, refetch: vi.fn(), isFetching: false }),
    useActivities: () => ({ data: activities, isError: false }),
    useOccupancy: () => ({
        data: [{ activityId: 1, activityName: "Farm tour", occupancyPercent: 40, booked: 6, capacity: 15 }],
        isPending: false,
        isError: false,
        refetch: vi.fn(),
        isFetching: false,
    }),
    useUpdateBooking: () => mutation(mutations.update),
    useBookingAction: () => mutation(mutations.action),
    useCreateActivity: () => mutation(mutations.createActivity),
    useUpdateActivity: () => mutation(mutations.updateActivity),
    useDeactivateActivity: () => mutation(mutations.deactivate),
}));

vi.mock("../../hooks/useRegistrations", () => ({
    useAvailability: () => ({
        data: [{ id: 7, startTime: "09:00:00", endTime: "11:00:00", remaining: 4 }],
        isPending: false,
        isError: false,
        refetch: vi.fn(),
    }),
}));

describe("BookingPage", () => {
    beforeEach(() => {
        Object.values(mutations).forEach((fn) => fn.mockClear());
    });

    it("lists the active bookings with their slot and payment, cancelled ones on demand", () => {
        render(<BookingPage />);
        expect(screen.getByRole("heading", { level: 1, name: "Agritourism booking system" })).toBeInTheDocument();
        const table = screen.getByRole("table", { name: "Bookings" });
        expect(within(table).getByText("BK-10")).toBeInTheDocument();
        expect(within(table).getByText(/^Mon 21 Sept? · 9–11am$/)).toBeInTheDocument();
        expect(within(table).getByText("Cash · Unpaid")).toBeInTheDocument();
        expect(within(table).queryByText("BK-11")).not.toBeInTheDocument();

        fireEvent.click(screen.getByLabelText("Show cancelled"));
        expect(within(screen.getByRole("table", { name: "Bookings" })).getByText("BK-11")).toBeInTheDocument();
    });

    it("runs a row action through the booking action mutation", () => {
        render(<BookingPage />);
        fireEvent.click(screen.getByRole("button", { name: "pay" }));
        expect(mutations.action).toHaveBeenCalledWith({ id: 10, action: "pay" });
    });

    it("edits a booking in a dialog, validates it and submits the payload", () => {
        render(<BookingPage />);
        const table = screen.getByRole("table", { name: "Bookings" });
        fireEvent.click(within(table).getByRole("button", { name: "edit" }));
        const dialog = screen.getByRole("dialog", { name: "Edit booking — BK-10" });

        fireEvent.change(within(dialog).getByLabelText("Visitor full name"), { target: { value: " " } });
        fireEvent.click(within(dialog).getByRole("button", { name: "Save booking" }));
        expect(within(dialog).getByText("Visitor name is required.")).toBeInTheDocument();
        expect(mutations.update).not.toHaveBeenCalled();

        fireEvent.change(within(dialog).getByLabelText("Visitor full name"), { target: { value: "Visitor Two" } });
        fireEvent.click(within(dialog).getByRole("button", { name: "Save booking" }));
        expect(mutations.update).toHaveBeenCalledWith(
            {
                id: 10,
                data: {
                    activityId: 1,
                    timeSlotId: 7,
                    visitorFullName: "Visitor Two",
                    visitorEmail: "one@example.com",
                    visitorPhone: null,
                    peopleCount: 2,
                    paymentMethod: "CASH",
                },
            },
            expect.objectContaining({ onSuccess: expect.any(Function) }),
        );

        fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });

    it("creates an activity from its dialog after validation", () => {
        render(<BookingPage />);
        fireEvent.click(screen.getByRole("button", { name: "New activity" }));
        const dialog = screen.getByRole("dialog", { name: "New activity" });

        fireEvent.click(within(dialog).getByRole("button", { name: "Create activity" }));
        expect(within(dialog).getByText("Activity name is required.")).toBeInTheDocument();
        expect(within(dialog).getByText("Capacity must be a whole number of at least 1.")).toBeInTheDocument();
        expect(mutations.createActivity).not.toHaveBeenCalled();

        fireEvent.change(within(dialog).getByLabelText("Activity name"), { target: { value: "Bee keeping" } });
        fireEvent.change(within(dialog).getByLabelText("Price (FCFA)"), { target: { value: "2000" } });
        fireEvent.change(within(dialog).getByLabelText("Capacity"), { target: { value: "10" } });
        fireEvent.change(within(dialog).getByLabelText("Duration (min)"), { target: { value: "60" } });
        fireEvent.click(within(dialog).getByRole("button", { name: "Create activity" }));
        expect(mutations.createActivity).toHaveBeenCalledWith(
            { name: "Bee keeping", price: 2000, capacity: 10, durationMinutes: 60, description: null },
            expect.objectContaining({ onSuccess: expect.any(Function) }),
        );
    });

    it("asks for confirmation before deactivating an activity", () => {
        render(<BookingPage />);
        const activitiesTable = screen.getByRole("table", { name: "Activities" });
        expect(within(activitiesTable).queryByText("Old workshop")).not.toBeInTheDocument();
        expect(within(activitiesTable).getByText("5 000 FCFA")).toBeInTheDocument();

        fireEvent.click(within(activitiesTable).getByRole("button", { name: "deactivate" }));
        const dialog = screen.getByRole("dialog", { name: "Deactivate and cancel its bookings?" });
        fireEvent.click(within(dialog).getByRole("button", { name: "No" }));
        expect(mutations.deactivate).not.toHaveBeenCalled();

        fireEvent.click(within(activitiesTable).getByRole("button", { name: "deactivate" }));
        fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Yes" }));
        expect(mutations.deactivate).toHaveBeenCalledWith(1);
    });
});
