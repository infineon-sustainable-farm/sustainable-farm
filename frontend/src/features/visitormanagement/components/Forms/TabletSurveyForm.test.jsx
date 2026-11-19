// @vitest-environment jsdom
import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import TabletSurveyForm from "./TabletSurveyForm";

function answer(question, option) {
    const group = screen.getByRole("group", { name: question });
    fireEvent.click(within(group).getByRole("button", { name: option }));
}

describe("TabletSurveyForm", () => {
    it("validates every required answer", () => {
        const onSubmit = vi.fn();
        render(<TabletSurveyForm onSubmit={onSubmit} onCancel={vi.fn()} />);
        fireEvent.click(screen.getByRole("button", { name: "Submit feedback (tablet)" }));
        expect(screen.getByText("Enter the visitor's name.")).toBeInTheDocument();
        expect(screen.getByText("Pick a rating from 1 to 5.")).toBeInTheDocument();
        expect(screen.getAllByText("Pick an answer.")).toHaveLength(3);
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it("submits the rating, the Yes/No answers and the comment", () => {
        const onSubmit = vi.fn();
        render(<TabletSurveyForm onSubmit={onSubmit} onCancel={vi.fn()} />);
        fireEvent.change(screen.getByLabelText("Your name"), { target: { value: " A visitor " } });
        fireEvent.click(screen.getByRole("radio", { name: "4 out of 5" }));
        answer("Was the safety briefing clear?", "Yes");
        answer("Did the educational program teach you something new?", "No");
        answer("Would you recommend this visit?", "Yes");
        expect(
            within(screen.getByRole("group", { name: "Would you recommend this visit?" })).getByRole("button", {
                name: "Yes",
            }),
        ).toHaveAttribute("aria-pressed", "true");
        fireEvent.click(screen.getByRole("button", { name: "Submit feedback (tablet)" }));
        expect(onSubmit).toHaveBeenCalledWith({
            visitorName: "A visitor",
            rating: 4,
            briefingClear: "Yes",
            educationalValue: "No",
            recommend: "Yes",
            comment: null,
        });
    });
});
