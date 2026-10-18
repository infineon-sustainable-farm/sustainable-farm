import StatusBadge from "./StatusBadge";

const TINY_BUTTON =
    "rounded border border-[#C3D8D4] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-primary hover:bg-[#ECFAF7] disabled:opacity-60";
const TINY_DANGER =
    "rounded border border-[#EDC9C9] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-error hover:bg-[#FBEAEA] disabled:opacity-60";

const COLUMNS = ["Workshop / template", "Duration", "Target group", "Facilitator", "Status", "Actions"];

/*
 * The API only accepts publish on a DRAFT and deactivate on an ACTIVE
 * workshop, so the table offers exactly the transition each row allows.
 */
function statusActions(status) {
    if (status === "DRAFT") return ["publish"];
    if (status === "ACTIVE") return ["deactivate"];
    return [];
}

export default function WorkshopsTable({
    workshops,
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
                    {workshops.length === 0 && (
                        <tr>
                            <td colSpan={COLUMNS.length} className="px-3 py-8 text-center text-muted">
                                No workshops yet.
                            </td>
                        </tr>
                    )}
                    {workshops.map((workshop) => {
                        const isPending = pendingAction?.id === workshop.id;
                        const failed = actionError?.id === workshop.id;
                        return (
                            <tr key={workshop.id} className="border-b border-line align-top">
                                <td className="px-3 py-2.5 font-medium text-ink">
                                    {workshop.name}
                                    {workshop.description && (
                                        <span className="mt-0.5 block text-xs text-muted">
                                            {workshop.description}
                                        </span>
                                    )}
                                </td>
                                <td className="px-3 py-2.5 whitespace-nowrap text-ink">
                                    {workshop.durationMinutes} min
                                </td>
                                <td className="px-3 py-2.5 text-ink">{workshop.targetGroup}</td>
                                <td className="px-3 py-2.5 text-ink">{workshop.facilitator ?? "—"}</td>
                                <td className="px-3 py-2.5">
                                    <StatusBadge value={workshop.status} />
                                </td>
                                <td className="px-3 py-2.5">
                                    <div className="flex flex-wrap items-center gap-1.5">
                                        <button
                                            type="button"
                                            disabled={isPending}
                                            onClick={() => onEdit(workshop)}
                                            className={TINY_BUTTON}
                                        >
                                            ✎ edit
                                        </button>
                                        {statusActions(workshop.status).map((action) => (
                                            <button
                                                key={action}
                                                type="button"
                                                disabled={isPending}
                                                onClick={() => onAction(workshop, action)}
                                                className={action === "deactivate" ? TINY_DANGER : TINY_BUTTON}
                                            >
                                                {action === "publish" ? "publish" : "✕ deactivate"}
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