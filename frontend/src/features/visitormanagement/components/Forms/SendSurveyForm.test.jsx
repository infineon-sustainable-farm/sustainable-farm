// @vitest-environment jsdom
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import SendSurveyForm from "./SendSurveyForm";

const VISITORS = [
    { id: 1, fullName: "Visitor One", email: "one@example.com" },
    { id: 2, fullName: "Visitor Two", email: null },
];

function renderForm(props = {}) {
    const onSubmit = vi.fn();
    const onCancel = vi.fn();
    render(<SendSurveyForm visitors={VISITORS} onSubmit={onSubmit} onCancel={onCancel} {...props} />);
    return { onSubmit, onCancel };
}

describe("SendSurveyForm", () => {
    it("requires a visitor and a link", () => {
        const { onSubmit } = renderForm();
        fireEvent.click(screen.getByRole("button", { name: "Send survey" }));
        expect(screen.getByText("Pick the visitor.")).toBeInTheDocument();
        expect(screen.getByText("Enter the survey link.")).toBeInTheDocument();
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("shows the visitor's email read-only and sends the survey by email", () => {
        const { onSubmit } = renderForm();
        fireEvent.change(screen.getByLabelText("Visitor"), { target: { value: "1" } });
        expect(screen.getByLabelText("Email")).toHaveValue("one@example.com");
        expect(screen.getByLabelText("Email")).toHaveAttribute("readonly");
        fireEvent.change(screen.getByLabelText("Survey Link"), {
            target: { value: " https://forms.example.com/s " },
        });
        fireEvent.click(screen.getByRole("button", { name: "Send survey" }));
        expect(onSubmit).toHaveBeenCalledWith({
            visitorId: 1,
            channel: "EMAIL",
            messageTemplate: "https://forms.example.com/s",
        });
    });

    it("warns when the visitor has no email, and shows the server error", () => {
        renderForm({ submitError: "Server refused" });
        fireEvent.change(screen.getByLabelText("Visitor"), { target: { value: "2" } });
        expect(screen.getByText("This visitor has no email address.")).toBeInTheDocument();
        expect(screen.getByRole("alert")).toHaveTextContent("Server refused");
    });
});
