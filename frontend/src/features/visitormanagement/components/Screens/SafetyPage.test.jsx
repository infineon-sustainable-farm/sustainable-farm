// @vitest-environment jsdom
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import SafetyPage from "./SafetyPage";

const deliverMutation = { mutate: vi.fn(), isPending: false, isError: false, error: null, variables: null };

vi.mock("../../hooks/useRegistrations", () => ({
    useRegistrations: () => ({
        data: [
            { id: 11, briefingId: 5, status: "CONFIRMED", visitorName: "Visitor One", groupSize: 4 },
            { id: 12, briefingId: null, status: "CONFIRMED", visitorName: "No briefing", groupSize: 1 },
            { id: 13, briefingId: 6, status: "PENDING", visitorName: "Not confirmed", groupSize: 2 },
        ],
        isPending: false,
        isError: false,
    }),
}));

vi.mock("../../../../shared/api/persons", () => ({
    fetchPersons: vi.fn(() => Promise.resolve([{ id: 1, fullName: "Front desk" }, { id: 2, fullName: "Awa Guide" }])),
}));

vi.mock("../../hooks/useSafety", () => ({
    useBriefing: () => ({ data: { status: "PENDING", deliveredAt: null, staffMember: null, signature: null }, isPending: false, isError: false }),
    useDeliverBriefing: () => deliverMutation,
}));

describe("SafetyPage", () => {
    it("tracks the confirmed registrations that carry a briefing", () => {
        render(<SafetyPage />);
        expect(screen.getByRole("heading", { level: 1, name: "Safety briefing tracker" })).toBeInTheDocument();
        expect(screen.getByText("1 briefing tracked")).toBeInTheDocument();
        expect(screen.getByText("Visitor One")).toBeInTheDocument();
        expect(screen.queryByText("Not confirmed")).not.toBeInTheDocument();
        expect(screen.getByText("Pending")).toBeInTheDocument();
    });

    it("records a pending briefing from its dialog", async () => {
        render(<SafetyPage />);
        fireEvent.click(screen.getByRole("button", { name: "record" }));
        expect(screen.getByRole("dialog", { name: "Record briefing — Visitor One" })).toBeInTheDocument();
        await screen.findByRole("option", { name: "Front desk" });
        fireEvent.change(screen.getByLabelText(/Delivered by/), { target: { value: "1" } });
        fireEvent.click(screen.getByRole("button", { name: "Record briefing" }));
        expect(deliverMutation.mutate).toHaveBeenCalledWith(
            { registrationId: 11, data: { staffMemberPersonId: 1, signature: null } },
            expect.any(Object),
        );
    });
});
