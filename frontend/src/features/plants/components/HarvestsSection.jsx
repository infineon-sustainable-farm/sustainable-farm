import { useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import { useHarvests } from "../hooks/useHarvests";
import HarvestsTable from "./HarvestsTable";
import PlantsEmptyState from "./PlantsEmptyState";

const LABEL_CLASS = "text-xs font-semibold text-gray-500";
const CONTROL_CLASS =
    "rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm text-gray-800 focus:border-primary focus:outline-none";

/**
 * One date filter of the list. Its accessible name joins the section title and
 * its own label, so "From" here is told apart from the forecast's "From".
 */
function DateFilter({ id, label, value, onChange }) {
    return (
        <div className="flex items-center gap-3">
            <label id={`${id}-label`} htmlFor={id} className={LABEL_CLASS}>
                {label}
            </label>
            <input
                id={id}
                type="date"
                value={value}
                onChange={(event) => onChange(event.target.value)}
                aria-labelledby={`recorded-harvests-title ${id}-label`}
                className={CONTROL_CLASS}
            />
        </div>
    );
}

/**
 * Recorded harvests for the selected block, between two optional dates, both
 * included. Filters the API cannot match simply give an empty list.
 */
export default function HarvestsSection({ blockCode }) {
    // "YYYY-MM-DD" as the date fields give it; an empty string means "no bound".
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");

    const { data: harvests, isPending, isError, refetch, isFetching } = useHarvests({
        blockCode,
        from: from || null,
        to: to || null,
    });
    const isFiltered = Boolean(blockCode || from || to);

    return (
        <section aria-labelledby="recorded-harvests-title" className="flex flex-col gap-4">
            <h3 id="recorded-harvests-title" className="font-heading text-base font-bold text-gray-900">
                Recorded harvests
            </h3>

            <div className="flex flex-wrap items-center gap-x-6 gap-y-3 rounded-xl border border-gray-200 bg-white px-4 py-3">
                <DateFilter id="recorded-harvests-from" label="From" value={from} onChange={setFrom} />
                <DateFilter id="recorded-harvests-to" label="To" value={to} onChange={setTo} />
            </div>

            {isPending && (
                <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading harvests…
                </div>
            )}

            {isError && (
                <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                    <TriangleAlert size={28} className="text-error" />
                    <div>
                        <p className="font-heading text-base font-bold text-gray-900">
                            Harvests could not be loaded
                        </p>
                        <p className="mt-1 text-sm text-gray-500">
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

            {!isPending && !isError && harvests.length === 0 && (
                isFiltered ? (
                    <PlantsEmptyState
                        title="No harvest matches these filters"
                        hint="Try another block or date range, or select “All blocks”."
                    />
                ) : (
                    <PlantsEmptyState title="No harvest recorded yet" />
                )
            )}

            {!isPending && !isError && harvests.length > 0 && <HarvestsTable harvests={harvests} />}
        </section>
    );
}
