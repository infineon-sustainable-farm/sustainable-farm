// @vitest-environment jsdom
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import FeedbackPage from "./FeedbackPage";

const mutation = () => ({ mutate: vi.fn(), reset: vi.fn(), isPending: false, isError: false, error: null });
const sendMutation = mutation();

vi.mock("../../hooks/useFeedback", () => ({
    useFeedbackList: () => ({
        data: [{ id: 7, visitorName: "Visitor One", submittedAt: "2026-09-20T18:30:00", rating: 4, comment: "Nice" }],
        isPending: false,
        isError: false,
    }),
    useSurveys: () => ({ data: [], isPending: false, isError: false }),
    useSubmitFeedback: () => mutation(),
    useSendSurvey: () => sendMutation,
    useRouteFeedback: () => mutation(),
}));

vi.mock("../../hooks/useVisitors", () => ({
    useVisitors: () => ({ data: [{ id: 1, fullName: "Visitor One", email: "one@example.com" }], isError: false }),
}));

describe("FeedbackPage", () => {
    it("lists the responses and the empty survey list", () => {
        render(<FeedbackPage />);
        expect(screen.getByRole("heading", { level: 1, name: "Satisfaction survey" })).toBeInTheDocument();
        expect(screen.getByText("Nice")).toBeInTheDocument();
        expect(screen.getByText(/^20 Sept? 2026, 18:30$/)).toBeInTheDocument();
        expect(screen.getByText("No survey sent yet.")).toBeInTheDocument();
        expect(document.title).toBe("Satisfaction Survey — Visitor Management");
    });

    it("opens and closes the send survey dialog", () => {
        render(<FeedbackPage />);
        fireEvent.click(screen.getByRole("button", { name: "Send survey (email)" }));
        expect(screen.getByRole("dialog", { name: "Send survey" })).toBeInTheDocument();
        fireEvent.keyDown(document, { key: "Escape" });
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });

    it("opens the on-site tablet survey dialog", () => {
        render(<FeedbackPage />);
        fireEvent.click(screen.getByRole("button", { name: "New on-site response" }));
        expect(screen.getByRole("dialog", { name: "On-site tablet survey" })).toBeInTheDocument();
        fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });

    it("reveals the route input of a response", () => {
        render(<FeedbackPage />);
        fireEvent.click(screen.getByRole("button", { name: "route" }));
        fireEvent.click(screen.getByRole("button", { name: "save" }));
        expect(screen.getByRole("alert")).toHaveTextContent("Team is required.");
    });
});
