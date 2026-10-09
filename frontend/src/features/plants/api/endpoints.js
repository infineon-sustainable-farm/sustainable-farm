/*
 * Plants endpoints, kept inside the feature rather than in the shared
 * endpoints file: several modules append to that file, and adjacent additions
 * from two branches make every merge conflict. Module-local constants keep the
 * shared file untouched, so merging develop stays clean.
 */
export const PLANTS_ENDPOINTS = {
    VARIETIES: "/api/plants/varieties",
    GROWTH_CALENDAR: "/api/plants/growth-calendar",
    PLANTINGS: "/api/plants/plantings",
    HARVESTS: "/api/plants/harvests",
    YIELD_FORECAST: "/api/plants/yield-forecast",
    FERTILIZERS: "/api/plants/fertilizers",
    FERTILIZER_MOVEMENTS: "/api/plants/fertilizer-movements",
    EUR_XOF_RATE: "/api/plants/currency-rates/EUR/XOF",
    VARIETY_REFERENCES: "/api/plants/variety-references",
    GROWTH_PHASE_YIELD_SHARES: "/api/plants/growth-phase-yield-shares",
};
