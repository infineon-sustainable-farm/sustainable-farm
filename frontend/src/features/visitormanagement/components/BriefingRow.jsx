import StatusBadge from "./StatusBadge";
import { formatDateTime } from "../utils/format";
import { useBriefing } from "../hooks/useSafety";

const TINY_BUTTON =
    "rounded border border-[#C3D8D4] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-primary hover:bg-[#ECFAF7] disabled:opacity-60";

/**
 * One registration's briefing row. The briefing is fetched per row (the API
 * exposes no list endpoint) and the delivery form opens in a modal from the
 * page, so the table stays compact.
 */
export default function BriefingRow({ registration, isDelivering, deliverError, onRecord }) {
    const {
        data: briefing,
        isPending,
        isError,
        refetch,
        isFetching,
    } = useBriefing(registration.id, Boolean(registration.briefingId));

    const status = briefing?.status;

    return (
        <tr className="border-b border-line align-top">
            <td className="px-3 py-2.5 font-medium text-ink">
                {registration.visitorName ?? "—"}
            </td>
            <td className="px-3 py-2.5 text-ink">{registration.groupSize ?? "—"}</td>
            <td className="px-3 py-2.5 whitespace-nowrap text-ink">
                {isPending ? "…" : formatDateTime(briefing?.deliveredAt)}
            </td>
            <td className="px-3 py-2.5 text-ink">{briefing?.staffMember ?? "—"}</td>
            <td className="px-3 py-2.5 text-ink">{briefing?.signature ? "✓ signed" : "—"}</td>
            <td className="px-3 py-2.5">
                {isPending && <span className="text-xs text-muted">Loading…</span>}
                {isError && (
                    <span className="text-xs text-error">
                        Unavailable{" "}
                        <button
                            type="button"
                            onClick={() => refetch()}
                            disabled={isFetching}
                            className="underline disabled:opacity-60"
                        >
                            retry
                        </button>
                    </span>
                )}
                {!isPending && !isError && <StatusBadge value={status} />}
            </td>
            <td className="px-3 py-2.5">
                {status === "PENDING" && (
                    <button
                        type="button"
                        disabled={isDelivering}
                        onClick={() => onRecord(registration)}
                        className={TINY_BUTTON}
                    >
                        {isDelivering ? "recording…" : "record"}
                    </button>
                )}
                {deliverError && (
                    <p className="mt-1 max-w-[260px] text-[11px] text-error">{deliverError}</p>
                )}
            </td>
        </tr>
    );
}