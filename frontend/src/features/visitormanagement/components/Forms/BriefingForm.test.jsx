// @vitest-environment jsdom
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import BriefingForm from "./BriefingForm";

describe("BriefingForm", () => {
    it("requires who delivered the briefing", () => {
        const onSubmit = vi.fn();
        render(<BriefingForm registrationId={3} onSubmit={onSubmit} onCancel={vi.fn()} />);
        fireEvent.click(screen.getByRole("button", { name: "Record briefing" }));
        expect(screen.getByText("Who delivered the briefing is required.")).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("records the staff member and the signature", () => {
        const onSubmit = vi.fn();
        render(<BriefingForm registrationId={3} onSubmit={onSubmit} onCancel={vi.fn()} />);
        fireEvent.change(screen.getByLabelText("Delivered by"), { target: { value: " Front desk " } });
        fireEvent.click(screen.getByRole("checkbox", { name: /Signature \(group leader\)/ }));
        fireEvent.click(screen.getByRole("button", { name: "Record briefing" }));
        expect(onSubmit).toHaveBeenCalledWith({ staffMember: "Front desk", signature: "signed" });
    });

    it("cancels", () => {
        const onCancel = vi.fn();
        render(<BriefingForm registrationId={3} onSubmit={vi.fn()} onCancel={onCancel} />);
        fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
        expect(onCancel).toHaveBeenCalled();
    });
});
