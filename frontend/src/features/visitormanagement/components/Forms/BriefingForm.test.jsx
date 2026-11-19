// @vitest-environment jsdom
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import BriefingForm from "./BriefingForm";

vi.mock("../../../../shared/api/persons", () => ({
    fetchPersons: vi.fn(() => Promise.resolve([{ id: 1, fullName: "Front desk" }, { id: 2, fullName: "Awa Guide" }])),
}));

describe("BriefingForm", () => {
    it("requires who delivered the briefing", async () => {
        const onSubmit = vi.fn();
        render(<BriefingForm registrationId={3} onSubmit={onSubmit} onCancel={vi.fn()} />);
        await screen.findByRole("option", { name: "Front desk" });
        fireEvent.click(screen.getByRole("button", { name: "Record briefing" }));
        expect(screen.getByText("Who delivered the briefing is required.")).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("records the person picked in the directory and the signature", async () => {
        const onSubmit = vi.fn();
        render(<BriefingForm registrationId={3} onSubmit={onSubmit} onCancel={vi.fn()} />);
        await screen.findByRole("option", { name: "Awa Guide" });
        fireEvent.change(screen.getByLabelText(/Delivered by/), { target: { value: "2" } });
        fireEvent.click(screen.getByRole("checkbox", { name: /Signature \(group leader\)/ }));
        fireEvent.click(screen.getByRole("button", { name: "Record briefing" }));
        expect(onSubmit).toHaveBeenCalledWith({ staffMemberPersonId: 2, signature: "signed" });
    });

    it("cancels", () => {
        const onCancel = vi.fn();
        render(<BriefingForm registrationId={3} onSubmit={vi.fn()} onCancel={onCancel} />);
        fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
        expect(onCancel).toHaveBeenCalled();
    });
});
