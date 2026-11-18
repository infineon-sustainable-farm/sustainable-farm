// @vitest-environment jsdom
import { fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import EducationPage from "./EducationPage";

const mutations = vi.hoisted(() => ({}));

function mutation() {
    return { mutate: vi.fn(), reset: vi.fn(), isPending: false, isError: false, error: null };
}

function query(data) {
    return { data, isPending: false, isError: false, refetch: vi.fn(), isFetching: false };
}

const STOPS = [
    { id: 1, name: "Orchard", position: 1, durationMinutes: 20, maxCapacity: 15, location: "North", active: true },
    { id: 2, name: "Old barn", position: 2, durationMinutes: 10, maxCapacity: 10, active: false },
];
const WORKSHOPS = [
    { id: 5, name: "Composting", durationMinutes: 40, targetGroup: "School", facilitator: null, status: "DRAFT" },
];

vi.mock("../../hooks/useEducation", () => ({
    useTourStops: () => query(STOPS),
    useWorkshops: () => query(WORKSHOPS),
    useCreateTourStop: () => mutations.createStop,
    useUpdateTourStop: () => mutations.updateStop,
    useDeactivateTourStop: () => mutations.deactivateStop,
    useCreateWorkshop: () => mutations.createWorkshop,
    useUpdateWorkshop: () => mutations.updateWorkshop,
    useWorkshopStatusAction: () => mutations.workshopStatus,
}));

vi.mock("../../hooks/useScheduling", () => ({
    useGuides: () => query([{ id: 9, fullName: "Guide One" }]),
}));

describe("EducationPage", () => {
    beforeEach(() => {
        for (const key of ["createStop", "updateStop", "deactivateStop", "createWorkshop", "updateWorkshop", "workshopStatus"]) {
            mutations[key] = mutation();
        }
    });

    it("shows the active stops, the hidden count and the workshops", () => {
        render(<EducationPage />);
        expect(screen.getByRole("heading", { level: 1, name: "Educational program" })).toBeInTheDocument();
        expect(screen.getByText("Orchard")).toBeInTheDocument();
        expect(screen.getByText("20 min · North · max 15 pers")).toBeInTheDocument();
        expect(screen.queryByText("Old barn")).not.toBeInTheDocument();
        expect(
            screen.getByText("1 deactivated stop is hidden from the tour — the API offers no reactivation."),
        ).toBeInTheDocument();
        expect(screen.getByText("Composting")).toBeInTheDocument();
        fireEvent.click(screen.getByRole("button", { name: "Publish" }));
        expect(mutations.workshopStatus.mutate).toHaveBeenCalledWith({ id: 5, action: "publish" });
    });

    it("asks for confirmation before deactivating a stop", () => {
        render(<EducationPage />);
        fireEvent.click(screen.getByRole("button", { name: "Deactivate" }));
        const dialog = screen.getByRole("dialog", { name: "Deactivate this stop?" });
        fireEvent.click(within(dialog).getByRole("button", { name: "No" }));
        expect(mutations.deactivateStop.mutate).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole("button", { name: "Deactivate" }));
        fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Yes" }));
        expect(mutations.deactivateStop.mutate).toHaveBeenCalledWith(1);
    });

    it("validates and creates a tour stop from the dialog", () => {
        render(<EducationPage />);
        fireEvent.click(screen.getByRole("button", { name: "New stop" }));
        const dialog = screen.getByRole("dialog", { name: "New tour stop" });
        fireEvent.submit(within(dialog).getByRole("button", { name: "Create stop" }).closest("form"));
        expect(within(dialog).getByText("Name is required.")).toBeInTheDocument();
        expect(within(dialog).getByText("Position must be a whole number of at least 1.")).toBeInTheDocument();

        fireEvent.change(within(dialog).getByLabelText("Stop name"), { target: { value: "Pond" } });
        fireEvent.change(within(dialog).getByLabelText("Position"), { target: { value: "3" } });
        fireEvent.change(within(dialog).getByLabelText("Duration (min)"), { target: { value: "15" } });
        fireEvent.change(within(dialog).getByLabelText("Max capacity"), { target: { value: "12" } });
        fireEvent.submit(within(dialog).getByRole("button", { name: "Create stop" }).closest("form"));
        expect(mutations.createStop.mutate).toHaveBeenCalledWith(
            {
                name: "Pond",
                position: 3,
                durationMinutes: 15,
                maxCapacity: 12,
                location: null,
                demo: null,
                safetyNotes: null,
                description: null,
            },
            expect.objectContaining({ onSuccess: expect.any(Function) }),
        );
    });

    it("edits a workshop in the dialog with the guides as facilitators", () => {
        render(<EducationPage />);
        fireEvent.click(screen.getAllByRole("button", { name: "Edit" }).at(-1));
        const dialog = screen.getByRole("dialog", { name: "Edit workshop — Composting" });
        fireEvent.change(within(dialog).getByLabelText("Facilitator (guide)"), { target: { value: "Guide One" } });
        fireEvent.submit(within(dialog).getByRole("button", { name: "Save workshop" }).closest("form"));
        expect(mutations.updateWorkshop.mutate).toHaveBeenCalledWith(
            {
                id: 5,
                data: {
                    name: "Composting",
                    durationMinutes: 40,
                    targetGroup: "School",
                    facilitator: "Guide One",
                    description: null,
                },
            },
            expect.objectContaining({ onSuccess: expect.any(Function) }),
        );
    });
});
