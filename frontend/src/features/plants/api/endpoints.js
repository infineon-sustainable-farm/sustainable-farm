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
    YIELD_COMPARISON: "/api/plants/yield-comparison",
    HARVEST_STATUS: "/api/plants/harvest-status",
    MATURITY_OBSERVATIONS: "/api/plants/maturity-observations",
    FERTILIZERS: "/api/plants/fertilizers",
    FERTILIZER_MOVEMENTS: "/api/plants/fertilizer-movements",
    EUR_XOF_RATE: "/api/plants/currency-rates/EUR/XOF",
    VARIETY_REFERENCES: "/api/plants/variety-references",
    GROWTH_PHASE_YIELD_SHARES: "/api/plants/growth-phase-yield-shares",
    HEALTH_ISSUES: "/api/plants/health-issues",
    BLOCK_HEALTH: "/api/plants/block-health",
    HEALTH_INSPECTIONS: "/api/plants/health-inspections",
    HEALTH_FINDINGS: "/api/plants/health-findings",
    TREATMENTS: "/api/plants/treatments",
    ALERTS: "/api/plants/alerts",
    NURSERY_BATCHES: "/api/plants/nursery-batches",
    NURSERY_EVENTS: "/api/plants/nursery-events",
    OVERVIEW: "/api/plants/overview",
    SCENARIOS: "/api/plants/scenarios",
};
