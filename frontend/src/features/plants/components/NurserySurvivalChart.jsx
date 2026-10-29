import { useNurseryBatches } from "../hooks/useNurseryBatches";
import { formatBlock, formatPercent } from "../utils/format";
import OverviewCard from "./OverviewCard";

const TRACK_HEIGHT = 140; // px

/**
 * The mock-up's survival bars, one per nursery batch in the API's order (by
 * start date), as tall as the survival rate the API computes: a transplanted
 * plant counts as a survivor. Each bar is labelled with its batch code and
 * destination block; "Lot" reads "Batch", as on the Nursery screen, since a lot
 * is now the number of a harvest. The batches cover every block. When they do
 * not fit, the bars scroll sideways inside the card.
 *
 * Bar #0a8276 on its track #eef2f1 -> 4.16:1, as on the Nursery screen. In each
 * item the code comes first for screen readers; the rate and the bar are only
 * moved above it on screen.
 */
export default function NurserySurvivalChart() {
    const query = useNurseryBatches();
    const batches = query.data;

    return (
        <OverviewCard
            titleId="overview-nursery-title"
            title="Nursery Survival Rate by Batch"
            caption="Every block: the block filter does not apply here."
            query={query}
            loadingText="Loading the nursery batches…"
            errorText="The nursery batches could not be loaded"
        >
            {batches && batches.length === 0 && <p className="text-sm text-gray-700">No nursery batch yet.</p>}

            {batches && batches.length > 0 && (
                <div className="relative overflow-x-auto">
                    <ul className="flex min-w-max items-end gap-3 pb-1">
                        {batches.map((batch) => (
                            <li key={batch.id} className="flex w-16 shrink-0 flex-col items-center gap-1 text-center">
                                <span className="text-xs font-semibold break-all text-gray-900">{batch.batchCode}</span>
                                {batch.plannedBlockCode && (
                                    <span className="text-xs text-gray-600">{formatBlock(batch.plannedBlockCode)}</span>
                                )}
                                <span className="order-first text-xs font-semibold text-gray-900">
                                    {formatPercent(batch.survivalRatePct)}
                                    <span className="sr-only"> survived</span>
                                </span>
                                <div
                                    aria-hidden="true"
                                    className="order-first flex w-8 items-end rounded-t-md bg-[#eef2f1]"
                                    style={{ height: TRACK_HEIGHT }}
                                >
                                    <div
                                        className="w-full rounded-t-md bg-primary"
                                        style={{ height: `${batch.survivalRatePct}%` }}
                                    />
                                </div>
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </OverviewCard>
    );
}
