import { Loader2, TriangleAlert } from "lucide-react";
import { useFertilizers } from "../hooks/useFertilizers";
import FertilizerStockTable from "./FertilizerStockTable";
import PlantsEmptyState from "./PlantsEmptyState";

/**
 * Current stock of every fertilizer of the catalogue. The stock and the
 * low-stock status come computed by the API.
 *
 * `addAction` is the page's "Add a fertilizer" button, offered again when the
 * catalogue is empty.
 */
export default function FertilizerStockSection({ addAction }) {
    const { data: fertilizers, isPending, isError, refetch, isFetching } = useFertilizers();

    return (
        <section aria-labelledby="current-stock-title" className="flex flex-col gap-4">
            <h3 id="current-stock-title" className="font-heading text-base font-bold text-gray-900">
                Current stock
            </h3>

            {isPending && (
                <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading fertilizers…
                </div>
            )}

            {isError && (
                <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                    <TriangleAlert size={28} className="text-error" />
                    <div>
                        <p className="font-heading text-base font-bold text-gray-900">
                            Fertilizers could not be loaded
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

            {!isPending && !isError && fertilizers.length === 0 && (
                <PlantsEmptyState title="No fertilizer in the catalogue yet" action={addAction} />
            )}

            {!isPending && !isError && fertilizers.length > 0 && <FertilizerStockTable fertilizers={fertilizers} />}
        </section>
    );
}
