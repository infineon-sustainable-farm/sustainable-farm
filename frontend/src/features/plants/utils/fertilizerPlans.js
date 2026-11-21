import i18n from "../../../shared/i18n/i18n";
import { formatDaysAhead, formatQuantity, formatSignedPercent, formatSignedQuantity } from "./format";

/*
 * Rules and texts of the planned applications of fertilizer, kept apart from
 * their dialogs and tables so they can be tested.
 *
 * Nothing here computes a date or a delay: whether a plan is late, and by how
 * many days, comes from the API (`overdue`, `daysFromToday`), as does the day
 * the forms compare their dates with. No quantity is suggested either: every
 * quantity is the farmer's own figure.
 */

export { formatSignedPercent, formatSignedQuantity };

/** The states of a planned application, as the API names them. */
export const PLAN_STATUSES = ["PLANNED", "DONE", "CANCELLED"];

/** Same rule as the API: a quantity above 0 with at most 3 decimals. */
const QUANTITY_PATTERN = /^\d+(\.\d{1,3})?$/;

function t(key, options) {
    return i18n.t(`plants:fertilizer.plans.${key}`, options);
}

function message(key) {
    return t(`errors.${key}`);
}

function isQuantity(text) {
    const trimmed = String(text ?? "").trim();
    return QUANTITY_PATTERN.test(trimmed) && Number(trimmed) > 0;
}

/**
 * Errors of the form that plans an application, by field, in the active
 * language; empty when the form can be sent. The checks mirror the API rules.
 * The block is picked among the blocks of the farm referential, so only its
 * presence is checked here.
 *
 * `today` is the API's own day as "YYYY-MM-DD", or null while it is not
 * known: the date is then left to the API. `unreadableQuantity` is true when
 * the number field holds text it cannot read, such as "1e".
 */
export function validatePlan(values, { today = null, unreadableQuantity = false } = {}) {
    const errors = {};
    if (!values.fertilizerId) {
        errors.fertilizerId = message("fertilizerRequired");
    }
    if (!values.blockCode.trim()) {
        errors.blockCode = message("blockRequired");
    }
    if (!values.plannedDate) {
        errors.plannedDate = message("plannedDateRequired");
    } else if (today && values.plannedDate < today) {
        errors.plannedDate = message("plannedDatePast");
    }
    if (unreadableQuantity || !isQuantity(values.plannedQuantity)) {
        errors.plannedQuantity = message("quantityInvalid");
    }
    if (values.note.length > 255) {
        errors.note = message("noteTooLong");
    }
    return errors;
}

/**
 * Body of a new planned application, from the values of its form. The note
 * is optional and not sent when empty; no farm is sent, as for a planting:
 * the block gives it.
 */
export function planRequestBody(values) {
    const body = {
        fertilizerId: Number(values.fertilizerId),
        blockCode: values.blockCode,
        plannedDate: values.plannedDate,
        plannedQuantity: Number(String(values.plannedQuantity).trim()),
    };
    if (values.note.trim()) body.note = values.note;
    return body;
}

/**
 * Errors of the form that marks a planned application as done, by field.
 * Same conventions as validatePlan; the date applied cannot be after `today`.
 * The applicator is a person of the farm people directory.
 */
export function validateCompletion(values, { today = null, unreadableQuantity = false } = {}) {
    const errors = {};
    if (!values.applicationDate) {
        errors.applicationDate = message("dateRequired");
    } else if (today && values.applicationDate > today) {
        errors.applicationDate = message("dateFuture");
    }
    if (unreadableQuantity || !isQuantity(values.quantity)) {
        errors.quantity = message("quantityInvalid");
    }
    if (!values.applicatorPersonId) {
        errors.applicatorPersonId = message("applicatorRequired");
    }
    if (values.method.length > 255) {
        errors.method = message("methodTooLong");
    }
    return errors;
}

/** Body of the real application that completes a plan. The method is optional. */
export function completionRequestBody(values) {
    const body = {
        applicationDate: values.applicationDate,
        quantity: Number(String(values.quantity).trim()),
        applicatorPersonId: Number(values.applicatorPersonId),
    };
    if (values.method.trim()) body.method = values.method;
    return body;
}

const STATUS_TONES = { PLANNED: "warning", DONE: "success", CANCELLED: "neutral" };

/**
 * Badge of a planned application: its state, or "Overdue" when the API says
 * a planned one is late. An unknown state is shown as received.
 */
export function planBadge(plan) {
    if (plan.status === "PLANNED" && plan.overdue) return { tone: "error", label: t("status.OVERDUE") };
    const status = String(plan.status ?? "");
    return {
        tone: STATUS_TONES[status] ?? "neutral",
        label: t(`status.${status}`, { defaultValue: status || i18n.t("plants:common.none") }),
    };
}

/** Label of a status code in the status filter. */
export function planStatusLabel(status) {
    return t(`status.${status}`, { defaultValue: status });
}

/**
 * How far a planned application is, from the days the API counted: "in 10
 * days", "today", "3 days late". Null once the plan is done or cancelled,
 * when the API sends no delay.
 */
export function planDelayLabel(plan) {
    const days = plan.daysFromToday;
    if (plan.status !== "PLANNED" || days === null || days === undefined) return null;
    if (days > 0) return formatDaysAhead(days);
    if (days === 0) return t("delay.today");
    const late = Math.abs(days);
    const locale = { en: "en-US", de: "de-DE", fr: "fr-FR" }[i18n.resolvedLanguage] ?? "en-US";
    return t("delay.late", { count: late, days: late.toLocaleString(locale) });
}

/**
 * One line of the stock warning, from the figures of the API: what is
 * planned in the window against the stock, and what is missing.
 */
export function stockWarningText(warning, days) {
    const { unit } = warning;
    const text = t("stockWarning.line", {
        fertilizer: warning.fertilizerName,
        planned: formatQuantity(warning.plannedQuantity, unit),
        days,
        stock: formatQuantity(warning.currentStock, unit),
        shortfall: formatQuantity(warning.shortfall, unit),
    });
    return warning.overdueQuantity > 0
        ? `${text} ${t("stockWarning.overdue", { quantity: formatQuantity(warning.overdueQuantity, unit) })}`
        : text;
}
