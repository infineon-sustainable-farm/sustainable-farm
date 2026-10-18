import { useState } from "react";
import { formatTimeRange, toIsoDate } from "../utils/format";
import { useSlotRegistrations } from "../hooks/useScheduling";

/*
 * Cancelled slots are hidden from the grid: the cell becomes free again and a
 * new slot can be created there. The backend keeps the cancelled row for the
 * audit trail.
 */
const HIDDEN_STATUSES = new Set(["CANCELLED"]);

/*
 * A modern week grid: day header sticks to the top while the page scrolls,
 * only horizontal separators (no dense grid), a primary-dark band for the time
 * column and a soft highlight for the current day. Data and behaviour are the
 * same as the original table.
 */
const CELL_BORDER = "border-b border-r border-line p-3 align-top";

function BookedPill({ slot }) {
    const isFull = slot.status === "FULL" || slot.booked >= slot.maxCapacity;
    return (
        <span
            className={`inline-block rounded-md px-2 py-1 text-[11px] font-semibold ${
                isFull
                    ? "border border-error/30 bg-[#FBEAEA] text-error"
                    : "border border-primary/30 bg-[#DCF3EF] text-primary-dark"
            }`}
        >
            {isFull ? "Full" : `${slot.booked}/${slot.maxCapacity}`}
        </span>
    );
}

/*
 * One calendar cell. It owns its registrations query (only fired when the slot
 * actually has bookings), its confirm state and the display of a cancel error
 * targeted at this slot, so a failure in one cell never leaks into another.
 */
function SlotCell({ slot, onEdit, onCancel, cancelPendingId, cancelError }) {
    const [confirming, setConfirming] = useState(false);
    const { data: registrations, isPending: isLoadingRegistrations } = useSlotRegistrations(
        slot.id,
        slot.booked > 0,
    );

    const activeRegistrations = (registrations ?? []).filter(
        (registration) =>
            registration.status !== "REJECTED" && registration.status !== "CANCELLED",
    );

    const isFull = slot.status === "FULL" || slot.booked >= slot.maxCapacity;
    const isCancelling = cancelPendingId === slot.id;
    const cancelFailed = cancelError?.slotId === slot.id;

    return (
        <td className={`${CELL_BORDER} group`}>
            <div className="flex items-start justify-between gap-1.5">
                <BookedPill slot={slot} />
                {!confirming && !isFull && (
                    <span className="flex gap-1">
                        <button
                            type="button"
                            onClick={() => onEdit(slot)}
                            disabled={isCancelling}
                            className="rounded-md border border-line bg-white px-1.5 py-0.5 text-[11px] font-semibold text-primary hover:border-primary hover:bg-[#ECFAF7] disabled:opacity-60"
                        >
                            edit
                        </button>
                        <button
                            type="button"
                            onClick={() => setConfirming(true)}
                            disabled={isCancelling}
                            className="rounded-md border border-line bg-white px-1.5 py-0.5 text-[11px] font-semibold text-error hover:border-error hover:bg-[#FBEAEA] disabled:opacity-60"
                        >
                            {isCancelling ? "cancelling…" : "cancel"}
                        </button>
                    </span>
                )}
            </div>

            <span className="mt-2 block text-[10px] leading-snug text-muted">
                {slot.booked === 0 && "No visitors yet"}
                {slot.booked > 0 && isLoadingRegistrations && "Loading visitors…"}
                {slot.booked > 0 &&
                    !isLoadingRegistrations &&
                    activeRegistrations.slice(0, 3).map((registration) => (
                        <span key={registration.id} className="block truncate">
                            {registration.visitorName} ({registration.groupSize})
                        </span>
                    ))}
                {activeRegistrations.length > 3 && (
                    <span className="block">+{activeRegistrations.length - 3} more</span>
                )}
            </span>

            {confirming && (
                <span className="mt-2 flex flex-wrap items-center gap-1.5">
                    <span className="text-[10px] text-error">Cancel slot?</span>
                    <button
                        type="button"
                        disabled={isCancelling}
                        onClick={() => {
                            setConfirming(false);
                            onCancel(slot);
                        }}
                        className="rounded-md border border-error px-1.5 py-0.5 text-[11px] font-semibold text-error hover:bg-[#FBEAEA] disabled:opacity-60"
                    >
                        {isCancelling ? "…" : "Yes"}
                    </button>
                    <button
                        type="button"
                        onClick={() => setConfirming(false)}
                        className="rounded-md border border-line px-1.5 py-0.5 text-[11px] text-muted hover:bg-[#F4F9F8]"
                    >
                        No
                    </button>
                </span>
            )}

            {cancelFailed && (
                <span className="mt-1 block text-[10px] text-error">{cancelError.message}</span>
            )}
        </td>
    );
}

/**
 * The mockup's week grid, modernised: one column per opening day
 * (Monday–Saturday) and one row per slot time. Rows come from the slots
 * actually stored in the week, plus the two canonical farm slots, so an empty
 * week still shows the grid staff will fill.
 */
export default function SchedulingGrid({
    rows,
    days,
    slotsByCell,
    onEdit,
    onCancel,
    cancelPendingId,
    cancelError,
}) {
    const todayIso = toIsoDate(new Date());

    return (
        <div className="overflow-x-auto rounded-xl border border-line bg-white shadow-sm">
            <table className="vm-table w-full border-separate border-spacing-0 text-xs">
                <thead>
                    <tr>
                        <th className="sticky top-0 z-10 w-20 border-r border-line bg-primary-dark px-3 py-2.5 text-left text-[11px] font-semibold tracking-wide text-white">
                            <span className="sr-only">Time slot</span>
                        </th>
                        {days.map((day) => {
                            const iso = toIsoDate(day);
                            const isToday = iso === todayIso;
                            return (
                                <th
                                    key={iso}
                                    className="sticky top-0 z-10 border-r border-line bg-primary-dark px-3 py-2.5 text-center"
                                >
                                    <span className="block text-[11px] font-semibold tracking-wide text-white uppercase">
                                        {day.toLocaleDateString("en-GB", { weekday: "short" })}
                                    </span>
                                    <span
                                        className={`mt-0.5 inline-block rounded-full px-1.5 py-0.5 text-[10px] font-semibold ${
                                            isToday
                                                ? "bg-accent text-white"
                                                : "bg-white/15 text-white/80"
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
                        <tr key={row.key} className="hover:bg-[#F4F9F8]">
                            <th className="border-r border-b border-line bg-primary-dark/95 px-3 py-3 text-center text-[11px] font-semibold tracking-wide whitespace-nowrap text-white uppercase">
                                {formatTimeRange(row.startTime, row.endTime)}
                            </th>
                            {days.map((day) => {
                                const slot = slotsByCell.get(`${toIsoDate(day)}|${row.key}`);
                                if (!slot || HIDDEN_STATUSES.has(slot.status)) {
                                    return (
                                        <td
                                            key={`${toIsoDate(day)}|${row.key}`}
                                            className={`${CELL_BORDER} text-center text-muted`}
                                        >
                                            —
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
                                        cancelError={cancelError}
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