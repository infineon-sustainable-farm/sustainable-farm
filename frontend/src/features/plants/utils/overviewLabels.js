import { formatBlock, formatCode, formatNumber } from "./format";

/*
 * Each alert type in a few words, with the screen that lists it. The full
 * sentence stays the API's `message`; these words only let a row be scanned.
 */
const ALERT_TYPES = {
    OPEN_HEALTH_ISSUE: { issue: "Health issue still open", screen: "Diseases & Alerts", path: "/plants/diseases-alerts" },
    PRE_HARVEST_INTERVAL: {
        issue: "Pre-harvest interval running",
        screen: "Diseases & Alerts",
        path: "/plants/diseases-alerts",
    },
    // A stock alert has no date: it holds as long as the stock stays low, so it reads "Now".
    LOW_STOCK: { issue: "Low stock", screen: "Fertilizer Inventory", path: "/plants/fertilizer-inventory", undated: "Now" },
    NURSERY_READY: { issue: "Ready to transplant", screen: "Nursery", path: "/plants/nursery" },
    HARVEST_APPROACHING: {
        issue: "Harvest season approaching",
        screen: "Harvest & Maturity",
        path: "/plants/harvest-maturity",
    },
};

/** A type the API adds later: its code in sentence case, and the alerts screen. */
const OTHER_TYPE = { screen: "Diseases & Alerts", path: "/plants/diseases-alerts" };

/** The short wording and the screen of an alert type. */
export function alertTypeLabel(type) {
    return ALERT_TYPES[type] ?? { ...OTHER_TYPE, issue: formatCode(type) };
}

/**
 * Where an alert applies, from its own fields: a fertilizer belongs to the
 * inventory, a batch to the nursery, anything else to its block. `batchCode`
 * and `treeLabel` come from the batch and the finding the alert points at,
 * when the screen has them; without them the row still names the place. Only
 * an open health problem names its tree: a pre-harvest interval also points
 * at a finding, but holds back the harvest of the whole block.
 */
export function alertPlace(alert, { batchCode = null, treeLabel = null } = {}) {
    if (alert.type === "LOW_STOCK" || (alert.fertilizerId != null && alert.blockCode == null)) return "Inventory";
    if (alert.type === "NURSERY_READY") return batchCode ? `Batch ${batchCode}` : "Nursery";
    if (alert.blockCode == null) return "All blocks";
    const block = formatBlock(alert.blockCode);
    return alert.type === "OPEN_HEALTH_ISSUE" && treeLabel ? `${block} · Tree ${treeLabel}` : block;
}

/**
 * The days between today and an alert's date, as the API counts them:
 * "in 9 days", "today", "7 days ago". Null when the alert has no date.
 */
export function formatRelativeDays(days) {
    if (days === null || days === undefined || String(days).trim() === "") return null;
    const count = Number(days);
    if (!Number.isInteger(count)) return null;
    if (count === 0) return "today";
    const amount = `${formatNumber(Math.abs(count))} ${Math.abs(count) === 1 ? "day" : "days"}`;
    return count > 0 ? `in ${amount}` : `${amount} ago`;
}

/*
 * What each maturity status means, in the words of the server's definitions
 * (MaturityStatus): Ripe also holds the trees whose window is open while a
 * pre-harvest interval still runs, so 0 Ready for harvest beside 40 Ripe is
 * not a contradiction. The ring, its legend and the KPI share these words.
 */
export const MATURITY_MEANINGS = {
    IMMATURE: "Harvest not near yet, young trees included",
    RIPE: "Harvest near, or on hold for a pre-harvest interval",
    READY_FOR_HARVEST: "Can be picked today",
};
