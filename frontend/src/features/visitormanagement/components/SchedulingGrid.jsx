import { useState } from "react";
import { formatTimeRange, toIsoDate } from "../utils/format";
import { useSlotRegistrations } from "../hooks/useScheduling";

/*
 * Cancelled slots are hidden from the grid: the cell becomes free again and a
 * new slot can be created there. The backend keeps the cancelled row for the
 * audit trail.
 */
const HIDDEN_STATUSES = new Set(["CANCELLED"]);

const CELL_BORDER = "border-r border-b border-line p-2 align-top";

function BookedPill({ slot }) {
    const isFull = slot.status === "FULL" || slot.booked >= slot.maxCapacity;
    return (
        <span
            className={`inline-block rounded px-1.5 py-0.5 text-[11px] font-semibold ${
                isFull
                    ? "border border-error bg-error/10 text-error"
                    : "border border-primary bg-[#DCF3EF] text-primary-dark"
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

    const isCancelling = cancelPendingId === slot.id;
    const cancelFailed = cancelError?.slotId === slot.id;

    return (
        <td className={CELL_BORDER}>
            <BookedPill slot={slot} />

            <span className="mt-1 block text-[10px] leading-snug text-muted">
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

            {confirming ? (
                <span className="mt-1.5 flex flex-wrap items-center gap-1">
                    <span className="text-[10px] text-error">Cancel slot?</span>
                    <button
                        type="button"
                        disabled={isCancelling}
                        onClick={() => {
                            setConfirming(false);
                            onCancel(slot);
                        }}
                        className="rounded border border-error px-1.5 py-0.5 text-[11px] font-semibold text-error disabled:opacity-60"
                    >
                        {isCancelling ? "…" : "Yes"}
                    </button>
                    <button
                        type="button"
                        onClick={() => setConfirming(false)}
                        className="rounded border border-gray-300 px-1.5 py-0.5 text-[11px] text-gray-600"
                    >
                        No
                    </button>
                </span>
            ) : (
                <span className="mt-1.5 flex flex-wrap gap-1">
                    <button
                        type="button"
                        onClick={() => onEdit(slot)}
                        disabled={isCancelling}
                        className="rounded border border-[#C3D8D4] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-primary hover:bg-[#ECFAF7] disabled:opacity-60"
                    >
                        edit
                    </button>
                    <button
                        type="button"
                        onClick={() => setConfirming(true)}
                        disabled={isCancelling}
                        className="rounded border border-[#EDC9C9] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-error hover:bg-[#FBEAEA] disabled:opacity-60"
                    >
                        {isCancelling ? "cancelling…" : "cancel"}
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
 * The mockup's week grid: one column per opening day (Monday–Saturday) and one
 * row per slot time. Rows come from the slots actually stored in the week,
 * plus the two canonical farm slots, so an empty week still shows the grid
 * staff will fill.
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
    return (
        <div className="overflow-x-auto">
            <table className="w-full border-collapse text-xs">
                <thead>
                    <tr>
                        <th className="w-20 border-t border-l border-line bg-primary-dark px-2 py-2 text-center text-[11px] font-medium text-white">
                            <span className="sr-only">Time slot</span>
                        </th>
                        {days.map((day) => (
                            <th
                                key={toIsoDate(day)}
                                className="border-t border-r border-line bg-primary-dark px-2 py-2 text-center text-[11px] font-medium text-white"
                            >
                                {day.toLocaleDateString("en-GB", { weekday: "short" })}
                                <span className="block text-[10px] font-normal text-white/70">
                                    {day.getDate()}
                                </span>
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {rows.map((row) => (
                        <tr key={row.key}>
                            <th className="border-r border-b border-line bg-white px-2 py-2 text-center text-[11px] font-semibold whitespace-nowrap text-primary-dark">
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