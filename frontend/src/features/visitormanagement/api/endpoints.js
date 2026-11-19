/*
 * Visitor Management endpoints, kept inside the feature rather than in the
 * shared endpoints file: several modules append to that file, and adjacent
 * additions from two branches make every merge conflict. Module-local
 * constants keep the shared file untouched, so merging develop stays clean.
 */
export const VM_ENDPOINTS = {
    DASHBOARD: "/api/visitor-management/dashboard",
    VISITORS: "/api/visitor-management/visitors",
    REGISTRATIONS: "/api/visitor-management/registrations",
    TIME_SLOTS: "/api/visitor-management/time-slots",
    ACTIVITIES: "/api/visitor-management/activities",
    BOOKINGS: "/api/visitor-management/bookings",
    EVENTS: "/api/visitor-management/events",
    FEEDBACK: "/api/visitor-management/feedback",
    SURVEYS: "/api/visitor-management/surveys",
    TOUR_STOPS: "/api/visitor-management/tour-stops",
    WORKSHOPS: "/api/visitor-management/workshops",
    STAFF: "/api/visitor-management/staff",
};