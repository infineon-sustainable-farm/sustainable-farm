import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../../../shared/api/client", () => ({
    apiClient: {
        get: vi.fn(() => Promise.resolve({ data: [] })),
        post: vi.fn(() => Promise.resolve({ data: {} })),
        put: vi.fn(() => Promise.resolve({ data: {} })),
        patch: vi.fn(() => Promise.resolve({ data: {} })),
        delete: vi.fn(() => Promise.resolve({ data: {} })),
    },
}));

import { apiClient } from "../../../shared/api/client";
import { VM_ENDPOINTS } from "./endpoints";
import {
    cancelBooking,
    createBooking,
    deliverBriefing,
    fetchAvailability,
    fetchBookings,
    fetchDashboard,
    fetchOccupancy,
    fetchStaff,
    fetchVisitors,
    routeFeedback,
} from "./visitormanagementApi";

describe("visitor management endpoints", () => {
    it("all live under /api/visitor-management", () => {
        for (const path of Object.values(VM_ENDPOINTS)) {
            expect(path).toMatch(/^\/api\/visitor-management\/[a-z-]+$/);
        }
    });
});

describe("visitor management api", () => {
    beforeEach(() => vi.clearAllMocks());

    it("creates a booking with the visitor fields unchanged", async () => {
        const booking = {
            activityId: 1,
            timeSlotId: 2,
            visitorFullName: "Lucas Weber",
            visitorEmail: "lucas@example.com",
            visitorPhone: "+22612345678",
            peopleCount: 3,
        };
        await createBooking(booking);
        expect(apiClient.post).toHaveBeenCalledWith("/api/visitor-management/bookings", booking);
    });

    it("calls the module paths for lists and actions", async () => {
        await fetchDashboard();
        await fetchVisitors();
        await fetchStaff();
        await fetchOccupancy();
        await fetchAvailability("2026-08-26");
        await fetchBookings({ page: 1, size: 20 });
        await cancelBooking(7);
        await routeFeedback(4, { routedTo: "staff" });
        await deliverBriefing(9, { staffMember: "Alix" });

        expect(apiClient.get).toHaveBeenCalledWith("/api/visitor-management/dashboard");
        expect(apiClient.get).toHaveBeenCalledWith("/api/visitor-management/visitors");
        expect(apiClient.get).toHaveBeenCalledWith("/api/visitor-management/staff");
        expect(apiClient.get).toHaveBeenCalledWith("/api/visitor-management/bookings/occupancy");
        expect(apiClient.get).toHaveBeenCalledWith("/api/visitor-management/time-slots/availability", {
            params: { date: "2026-08-26" },
        });
        expect(apiClient.get).toHaveBeenCalledWith(
            "/api/visitor-management/bookings",
            expect.objectContaining({ params: expect.objectContaining({ page: 1, size: 20 }) }),
        );
        expect(apiClient.post).toHaveBeenCalledWith("/api/visitor-management/bookings/7/cancel");
        expect(apiClient.patch).toHaveBeenCalledWith("/api/visitor-management/feedback/4/route", {
            routedTo: "staff",
        });
        expect(apiClient.patch).toHaveBeenCalledWith(
            "/api/visitor-management/registrations/9/briefing/deliver",
            { staffMember: "Alix" },
        );
    });
});
