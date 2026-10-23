import { Loader2, TriangleAlert } from "lucide-react";
import { useHarvestStatus } from "../hooks/useHarvestStatus";
import { formatDate } from "../utils/format";
import HarvestStatusTable from "./HarvestStatusTable";
import PlantsEmptyState from "./PlantsEmptyState";

/**
 * The maturity status of the day of the planted varieties of the selected
 * block, as the API computes it from the harvest window of each variety and
 * the pre-harvest intervals of its block. The rules are stated with the day
 * and the look-ahead the API used; nothing of the status is computed here.
 */
export default function HarvestStatusSection({ blockCode }) {
    const { data, isPending, isError, refetch, isFetching } = useHarvestStatus({ blockCode });

    return (
        <section aria-labelledby="maturity-status-title" className="flex flex-col gap-4">
            <div>
                <h3 id="maturity-status-title" className="font-heading text-base font-bold text-gray-900">
                    Maturity status
                </h3>
                {data && (
                    <p className="mt-1 max-w-3xl text-sm text-gray-600">
                        {`As of ${formatDate(data.today)}. Ready for harvest: the harvest window is open and no `
                            + "pre-harvest interval runs on the block. Ripe: the window opens within "
                            + `${data.withinDays} days, or it is open while an interval runs. Immature: the window `
                            + "opens later."}
                    </p>
                )}
            </div>

            {isPending && (
                <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading maturity status…
                </div>
            )}

            {isError && (
                <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                    <TriangleAlert size={28} className="text-error" />
                    <div>
                        <p className="font-heading text-base font-bold text-gray-900">
                            Maturity status could not be loaded
                        </p>
                        {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1; gray-500 gave 4.32:1. */}
                        <p className="mt-1 text-sm text-gray-600">
                            The request to the server did not succeed.
                        </p>
                    </div>
                    <button
                        type="button"
                        onClick={() => refetch()}
                        disabled={isFetching}
                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                    >
                        {isFetching ? "Retrying…" : "Retry"}
                    </button>
                </div>
            )}

            {!isPending && !isError && data.varieties.length === 0 && (
                blockCode ? (
                    <PlantsEmptyState
                        title="No planted variety on this block"
                        hint="Select another block, or “All blocks”."
                    />
                ) : (
                    <PlantsEmptyState
                        title="No planted variety yet"
                        hint="Record a planting from the Varieties screen."
                    />
                )
            )}

            {!isPending && !isError && data.varieties.length > 0 && (
                <>
                    <HarvestStatusTable statuses={data.varieties} />
                    <p className="px-1 text-xs text-gray-500">
                        Harvest window: from the first month of the season in which the trees give a yield, to the
                        end of the season, as in the forecast. First harvest: the first harvest recorded in the open
                        window; earlier pickings are listed under Recorded harvests. A dash means no value.
                    </p>
                </>
            )}
        </section>
    );
}
