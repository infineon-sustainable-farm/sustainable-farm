import { useState } from "react";
import StatusBadge from "./StatusBadge";
import { formatFcfa, formatNumber } from "../utils/format";

const TINY_BUTTON =
    "rounded border border-[#C3D8D4] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-primary hover:bg-[#ECFAF7] disabled:opacity-60";
const TINY_DANGER =
    "rounded border border-[#EDC9C9] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-error hover:bg-[#FBEAEA] disabled:opacity-60";

const COLUMNS = ["Activity", "Price", "Capacity", "Duration", "Status", "Actions"];

/**
 * One row's actions. Deactivation is irreversible through the API (there is no
 * active flag on the update payload) and cancels the activity's bookings, so it
 * asks for confirmation and spells the consequence out.
 */
function ActivityActions({ activity, isPending, onAction, onEdit }) {
    const [confirming, setConfirming] = useState(false);

    if (confirming) {
        return (
            <div className="flex flex-wrap items-center gap-1.5">
                <span className="text-[11px] text-error">
                    Deactivate and cancel its bookings?
                </span>
                <button
                    type="button"
                    disabled={isPending}
                    onClick={() => {
                        setConfirming(false);
                        onAction(activity);
                    }}
                    className={TINY_DANGER}
                >
                    {isPending ? "…" : "Yes"}
                </button>
                <button
                    type="button"
                    onClick={() => setConfirming(false)}
                    className={TINY_BUTTON}
                >
                    No
                </button>
            </div>
        );
    }

    return (
        <div className="flex flex-wrap items-center gap-1.5">
            <button
                type="button"
                disabled={isPending}
                onClick={() => onEdit(activity)}
                className={TINY_BUTTON}
            >
                ✎ edit
            </button>
            {activity.active && (
                <button
                    type="button"
                    disabled={isPending}
                    onClick={() => setConfirming(true)}
                    className={TINY_DANGER}
                >
                    {isPending ? "deactivating…" : "✕ deactivate"}
                </button>
            )}
        </div>
    );
}

export default function ActivitiesTable({
    activities,
    pendingDeactivateId,
    deactivateError,
    onDeactivate,
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
                    {activities.length === 0 && (
                        <tr>
                            <td colSpan={COLUMNS.length} className="px-3 py-8 text-center text-muted">
                                No activity yet.
                            </td>
                        </tr>
                    )}
                    {activities.map((activity) => {
                        const isPending = pendingDeactivateId === activity.id;
                        const failed = deactivateError?.id === activity.id;
                        return (
                            <tr key={activity.id} className="border-b border-line align-top">
                                <td className="px-3 py-2.5 font-medium text-ink">
                                    {activity.name}
                                    {activity.description && (
                                        <span className="mt-0.5 block text-xs text-muted">
                                            {activity.description}
                                        </span>
                                    )}
                                </td>
                                <td className="px-3 py-2.5 whitespace-nowrap text-ink">
                                    {formatFcfa(activity.price)}
                                </td>
                                <td className="px-3 py-2.5 text-ink">
                                    {formatNumber(activity.capacity)}
                                </td>
                                <td className="px-3 py-2.5 whitespace-nowrap text-ink">
                                    {activity.durationMinutes} min
                                </td>
                                <td className="px-3 py-2.5">
                                    <StatusBadge value={activity.active ? "ACTIVE" : "INACTIVE"} />
                                </td>
                                <td className="px-3 py-2.5">
                                    <ActivityActions
                                        activity={activity}
                                        isPending={isPending}
                                        onAction={onDeactivate}
                                        onEdit={onEdit}
                                    />
                                    {failed && (
                                        <p className="mt-1 max-w-[260px] text-[11px] text-error">
                                            {deactivateError.message}
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