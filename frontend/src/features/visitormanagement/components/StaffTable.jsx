import { useState } from "react";
import StatusBadge from "./StatusBadge";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";

const TINY_BUTTON =
    "rounded border border-[#C3D8D4] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-primary hover:bg-[#ECFAF7] disabled:opacity-60";
const TINY_DANGER =
    "rounded border border-[#EDC9C9] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-error hover:bg-[#FBEAEA] disabled:opacity-60";

const COLUMNS = ["Name", "Role", "Email", "Phone", "Status", "Actions"];

/**
 * One row's actions. Deactivation asks for confirmation because it removes the
 * member from every selector at once; reactivation goes through an update with
 * active=true, the only path the API offers.
 */
function StaffActions({ member, isPending, onAction, onEdit }) {
    const [confirming, setConfirming] = useState(false);

    if (confirming) {
        return (
            <div className="flex flex-wrap items-center gap-1.5">
                <span className="text-[11px] text-error">Deactivate?</span>
                <button
                    type="button"
                    disabled={isPending}
                    onClick={() => {
                        setConfirming(false);
                        onAction(member, "deactivate");
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
                onClick={() => onEdit(member)}
                className={TINY_BUTTON}
            >
                ✎ edit
            </button>
            {member.active ? (
                <button
                    type="button"
                    disabled={isPending}
                    onClick={() => setConfirming(true)}
                    className={TINY_DANGER}
                >
                    {isPending ? "deactivating…" : "✕ deactivate"}
                </button>
            ) : (
                <button
                    type="button"
                    disabled={isPending}
                    onClick={() => onAction(member, "activate")}
                    className={TINY_BUTTON}
                >
                    {isPending ? "activating…" : "✓ activate"}
                </button>
            )}
        </div>
    );
}

export default function StaffTable({ staff, pendingAction, actionError, onAction, onEdit }) {
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
                    {staff.length === 0 && (
                        <tr>
                            <td colSpan={COLUMNS.length} className="px-3 py-8 text-center text-muted">
                                No staff member matches this filter.
                            </td>
                        </tr>
                    )}
                    {staff.map((member) => {
                        const isPending = pendingAction?.id === member.id;
                        const failed = actionError?.id === member.id;
                        return (
                            <tr key={member.id} className="border-b border-line align-top">
                                <td className="px-3 py-2.5 font-medium text-ink">
                                    {member.fullName}
                                </td>
                                <td className="px-3 py-2.5 text-ink">
                                    {formatEnumLabel(member.role)}
                                </td>
                                <td className="px-3 py-2.5 text-ink">{member.email ?? "—"}</td>
                                <td className="px-3 py-2.5 text-ink">{member.phone ?? "—"}</td>
                                <td className="px-3 py-2.5">
                                    <StatusBadge value={member.active ? "ACTIVE" : "INACTIVE"} />
                                </td>
                                <td className="px-3 py-2.5">
                                    <StaffActions
                                        member={member}
                                        isPending={isPending}
                                        onAction={onAction}
                                        onEdit={onEdit}
                                    />
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