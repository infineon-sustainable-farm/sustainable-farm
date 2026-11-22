import { i18n } from "../../../shared/i18n";

// Numbers, dates and units follow the language chosen in the shell (en, de, fr).
const LOCALES = { en: "en-US", de: "de-DE", fr: "fr-FR" };

export const EMPTY = "—";

export function currentLocale() {
    return LOCALES[i18n.resolvedLanguage] ?? LOCALES.en;
}

function format(value, options) {
    return new Intl.NumberFormat(currentLocale(), options).format(Number(value));
}

function isBlank(value) {
    return value == null || value === "" || !Number.isFinite(Number(value));
}

export function formatNumber(value) {
    return isBlank(value) ? EMPTY : format(value, { maximumFractionDigits: 2 });
}

/**
 * A percentage rounded to 2 decimals, unless the rounding would move it across a capacity
 * threshold (70 and 90 %): 69.999 stays 69.999 so the text matches its colour.
 */
export function formatPercentage(value) {
    if (isBlank(value)) return EMPTY;
    const rounded = Math.round(Number(value) * 100) / 100;
    return capacityLevel(rounded) === capacityLevel(Number(value))
        ? format(value, { maximumFractionDigits: 2 })
        : format(value, { maximumFractionDigits: 16 });
}

export function formatQuantity(value) {
    return isBlank(value) ? EMPTY : `${formatNumber(value)} kg`;
}

export function formatMoney(value) {
    return isBlank(value) ? EMPTY : `${formatNumber(value)} FCFA`;
}

/** Local date and time of an ISO value (2026-11-20T10:00:00), or the empty mark. */
export function formatDateTime(value) {
    if (!value) return EMPTY;
    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? EMPTY
        : date.toLocaleString(currentLocale(), { dateStyle: "short", timeStyle: "short" });
}

export function formatDate(value) {
    if (!value) return EMPTY;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? EMPTY : date.toLocaleDateString(currentLocale());
}

/** Capacity usage level: below 70 %, 70 to 90 %, above 90 %, or unknown. */
export function capacityLevel(percent) {
    if (!Number.isFinite(percent)) return "unknown";
    if (percent < 70) return "ok";
    if (percent <= 90) return "high";
    return "full";
}

/** Colour token of a capacity level (the text next to it always names the value). */
export const CAPACITY_COLORS = {
    ok: "var(--color-success-fg)",
    high: "var(--color-warning-fg)",
    full: "var(--color-error-fg)",
    unknown: "var(--color-border-subtle)",
};

/** Tailwind background class of a capacity level, for the usage bars. */
export const CAPACITY_BAR_CLASSES = {
    ok: "bg-success-fg",
    high: "bg-warning-fg",
    full: "bg-error-fg",
    unknown: "bg-border-subtle",
};
