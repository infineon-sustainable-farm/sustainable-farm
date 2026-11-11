import { useNurseryBatches } from "../hooks/useNurseryBatches";
import { formatBlock, formatNumber, formatPercent } from "../utils/format";
import OverviewCard from "./OverviewCard";
import { CARD_CAPTION } from "./plantsStyles";

function plantsOf(count) {
    return Number(count) === 1 ? "plant" : "plants";
}

/**
 * The mock-up's survival rate, one row per nursery batch in the API's order
 * (by start date): its code and destination block, a bar as long as the
 * survival rate the API computes, and the counts behind it, "138 of 150 plants
 * survived (92%)". A transplanted plant counts as a survivor. With one or two
 * batches the counts give the bar its context; with many, the rows stack.
 * "Lot" reads "Batch", as on the Nursery screen, since a lot is now the number
 * of a harvest. The batches cover every block.
 *
 * Bar #0a8276 on its track #eef2f1 -> 4.16:1, as on the Nursery screen. The
 * bar repeats the sentence under it, so it is hidden from screen readers.
 */
export default function NurserySurvivalChart() {
    const query = useNurseryBatches();
    const batches = query.data;

    return (
        <OverviewCard
            titleId="overview-nursery-title"
            className="lg:row-span-2 lg:grid lg:grid-rows-subgrid"
            title="Nursery Survival Rate by Batch"
            caption="All nursery batches."
            query={query}
            loadingText="Loading the nursery batches…"
            errorText="Nursery batches are unavailable."
        >
            {batches && batches.length === 0 && <p className="text-sm text-gray-700">No nursery batch yet.</p>}

            {batches && batches.length > 0 && (
                <ul className="flex flex-col gap-3">
                    {batches.map((batch) => (
                        <li key={batch.id} className="flex flex-col gap-1">
                            <p className="text-sm break-words text-on-background">
                                <span className="font-semibold">{`Batch ${batch.batchCode}`}</span>
                                {batch.plannedBlockCode && (
                                    <span className="text-xs text-gray-600">{` · to ${formatBlock(batch.plannedBlockCode)}`}</span>
                                )}
                            </p>
                            <div aria-hidden="true" className="h-2.5 rounded-full bg-[#eef2f1]">
                                <div
                                    className="h-2.5 rounded-full bg-primary"
                                    style={{ width: `${Math.min(Math.max(batch.survivalRatePct ?? 0, 0), 100)}%` }}
                                />
                            </div>
                            <p className={CARD_CAPTION}>
                                <span className="font-semibold text-on-background">
                                    {`${formatNumber(batch.survivedCount)} of ${formatNumber(batch.initialCount)}`}
                                </span>
                                {` ${plantsOf(batch.initialCount)} survived (${formatPercent(batch.survivalRatePct)})`}
                                {batch.lossCount > 0 && ` · ${formatNumber(batch.lossCount)} lost`}
                            </p>
                            <p className={CARD_CAPTION}>
                                {`${formatNumber(batch.transplantedCount)} transplanted · ${formatNumber(batch.currentCount)} still in the nursery`}
                            </p>
                        </li>
                    ))}
                </ul>
            )}
        </OverviewCard>
    );
}
