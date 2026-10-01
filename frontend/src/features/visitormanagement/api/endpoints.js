/*
 * Visitor Management endpoints, kept inside the feature rather than in the
 * shared endpoints file: several modules append to that file, and adjacent
 * additions from two branches make every merge conflict. Module-local
 * constants keep the shared file untouched, so merging develop stays clean.
 */
export const VM_ENDPOINTS = {
    DASHBOARD: "/api/v1/dashboard",
    VISITORS: "/api/v1/visitors",
    REGISTRATIONS: "/api/v1/registrations",
    TIME_SLOTS: "/api/v1/time-slots",
    ACTIVITIES: "/api/v1/activities",
    BOOKINGS: "/api/v1/bookings",
    EVENTS: "/api/v1/events",
    FEEDBACK: "/api/v1/feedback",
    SURVEYS: "/api/v1/surveys",
    TOUR_STOPS: "/api/v1/tour-stops",
    WORKSHOPS: "/api/v1/workshops",
    STAFF: "/api/v1/staff",
};