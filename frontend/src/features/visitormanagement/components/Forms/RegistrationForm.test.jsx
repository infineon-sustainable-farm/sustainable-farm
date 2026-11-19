// @vitest-environment jsdom
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import RegistrationForm from "./RegistrationForm";

const visitors = [
    { id: 7, fullName: "Visitor Seven", type: "SCHOOL", groupSize: 12, email: "seven@example.com", language: "French" },
];
const availability = [
    { id: 30, startTime: "09:00:00", endTime: "11:00:00", remaining: 5 },
    { id: 31, startTime: "14:00:00", endTime: "16:00:00", remaining: 20 },
];

function renderForm(props = {}) {
    const onSubmit = vi.fn();
    render(
        <RegistrationForm
            mode="TOUR"
            onModeChange={vi.fn()}
            date="2026-09-21"
            onDateChange={vi.fn()}
            availability={availability}
            availabilityLoading={false}
            availabilityError={false}
            onRetryAvailability={vi.fn()}
            nextAvailableDate={null}
            onUseNextDate={vi.fn()}
            events={[]}
            activities={[]}
            visitors={visitors}
            editingVisitor={null}
            isSubmitting={false}
            onSubmit={onSubmit}
            onCancel={vi.fn()}
            {...props}
        />,
    );
    return onSubmit;
}

describe("RegistrationForm", () => {
    it("refuses a name that is not in the directory", () => {
        const onSubmit = renderForm();
        fireEvent.change(screen.getByLabelText("Full name"), { target: { value: "Nobody" } });
        fireEvent.click(screen.getByRole("button", { name: "Register visitor" }));
        expect(screen.getByText("No visitor with this name in the directory.")).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("fetches the visitor from the directory and registers them on the first slot that fits", () => {
        const onSubmit = renderForm();
        fireEvent.change(screen.getByLabelText("Full name"), { target: { value: "visitor seven" } });
        expect(screen.getByText("people")).toBeInTheDocument();
        expect(screen.getByRole("option", { name: "9–11am · 5 left" })).toBeDisabled();
        fireEvent.click(screen.getByRole("button", { name: "Register visitor" }));
        expect(onSubmit).toHaveBeenCalledWith({ visitorId: 7, timeSlotId: 31, visitPurpose: "EDUCATION" });
    });

    it("switches the target with the mode buttons", () => {
        const onModeChange = vi.fn();
        renderForm({ onModeChange });
        expect(screen.getByRole("button", { name: "Farm tour" })).toHaveAttribute("aria-pressed", "true");
        fireEvent.click(screen.getByRole("button", { name: "Event" }));
        expect(onModeChange).toHaveBeenCalledWith("EVENT");
    });
});
