import { useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import { useProspects } from "../hooks/useRegistrations";
import { formatDateShort, formatTime } from "../utils/format";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";
import StatusBadge from "./StatusBadge";

const COLUMNS = ["Visitor", "Group", "Purpose", "Slot", "Status"];

/*
 * The API flags every purchase, partnership or investment registration as a
 * prospect, cancelled and rejected ones included. The commercial follow-up
 * only cares about the active ones, so those are filtered out here.
 */
const ACTIVE_STATUSES = new Set(["PENDING", "CONFIRMED", "CHECKED_IN"]);

/**
 * Collapsible commercial follow-up section of the registration screen. The
 * prospects query only runs once the section is opened.
 */
export default function ProspectsSection() {
    const [open, setOpen] = useState(false);
    const {
        data: prospects,
        isPending,
        isError,
        refetch,
        isFetching,
    } = useProspects(open);

    const activeProspects = (prospects ?? []).filter((registration) =>
        ACTIVE_STATUSES.has(registration.status),
    );

    return (
        <section className="mt-6 rounded-lg border border-line bg-[#F7FDFB] p-4.5 animate-fade-up">
            <div className="flex flex-wrap items-center gap-3">
                <div>
                    <h3 className="font-heading text-[17px] font-bold text-ink">
                        Prospects (commercial follow-up)
                    </h3>
                    <p className="mt-0.5 text-[11px] text-muted">
                        Visits flagged automatically for a purchase, a partnership or an investment.
                    </p>
                </div>
                <button
                    type="button"
                    onClick={() => setOpen((current) => !current)}
                    className="ml-auto rounded-md border border-line bg-white px-4 py-2 text-xs text-primary hover:bg-[#F2FBF9]"
                >
                    {open ? "Hide prospects" : "Show prospects"}
                </button>
            </div>

            {open && isPending && (
                <div className="mt-4 flex items-center justify-center gap-3 px-6 py-10 text-sm text-muted">
                    <Loader2 size={18} className="animate-spin" />
                    Loading prospects…
                </div>
            )}

            {open && isError && (
                <div className="mt-4 flex flex-col items-center gap-3 rounded-lg border border-error/30 bg-error/5 px-6 py-8 text-center">
                    <TriangleAlert size={24} className="text-error" />
                    <p className="text-sm text-muted">Prospects could not be loaded.</p>
                    <button
                        type="button"
                        onClick={() => refetch()}
                        disabled={isFetching}
                        className="rounded-md bg-primary px-4 py-2 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:opacity-60"
                    >
                        {isFetching ? "Retrying…" : "Retry"}
                    </button>
                </div>
            )}

            {open && !isPending && !isError && (
                <>
                    <div className="mt-4 overflow-x-auto">
                        <table className="vm-table w-full border-collapse text-sm">
                            <thead>
                                <tr>
                                    {COLUMNS.map((column) => (
                                        <th
                                            key={column}
                                            scope="col"
                                            className="border-b-2 border-line bg-white px-3 py-2 text-left text-xs font-semibold tracking-wide whitespace-nowrap text-ink uppercase"
                                        >
                                            {column}
                                        </th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                {activeProspects.length === 0 && (
                                    <tr>
                                        <td
                                            colSpan={COLUMNS.length}
                                            className="px-3 py-6 text-center text-muted"
                                        >
                                            No active prospect.
                                        </td>
                                    </tr>
                                )}
                                {activeProspects.map((registration) => (
                                    <tr key={registration.id} className="border-b border-line">
                                        <td className="px-3 py-2.5 font-medium text-ink">
                                            {registration.visitorName ?? "—"}
                                        </td>
                                        <td className="px-3 py-2.5 text-ink">
                                            {registration.groupSize ?? "—"}
                                        </td>
                                        <td className="px-3 py-2.5 text-ink">
                                            {formatEnumLabel(registration.visitPurpose)}
                                        </td>
                                        <td className="px-3 py-2.5 whitespace-nowrap text-ink">
                                            {formatDateShort(registration.slotDate)}
                                            {" · "}
                                            {formatTime(registration.slotStart)}
                                        </td>
                                        <td className="px-3 py-2.5">
                                            <StatusBadge value={registration.status} />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    <p className="mt-3 text-[11px] text-muted">
                        Cancelled and rejected prospects are hidden here; the full record stays in the
                        registration queue above.
                    </p>
                </>
            )}
        </section>
    );
}