import { Ban, Pencil } from "lucide-react";
import { useTranslation } from "react-i18next";
import { IconButton } from "../../../shared/ui";
import { toIsoDate, useVisitorFormat } from "../utils/format";
import { useSlotRegistrations } from "../hooks/useScheduling";

/*
 * Cancelled slots are hidden from the grid: the cell becomes free again and a new slot can be
 * created there. The backend keeps the cancelled row for the audit trail.
 */
const HIDDEN_STATUSES = new Set(["CANCELLED"]);

const CELL = "border-b border-border-subtle p-3 align-top";

function isSlotFull(slot) {
    return slot.status === "FULL" || slot.booked >= slot.maxCapacity;
}

function BookedPill({ slot }) {
    const { t } = useTranslation("visitormanagement");
    const isFull = isSlotFull(slot);
    return (
        <span
            className={`inline-block rounded-pill px-2 py-0.5 text-xs font-semibold ${
                isFull ? "bg-error-bg text-error-fg" : "bg-primary-tint text-primary"
            }`}
        >
            {isFull
                ? t("scheduling.full")
                : t("scheduling.bookedRatio", { booked: slot.booked, capacity: slot.maxCapacity })}
        </span>
    );
}

/*
 * One calendar cell. It owns its registrations query (only fired when the slot actually has
 * bookings); the cancellation is confirmed by the page.
 */
function SlotCell({ slot, onEdit, onCancel, cancelPendingId }) {
    const { t } = useTranslation("visitormanagement");
    const { data: registrations, isPending: isLoadingRegistrations } = useSlotRegistrations(
        slot.id,
        slot.booked > 0,
    );

    const activeRegistrations = (registrations ?? []).filter(
        (registration) => registration.status !== "REJECTED" && registration.status !== "CANCELLED",
    );

    const isCancelling = cancelPendingId === slot.id;

    return (
        <td className={CELL}>
            <div className="flex items-start justify-between gap-1">
                <BookedPill slot={slot} />
                {!isSlotFull(slot) && (
                    <span className="flex">
                        <IconButton
                            icon={Pencil}
                            label={t("scheduling.edit")}
                            onClick={() => onEdit(slot)}
                            disabled={isCancelling}
                        />
                        <IconButton
                            icon={Ban}
                            tone="danger"
                            label={isCancelling ? t("scheduling.cancelling") : t("scheduling.cancel")}
                            onClick={() => onCancel(slot)}
                            disabled={isCancelling}
                        />
                    </span>
                )}
            </div>

            <span className="mt-2 block text-xs leading-snug text-text-muted">
                {slot.booked === 0 && t("scheduling.noVisitors")}
                {slot.booked > 0 && isLoadingRegistrations && t("scheduling.loadingVisitors")}
                {slot.booked > 0 &&
                    !isLoadingRegistrations &&
                    activeRegistrations.slice(0, 3).map((registration) => (
                        <span key={registration.id} className="block truncate">
                            {t("scheduling.visitorLine", {
                                name: registration.visitorName,
                                size: registration.groupSize,
                            })}
                        </span>
                    ))}
                {activeRegistrations.length > 3 && (
                    <span className="block">
                        {t("scheduling.more", { count: activeRegistrations.length - 3 })}
                    </span>
                )}
            </span>
        </td>
    );
}

/**
 * The week grid: one column per opening day (Monday to Saturday) and one row per slot time. Rows
 * come from the slots actually stored in the week, plus the two canonical farm slots, so an empty
 * week still shows the grid staff will fill.
 */
export default function SchedulingGrid({ rows, days, slotsByCell, onEdit, onCancel, cancelPendingId }) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const todayIso = toIsoDate(new Date());

    return (
        <div className="overflow-x-auto rounded-card border border-border-subtle bg-surface-raised shadow-raised">
            <table className="w-full border-separate border-spacing-0 text-sm">
                <caption className="sr-only">{t("scheduling.title")}</caption>
                <thead>
                    <tr>
                        <th scope="col" className="w-20 border-b border-border-divider px-3 py-2 text-left">
                            <span className="sr-only">{t("scheduling.timeSlot")}</span>
                        </th>
                        {days.map((day) => {
                            const iso = toIsoDate(day);
                            const isToday = iso === todayIso;
                            return (
                                <th
                                    key={iso}
                                    scope="col"
                                    className="border-b border-border-divider px-3 py-2 text-center font-normal text-text-muted"
                                >
                                    <span className="block text-xs font-semibold uppercase">
                                        {format.weekdayShort(day)}
                                    </span>
                                    <span
                                        className={`mt-0.5 inline-block rounded-pill px-2 py-0.5 text-xs font-semibold ${
                                            isToday ? "bg-primary text-on-primary" : "bg-neutral-bg text-neutral-fg"
                                        }`}
                                    >
                                        {day.getDate()}
                                    </span>
                                </th>
                            );
                        })}
                    </tr>
                </thead>
                <tbody>
                    {rows.map((row) => (
                        <tr key={row.key} className="hover:bg-surface-muted">
                            <th
                                scope="row"
                                className="border-b border-border-subtle px-3 py-3 text-center text-xs font-semibold whitespace-nowrap text-text"
                            >
                                {format.timeRange(row.startTime, row.endTime)}
                            </th>
                            {days.map((day) => {
                                const slot = slotsByCell.get(`${toIsoDate(day)}|${row.key}`);
                                if (!slot || HIDDEN_STATUSES.has(slot.status)) {
                                    return (
                                        <td
                                            key={`${toIsoDate(day)}|${row.key}`}
                                            className={`${CELL} text-center text-text-muted`}
                                        >
                                            {format.none}
                                        </td>
                                    );
                                }
                                return (
                                    <SlotCell
                                        key={slot.id}
                                        slot={slot}
                                        onEdit={onEdit}
                                        onCancel={onCancel}
                                        cancelPendingId={cancelPendingId}
                                    />
                                );
                            })}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
