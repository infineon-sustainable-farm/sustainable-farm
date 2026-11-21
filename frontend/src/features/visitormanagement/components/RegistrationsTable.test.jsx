// @vitest-environment jsdom
import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import RegistrationsTable from "./RegistrationsTable";

const registrations = [
    { id: 1, visitorId: 7, visitorName: "Visitor Seven", groupSize: 2, status: "PENDING", slotDate: "2026-09-21", timeSlotId: 30 },
    { id: 2, visitorId: 7, visitorName: "Visitor Seven", groupSize: 2, status: "CHECKED_IN", eventId: 4 },
];

function renderTable(onAction = vi.fn()) {
    render(
        <RegistrationsTable
            registrations={registrations}
            visitorsById={new Map([[7, { id: 7, language: "Moore", type: "SCHOOL" }]])}
            slotsById={new Map([[30, { id: 30, startTime: "09:00:00", endTime: "11:00:00" }]])}
            eventsById={new Map([[4, { id: 4, title: "Open day" }]])}
            pendingAction={null}
            actionError={null}
            onAction={onAction}
            onEdit={vi.fn()}
        />,
    );
    return onAction;
}

describe("RegistrationsTable", () => {
    it("offers only the transitions the backend accepts and shows the slot or event", () => {
        renderTable();
        const [, pending, checkedIn] = screen.getAllByRole("row");
        expect(within(pending).getByText(/^Mon 21 Sept? · 9–11am$/)).toBeInTheDocument();
        expect(within(pending).getByRole("button", { name: "approve" })).toBeInTheDocument();
        expect(within(pending).getByText("Pending")).toBeInTheDocument();
        expect(within(checkedIn).getByText("Event · Open day")).toBeInTheDocument();
        expect(within(checkedIn).queryByRole("button", { name: "cancel" })).not.toBeInTheDocument();
    });

    it("confirms a cancellation in a dialog before sending it", () => {
        const onAction = renderTable();
        fireEvent.click(screen.getByRole("button", { name: "cancel" }));
        const dialog = screen.getByRole("dialog", { name: "Cancel registration?" });
        expect(onAction).not.toHaveBeenCalled();
        fireEvent.click(within(dialog).getByRole("button", { name: "Yes" }));
        expect(onAction).toHaveBeenCalledWith(registrations[0], "cancel");
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
});
