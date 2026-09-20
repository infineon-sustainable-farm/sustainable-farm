import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";

/*
 * Colour mapping per raw domain value, shared by every screen of the module as
 * its statuses grow. Entries are added per screen: an unknown value falls back
 * to a neutral grey instead of pretending it is a known state.
 */
const STATUS_COLORS = {
    PENDING: "bg-warning/15 text-warning",
    DRAFT: "bg-gray-100 text-gray-600",
    PUBLISHED: "bg-success/15 text-success",
    ACTIVE: "bg-success/15 text-success",
    CONFIRMED: "bg-success/15 text-success",
    COMPLETED: "bg-info/15 text-info",
    CHECKED_IN: "bg-info/15 text-info",
    REJECTED: "bg-error/15 text-error",
    CANCELLED: "bg-error/15 text-error",
    PAID: "bg-success/15 text-success",
    UNPAID: "bg-warning/15 text-warning",
    AVAILABLE: "bg-success/15 text-success",
    RESERVED: "bg-info/15 text-info",
    FULL: "bg-warning/15 text-warning",
    SENT: "bg-info/15 text-info",
    DONE: "bg-success/15 text-success",
};

const FALLBACK_CLASS = "bg-gray-100 text-gray-600";

/**
 * A small rounded pill for a status value: "CHECKED_IN" renders as
 * "Checked in" with the module's colour for that state. The raw value is
 * never guessed from anything else — it is exactly what the API returns.
 */
export default function StatusBadge({ value, className = "" }) {
    if (!value) return null;

    const tone = STATUS_COLORS[value] ?? FALLBACK_CLASS;
    return (
        <span
            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${tone} ${className}`}
        >
            {formatEnumLabel(value)}
        </span>
    );
}