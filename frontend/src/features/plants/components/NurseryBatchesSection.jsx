import { Loader2, TriangleAlert } from "lucide-react";
import { useNurseryBatches } from "../hooks/useNurseryBatches";
import NurseryBatchesTable from "./NurseryBatchesTable";
import NurserySurvivalCards from "./NurserySurvivalCards";
import PlantsEmptyState from "./PlantsEmptyState";

/**
 * The nursery batches, then the survival rate of each, for the selected
 * destination block. Counts, rates and stages all come from the API.
 *
 * The API takes no block filter for the batches, so the batches planned for
 * the selected block are picked out of the list here; nothing else is
 * computed. A batch with no planned block shows only with "All blocks".
 *
 * `addAction` is the page's "Add a batch" button, offered again when no batch
 * is recorded at all.
 */
export default function NurseryBatchesSection({ blockCode, addAction }) {
    const { data: batches, isPending, isError, refetch, isFetching } = useNurseryBatches();
    const shown = blockCode ? (batches ?? []).filter((batch) => batch.plannedBlockCode === blockCode) : batches;

    return (
        <section aria-labelledby="nursery-batches-title" className="flex flex-col gap-4">
            <h3 id="nursery-batches-title" className="font-heading text-base font-bold text-gray-900">
                Nursery batches
            </h3>

            {isPending && (
                <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading batches…
                </div>
            )}

            {isError && (
                <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                    <TriangleAlert size={28} className="text-error" />
                    <div>
                        <p className="font-heading text-base font-bold text-gray-900">
                            Batches could not be loaded
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

            {!isPending && !isError && batches.length === 0 && (
                <PlantsEmptyState title="No nursery batch yet" action={addAction} />
            )}

            {!isPending && !isError && batches.length > 0 && shown.length === 0 && (
                <PlantsEmptyState title="No batch is planned for this block" hint="Select “All blocks”." />
            )}

            {!isPending && !isError && shown.length > 0 && (
                <>
                    <NurseryBatchesTable batches={shown} />
                    <NurserySurvivalCards batches={shown} />
                </>
            )}
        </section>
    );
}
