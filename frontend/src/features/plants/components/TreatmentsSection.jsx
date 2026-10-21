import { Loader2, TriangleAlert } from "lucide-react";
import { useTreatments } from "../hooks/useTreatments";
import PlantsEmptyState from "./PlantsEmptyState";
import TreatmentsTable from "./TreatmentsTable";

/**
 * The treatment record of the selected block: the treatments answering a
 * finding and the preventive ones, by date, with the date from which the
 * block can be harvested again, computed by the API.
 *
 * `recordAction` is the page's "Record a preventive treatment" button,
 * offered again when no treatment is recorded at all.
 */
export default function TreatmentsSection({ blockCode, recordAction }) {
    const { data: treatments, isPending, isError, refetch, isFetching } = useTreatments({ blockCode });

    return (
        <section aria-labelledby="treatment-record-title" className="flex flex-col gap-4">
            <div>
                <h3 id="treatment-record-title" className="font-heading text-base font-bold text-gray-900">
                    Treatment record
                </h3>
                <p className="mt-1 max-w-2xl text-sm text-gray-600">
                    Every plant protection treatment, for a finding or preventive, with its pre-harvest interval.
                </p>
            </div>

            {isPending && (
                <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading treatments…
                </div>
            )}

            {isError && (
                <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                    <TriangleAlert size={28} className="text-error" />
                    <div>
                        <p className="font-heading text-base font-bold text-gray-900">
                            Treatments could not be loaded
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

            {!isPending && !isError && treatments.length === 0 && (
                blockCode ? (
                    <PlantsEmptyState
                        title="No treatment on this block"
                        hint="Select “All blocks” to see the whole record."
                    />
                ) : (
                    <PlantsEmptyState
                        title="No treatment recorded yet"
                        hint="A treatment is recorded for a finding, or as a preventive treatment."
                        action={recordAction}
                    />
                )
            )}

            {!isPending && !isError && treatments.length > 0 && <TreatmentsTable treatments={treatments} />}
        </section>
    );
}
