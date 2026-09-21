import StatusBadge from "./StatusBadge";
import { formatDateShort, formatTime, formatTimeRange } from "../utils/format";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";

const TINY_BUTTON =
    "rounded border border-[#C3D8D4] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-primary hover:bg-[#ECFAF7] disabled:opacity-60";
const TINY_DANGER =
    "rounded border border-[#EDC9C9] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-error hover:bg-[#FBEAEA] disabled:opacity-60";

const COLUMNS = ["Name", "Group", "Date / Slot", "Language", "Type", "Status", "Actions"];

/*
 * Which transitions the backend accepts, per current status: approve and
 * reject only from PENDING, check-in only from CONFIRMED. Showing any other
 * button would guarantee a 422, so the table never offers it.
 */
function rowActions(status) {
    if (status === "PENDING") return ["approve", "reject"];
    if (status === "CONFIRMED") return ["check-in"];
    return [];
}

function ActionButtons({ registration, pendingAction, onAction }) {
    const actions = rowActions(registration.status);
    return (
        <div className="flex flex-wrap gap-1.5">
            {actions.map((action) => {
                const isPending = pendingAction?.id === registration.id;
                return (
                    <button
                        key={action}
                        type="button"
                        disabled={isPending}
                        onClick={() => onAction(registration, action)}
                        className={action === "reject" ? TINY_DANGER : TINY_BUTTON}
                    >
                        {action === "approve" && "✓ approve"}
                        {action === "reject" && "✗ reject"}
                        {action === "check-in" && "☑ check-in"}
                    </button>
                );
            })}
        </div>
    );
}

/**
 * The mockup's "Registered visitors" table. Language and type are not part of
 * a registration payload, so they are joined from the visitors list by id; a
 * registration without a visitor (the API allows it) shows dashes.
 */
export default function RegistrationsTable({
    registrations,
    visitorsById,
    slotsById,
    pendingAction,
    actionError,
    onAction,
    onEdit,
}) {
    return (
        <div className="overflow-x-auto">
            <table className="w-full border-collapse text-sm">
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
                    {registrations.length === 0 && (
                        <tr>
                            <td colSpan={COLUMNS.length} className="px-3 py-8 text-center text-muted">
                                No registrations yet.
                            </td>
                        </tr>
                    )}
                    {registrations.map((registration) => {
                        const visitor = visitorsById.get(registration.visitorId);
                        const slot = slotsById.get(registration.timeSlotId);
                        const isPending = pendingAction?.id === registration.id;
                        const failed = actionError?.id === registration.id;
                        return (
                            <tr key={registration.id} className="border-b border-line align-top">
                                <td className="px-3 py-2.5 font-medium text-ink">
                                    {registration.visitorName ?? "—"}
                                </td>
                                <td className="px-3 py-2.5 text-ink">{registration.groupSize ?? "—"}</td>
                                <td className="px-3 py-2.5 whitespace-nowrap text-ink">
                                    {formatDateShort(registration.slotDate)}
                                    {" · "}
                                    {slot
                                        ? formatTimeRange(slot.startTime, slot.endTime)
                                        : formatTime(registration.slotStart)}
                                </td>
                                <td className="px-3 py-2.5 text-ink">{visitor?.language ?? "—"}</td>
                                <td className="px-3 py-2.5 text-ink">
                                    {visitor?.type ? formatEnumLabel(visitor.type) : "—"}
                                </td>
                                <td className="px-3 py-2.5">
                                    <StatusBadge value={registration.status} />
                                </td>
                                <td className="px-3 py-2.5">
                                    <div className="flex flex-wrap items-center gap-1.5">
                                        <ActionButtons
                                            registration={registration}
                                            pendingAction={pendingAction}
                                            onAction={onAction}
                                        />
                                        <button
                                            type="button"
                                            disabled={isPending || !registration.visitorId}
                                            onClick={() => onEdit(registration)}
                                            className={TINY_BUTTON}
                                        >
                                            ✎ edit
                                        </button>
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