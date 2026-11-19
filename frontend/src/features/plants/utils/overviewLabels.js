import i18n from "../../../shared/i18n/i18n";
import { formatBlock, formatCode, formatNumber } from "./format";

/** Text of the Plants namespace in the active language, read at each call. */
function t(key, options) {
    return i18n.t(key, { ns: "plants", ...options });
}

/*
 * Each alert type in a few words, with the screen that lists it. The full
 * sentence stays the API's `message`; these words only let a row be scanned.
 * The screen is named as its sidebar entry.
 */
const ALERT_TYPES = {
    OPEN_HEALTH_ISSUE: { screenKey: "nav.diseasesAlerts", path: "/plants/diseases-alerts" },
    PRE_HARVEST_INTERVAL: { screenKey: "nav.diseasesAlerts", path: "/plants/diseases-alerts" },
    // A stock alert has no date: it holds as long as the stock stays low, so it reads "Now".
    LOW_STOCK: { screenKey: "nav.fertilizerInventory", path: "/plants/fertilizer-inventory", undated: true },
    NURSERY_READY: { screenKey: "nav.nursery", path: "/plants/nursery" },
    HARVEST_APPROACHING: { screenKey: "nav.harvestMaturity", path: "/plants/harvest-maturity" },
};

/** A type the API adds later: its code in sentence case, and the alerts screen. */
const OTHER_TYPE = { screenKey: "nav.diseasesAlerts", path: "/plants/diseases-alerts" };

/** The short wording and the screen of an alert type. */
export function alertTypeLabel(type) {
    const known = ALERT_TYPES[type];
    const { screenKey, path, undated } = known ?? OTHER_TYPE;
    const label = {
        issue: known ? t(`overview.alertTypes.${type}`) : formatCode(type),
        screen: t(screenKey),
        path,
    };
    if (undated) label.undated = t("overview.alertTypes.now");
    return label;
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
    if (alert.type === "LOW_STOCK" || (alert.fertilizerId != null && alert.blockCode == null)) {
        return t("overview.places.inventory");
    }
    if (alert.type === "NURSERY_READY") {
        return batchCode ? t("overview.places.batch", { code: batchCode }) : t("overview.places.nursery");
    }
    if (alert.blockCode == null) return t("overview.places.allBlocks");
    const block = formatBlock(alert.blockCode);
    return alert.type === "OPEN_HEALTH_ISSUE" && treeLabel
        ? t("overview.places.blockTree", { block, tree: treeLabel })
        : block;
}

/**
 * The days between today and an alert's date, as the API counts them:
 * "in 9 days", "today", "7 days ago". Null when the alert has no date.
 */
export function formatRelativeDays(days) {
    if (days === null || days === undefined || String(days).trim() === "") return null;
    const count = Number(days);
    if (!Number.isInteger(count)) return null;
    if (count === 0) return t("overview.relative.today");
    const options = { count: Math.abs(count), days: formatNumber(Math.abs(count)) };
    return count > 0 ? t("overview.relative.ahead", options) : t("overview.relative.ago", options);
}

/** The maturity statuses of the server (MaturityStatus), in its order. */
export const MATURITY_STATUSES = ["IMMATURE", "RIPE", "READY_FOR_HARVEST"];

/*
 * What each maturity status means, in the words of the server's definitions
 * (MaturityStatus): Ripe also holds the trees whose window is open while a
 * pre-harvest interval still runs, so 0 Ready for harvest beside 40 Ripe is
 * not a contradiction. The ring, its legend and the KPI share these words.
 */
export function maturityMeaning(status) {
    return t(`overview.maturity.meanings.${status}`);
}
