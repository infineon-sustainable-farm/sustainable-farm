// @vitest-environment jsdom
import { fireEvent, render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import EventsPage from "./EventsPage";

const mutations = vi.hoisted(() => ({}));

function mutation() {
    return { mutate: vi.fn(), reset: vi.fn(), isPending: false, isError: false, error: null };
}

const EVENTS = [
    {
        id: 1,
        title: "Harvest day",
        type: "OPEN_DAY",
        status: "DRAFT",
        startDateTime: "2026-09-21T09:00:00",
        endDateTime: "2026-09-21T11:00:00",
        location: "Courtyard",
        maxCapacity: 30,
        booked: 4,
    },
];

vi.mock("../../hooks/useEvents", () => ({
    useEvents: () => ({ data: EVENTS, isPending: false, isError: false, refetch: vi.fn(), isFetching: false }),
    useCreateEvent: () => mutations.create,
    useUpdateEvent: () => mutations.update,
    useEventStatusAction: () => mutations.status,
    useEventRegistrations: () => ({
        data: [{ id: 7, visitorName: "Visitor A", groupSize: 3, visitPurpose: "TOURISM", status: "CONFIRMED" }],
        isPending: false,
        isError: false,
        refetch: vi.fn(),
        isFetching: false,
    }),
}));

function renderPage() {
    return render(
        <MemoryRouter>
            <EventsPage />
        </MemoryRouter>,
    );
}

describe("EventsPage", () => {
    beforeEach(() => {
        mutations.create = mutation();
        mutations.update = mutation();
        mutations.status = mutation();
    });

    it("shows the event cards with their status and actions", () => {
        renderPage();
        expect(screen.getByRole("heading", { level: 1, name: "Events" })).toBeInTheDocument();
        const card = screen.getByRole("article");
        expect(within(card).getByText("Harvest day")).toBeInTheDocument();
        expect(within(card).getByText(/^Mon 21 Sept? · 9–11am · Courtyard · Max 30$/)).toBeInTheDocument();
        expect(within(card).getByText("4 registered")).toBeInTheDocument();
        expect(within(card).getByText("Draft")).toBeInTheDocument();
        fireEvent.click(within(card).getByRole("button", { name: "Publish" }));
        expect(mutations.status.mutate).toHaveBeenCalledWith({ id: 1, action: "publish" });
    });

    it("opens the participants panel of a card", () => {
        renderPage();
        fireEvent.click(screen.getByRole("button", { name: "Participants" }));
        expect(screen.getByRole("heading", { name: "Participants — Harvest day" })).toBeInTheDocument();
        expect(screen.getByText("Visitor A")).toBeInTheDocument();
        expect(screen.getByText("Tourism")).toBeInTheDocument();
        fireEvent.click(screen.getByRole("button", { name: "Close participants" }));
        expect(screen.queryByText("Visitor A")).not.toBeInTheDocument();
    });

    it("validates and creates an event from the dialog", () => {
        renderPage();
        fireEvent.click(screen.getByRole("button", { name: "New event" }));
        const dialog = screen.getByRole("dialog", { name: "New event" });

        fireEvent.click(within(dialog).getByRole("button", { name: "Create event" }));
        expect(within(dialog).getByText("Event name is required.")).toBeInTheDocument();
        expect(within(dialog).getByText("Pick a date.")).toBeInTheDocument();
        expect(mutations.create.mutate).not.toHaveBeenCalled();

        fireEvent.change(within(dialog).getByLabelText("Event name"), { target: { value: "Open day" } });
        fireEvent.change(within(dialog).getByLabelText("Date"), { target: { value: "2026-10-10" } });
        fireEvent.change(within(dialog).getByLabelText("Start time"), { target: { value: "09:00" } });
        fireEvent.change(within(dialog).getByLabelText("End time"), { target: { value: "08:00" } });
        fireEvent.click(within(dialog).getByRole("button", { name: "Create event" }));
        expect(within(dialog).getByText("End time must be after the start time.")).toBeInTheDocument();

        fireEvent.change(within(dialog).getByLabelText("End time"), { target: { value: "12:00" } });
        fireEvent.click(within(dialog).getByRole("button", { name: "Create event" }));
        expect(mutations.create.mutate).toHaveBeenCalledWith(
            {
                title: "Open day",
                type: "OPEN_DAY",
                startDateTime: "2026-10-10T09:00:00",
                endDateTime: "2026-10-10T12:00:00",
                maxCapacity: 10,
                location: null,
                description: null,
            },
            expect.objectContaining({ onSuccess: expect.any(Function) }),
        );
    });

    it("opens the edit dialog with the event values and a cancel button", () => {
        renderPage();
        fireEvent.click(screen.getByRole("button", { name: "Edit" }));
        const dialog = screen.getByRole("dialog", { name: "Edit event — Harvest day" });
        expect(within(dialog).getByLabelText("Event name")).toHaveValue("Harvest day");
        fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
});
