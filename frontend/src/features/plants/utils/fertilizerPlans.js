/*
 * Rules and texts of the planned applications of fertilizer, kept apart from
 * their dialogs and tables so they can be tested.
 *
 * Nothing here computes a date or a delay: whether a plan is late, and by how
 * many days, comes from the API (`overdue`, `daysFromToday`), as does the day
 * the forms compare their dates with. No quantity is suggested either: every
 * quantity is the farmer's own figure.
 */
import { formatDaysAhead, formatQuantity, formatUnit, NO_VALUE } from "./format";

/** The states of a planned application, as the API names them. */
export const PLAN_STATUSES = ["PLANNED", "DONE", "CANCELLED"];

/** Same rule as the API: a quantity above 0 with at most 3 decimals. */
const QUANTITY_PATTERN = /^\d+(\.\d{1,3})?$/;

/** Same rule as a planting: a short code such as "A" or "B2"; spaces around it are removed. */
const BLOCK_CODE_PATTERN = /^\s*[A-Za-z0-9]{1,10}\s*$/;

const QUANTITY_MESSAGE = "Quantity must be a number greater than 0, with at most 3 decimals.";

function isQuantity(text) {
    const trimmed = String(text ?? "").trim();
    return QUANTITY_PATTERN.test(trimmed) && Number(trimmed) > 0;
}

/**
 * Errors of the form that plans an application, by field; empty when the
 * form can be sent. The checks mirror the API rules.
 *
 * `today` is the API's own day as "YYYY-MM-DD", or null while it is not
 * known: the date is then left to the API. `unreadableQuantity` is true when
 * the number field holds text it cannot read, such as "1e".
 */
export function validatePlan(values, { today = null, unreadableQuantity = false } = {}) {
    const errors = {};
    if (!values.fertilizerId) {
        errors.fertilizerId = "Fertilizer is required.";
    }
    if (!values.blockCode.trim()) {
        errors.blockCode = "Block is required.";
    } else if (!BLOCK_CODE_PATTERN.test(values.blockCode)) {
        errors.blockCode = "Block must be a short code such as A or B2, without prefix or space.";
    }
    if (!values.plannedDate) {
        errors.plannedDate = "Planned date is required.";
    } else if (today && values.plannedDate < today) {
        errors.plannedDate = "Planned date cannot be in the past.";
    }
    if (unreadableQuantity || !isQuantity(values.plannedQuantity)) {
        errors.plannedQuantity = QUANTITY_MESSAGE;
    }
    if (values.note.length > 255) {
        errors.note = "Note must be at most 255 characters.";
    }
    return errors;
}

/**
 * Body of a new planned application, from the values of its form. The note
 * is optional and not sent when empty; no farm is sent, as for a planting.
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
 */
export function validateCompletion(values, { today = null, unreadableQuantity = false } = {}) {
    const errors = {};
    if (!values.applicationDate) {
        errors.applicationDate = "Date is required.";
    } else if (today && values.applicationDate > today) {
        errors.applicationDate = "Date cannot be in the future.";
    }
    if (unreadableQuantity || !isQuantity(values.quantity)) {
        errors.quantity = QUANTITY_MESSAGE;
    }
    if (!values.applicator.trim()) {
        errors.applicator = "Applicator is required.";
    } else if (values.applicator.length > 255) {
        errors.applicator = "Applicator must be at most 255 characters.";
    }
    if (values.method.length > 255) {
        errors.method = "Method must be at most 255 characters.";
    }
    return errors;
}

/** Body of the real application that completes a plan. The method is optional. */
export function completionRequestBody(values) {
    const body = {
        applicationDate: values.applicationDate,
        quantity: Number(String(values.quantity).trim()),
        applicator: values.applicator,
    };
    if (values.method.trim()) body.method = values.method;
    return body;
}

/**
 * Badge of a planned application: its state, or "Overdue" when the API says
 * a planned one is late. An unknown state is shown as received.
 */
export function planBadge(plan) {
    if (plan.status === "PLANNED" && plan.overdue) return { tone: "error", label: "Overdue" };
    switch (plan.status) {
        case "PLANNED":
            return { tone: "warning", label: "Planned" };
        case "DONE":
            return { tone: "success", label: "Done" };
        case "CANCELLED":
            return { tone: "neutral", label: "Cancelled" };
        default:
            return { tone: "neutral", label: String(plan.status ?? NO_VALUE) };
    }
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
    if (days === 0) return "today";
    const late = Math.abs(days);
    return `${late.toLocaleString("en-US")} ${late === 1 ? "day" : "days"} late`;
}

/** A gap in the unit of its fertilizer, with its sign: "+5 kg", "-20 kg", "0 L". */
export function formatSignedQuantity(value, unit) {
    if (value === null || value === undefined) return NO_VALUE;
    const number = Number(value).toLocaleString("en-US", { signDisplay: "exceptZero" });
    return `${number} ${formatUnit(unit)}`;
}

/** A gap as the percentage the API computed, with its sign: "-33.3%". Null when nothing was planned. */
export function formatSignedPercent(value) {
    if (value === null || value === undefined) return null;
    return `${Number(value).toLocaleString("en-US", { signDisplay: "exceptZero", maximumFractionDigits: 1 })}%`;
}

/**
 * One line of the stock warning, from the figures of the API: what is
 * planned in the window against the stock, and what is missing.
 */
export function stockWarningText(warning, days) {
    const { unit } = warning;
    const text = `${warning.fertilizerName}: ${formatQuantity(warning.plannedQuantity, unit)} planned in the next `
        + `${days} days, ${formatQuantity(warning.currentStock, unit)} in stock, `
        + `${formatQuantity(warning.shortfall, unit)} short.`;
    return warning.overdueQuantity > 0
        ? `${text} ${formatQuantity(warning.overdueQuantity, unit)} of it is overdue.`
        : text;
}
