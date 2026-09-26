import { useState } from "react";
import { formatNumber } from "../utils/format";

const TINY_BUTTON =
    "rounded border border-[#C3D8D4] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-primary hover:bg-[#ECFAF7] disabled:opacity-60";
const TINY_DANGER =
    "rounded border border-[#EDC9C9] bg-white px-1.5 py-0.5 text-[11px] font-semibold text-error hover:bg-[#FBEAEA] disabled:opacity-60";

/**
 * Per-stop actions. Deactivation has no counterpart in the API (no active flag
 * on the payload, no reactivation call), so it asks for confirmation and the
 * stop leaves the timeline for good.
 */
function StopActions({ stop, isPending, onEdit, onDeactivate }) {
    const [confirming, setConfirming] = useState(false);

    if (confirming) {
        return (
            <div className="mt-2 flex flex-wrap items-center gap-1.5">
                <span className="text-[11px] text-error">Deactivate this stop?</span>
                <button
                    type="button"
                    disabled={isPending}
                    onClick={() => {
                        setConfirming(false);
                        onDeactivate(stop);
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
        <div className="mt-2 flex flex-wrap gap-1.5">
            <button
                type="button"
                disabled={isPending}
                onClick={() => onEdit(stop)}
                className={TINY_BUTTON}
            >
                ✎ edit
            </button>
            <button
                type="button"
                disabled={isPending}
                onClick={() => setConfirming(true)}
                className={TINY_DANGER}
            >
                {isPending ? "deactivating…" : "✕ deactivate"}
            </button>
        </div>
    );
}

/**
 * The mockup's vertical timeline of the standard tour, now manageable: each
 * stop carries edit and deactivate actions. Stops come from the API already
 * ordered by position.
 */
export default function TourStopsTimeline({
    stops,
    pendingDeactivateId,
    deactivateError,
    onEdit,
    onDeactivate,
}) {
    if (stops.length === 0) {
        return <p className="text-sm text-muted">No tour stop defined yet.</p>;
    }

    return (
        <div className="flex flex-col gap-4.5 border-l-[3px] border-primary pl-4.5">
            {stops.map((stop) => (
                <div key={stop.id} className={`relative ${stop.active ? "" : "opacity-60"}`}>
                    <span
                        aria-hidden="true"
                        className="absolute top-1 -left-[25px] h-2.5 w-2.5 rounded-full border-2 border-primary-dark bg-accent"
                    />
                    <p className="font-heading flex items-center gap-2 text-base font-bold text-primary-dark">
                        {stop.name}
                        {!stop.active && (
                            <span className="rounded-full bg-gray-200 px-2 py-0.5 text-[10px] font-semibold tracking-wide text-gray-600 uppercase">
                                Inactive
                            </span>
                        )}
                    </p>
                    <p className="mt-0.5 text-[11px] text-muted">
                        {stop.durationMinutes} min
                        {stop.location ? ` · ${stop.location}` : ""}
                        {stop.maxCapacity ? ` · max ${formatNumber(stop.maxCapacity)} pers` : ""}
                    </p>
                    {stop.description && (
                        <p className="mt-1 text-sm text-ink">{stop.description}</p>
                    )}
                    {stop.demo && (
                        <p className="mt-1 text-xs text-muted">
                            <span className="font-semibold">Demo:</span> {stop.demo}
                        </p>
                    )}
                    {stop.safetyNotes && (
                        <p className="mt-1 text-xs text-muted">
                            <span className="font-semibold">Safety:</span> {stop.safetyNotes}
                        </p>
                    )}

                    <StopActions
                        stop={stop}
                        isPending={pendingDeactivateId === stop.id}
                        onEdit={onEdit}
                        onDeactivate={onDeactivate}
                    />
                    {deactivateError?.id === stop.id && (
                        <p className="mt-1 text-[11px] text-error">{deactivateError.message}</p>
                    )}
                </div>
            ))}
        </div>
    );
}