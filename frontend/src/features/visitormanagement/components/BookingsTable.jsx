import StatusBadge from "./StatusBadge";
import { formatDateShort, formatTimeRange } from "../utils/format";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";

const TINY_BUTTON =
    "rounded border border-[#C3D8D4] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-primary hover:bg-[#ECFAF7] disabled:opacity-60";
const TINY_DANGER =
    "rounded border border-[#EDC9C9] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-error hover:bg-[#FBEAEA] disabled:opacity-60";

const COLUMNS = ["Ref", "Activity", "Date / Slot", "People", "Payment", "Status", "Actions"];

/*
 * The transitions the API accepts, per row. A booking must be PAID before it
 * can be confirmed, so an unpaid pending booking offers "pay", not "confirm" —
 * offering confirm would guarantee a 422. Completing requires CONFIRMED, and
 * cancelling is only possible while the booking is still active.
 */
function rowActions(booking) {
    const actions = [];
    const active = booking.status === "PENDING" || booking.status === "CONFIRMED";
    if (active && booking.paymentStatus === "UNPAID") actions.push("pay");
    if (booking.status === "PENDING" && booking.paymentStatus === "PAID") actions.push("confirm");
    if (booking.status === "CONFIRMED") actions.push("complete");
    if (active) actions.push("cancel");
    if (booking.status !== "COMPLETED" && booking.status !== "CANCELLED") actions.push("edit");
    return actions;
}

function paymentLabel(booking) {
    const method = booking.paymentMethod ? formatEnumLabel(booking.paymentMethod) : "—";
    const status = booking.paymentStatus ? formatEnumLabel(booking.paymentStatus) : "";
    return status ? `${method} · ${status}` : method;
}

export default function BookingsTable({
    bookings,
    pendingAction,
    actionError,
    onAction,
    onEdit,
}) {
    return (
        <div className="overflow-x-auto">
            <table className="vm-table w-full border-collapse text-sm">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th
                                key={column}
                                scope="col"
                                className="border-b-2 border-line bg-[#F4F8F8] px-3 py-2.5 text-left text-xs font-semibold tracking-wide whitespace-nowrap text-ink uppercase"
                            >
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {bookings.length === 0 && (
                        <tr>
                            <td colSpan={COLUMNS.length} className="px-3 py-8 text-center text-muted">
                                No bookings yet.
                            </td>
                        </tr>
                    )}
                    {bookings.map((booking) => {
                        const isPending = pendingAction?.id === booking.id;
                        const failed = actionError?.id === booking.id;
                        return (
                            <tr key={booking.id} className="border-b border-line align-top">
                                <td className="px-3 py-2.5 font-medium whitespace-nowrap text-ink">
                                    {booking.reference}
                                </td>
                                <td className="px-3 py-2.5 text-ink">{booking.activityName}</td>
                                <td className="px-3 py-2.5 whitespace-nowrap text-ink">
                                    {formatDateShort(booking.slotDate)}
                                    {" · "}
                                    {formatTimeRange(booking.slotStartTime, booking.slotEndTime)}
                                </td>
                                <td className="px-3 py-2.5 text-ink">{booking.peopleCount}</td>
                                <td className="px-3 py-2.5 whitespace-nowrap text-ink">
                                    {paymentLabel(booking)}
                                </td>
                                <td className="px-3 py-2.5">
                                    <StatusBadge value={booking.status} />
                                </td>
                                <td className="px-3 py-2.5">
                                    <div className="flex flex-wrap items-center gap-1.5">
                                        {rowActions(booking).map((action) => (
                                            <button
                                                key={action}
                                                type="button"
                                                disabled={isPending}
                                                onClick={() =>
                                                    action === "edit"
                                                        ? onEdit(booking)
                                                        : onAction(booking, action)
                                                }
                                                className={
                                                    action === "cancel" ? TINY_DANGER : TINY_BUTTON
                                                }
                                            >
                                                {action === "pay" && "₣ pay"}
                                                {action === "confirm" && "✓ confirm"}
                                                {action === "complete" && "☑ complete"}
                                                {action === "cancel" && "✕ cancel"}
                                                {action === "edit" && "✎ edit"}
                                            </button>
                                        ))}
                                    </div>
                                    {failed && (
                                        <p className="mt-1 max-w-[260px] text-[11px] text-error">
                                            {actionError.message}
                                        </p>
                                    )}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}