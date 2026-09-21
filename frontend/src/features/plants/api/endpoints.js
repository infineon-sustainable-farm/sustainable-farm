/*
 * Plants endpoints, kept inside the feature rather than in the shared
 * endpoints file: several modules append to that file, and adjacent additions
 * from two branches make every merge conflict. Module-local constants keep the
 * shared file untouched, so merging develop stays clean.
 */
export const PLANTS_ENDPOINTS = {
    VARIETIES: "/api/plants/varieties",
    GROWTH_CALENDAR: "/api/plants/growth-calendar",
};
