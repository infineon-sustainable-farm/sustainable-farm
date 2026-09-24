import { useEffect, useState } from "react";
import { Info, Loader2, TriangleAlert } from "lucide-react";
import PlantsEmptyState from "./PlantsEmptyState";
import { useGrowthCalendar, useGrowthCalendarBlocks } from "../hooks/useGrowthCalendar";
import BlockFilter from "./BlockFilter";
import GrowthCalendarTable from "./GrowthCalendarTable";

/**
 * Explains, next to the table, why age and phase show a dash. Only rendered
 * when a displayed row actually lacks its planting date, and the Zalka wording
 * is only quoted for rows that come from that study.
 */
function MissingPlantingDateNote({ entries }) {
    const undated = entries.filter((entry) => !entry.plantingDate);
    if (undated.length === 0) return null;
    const fromZalka = undated.some((entry) => entry.source === "Zalka_2025");

    return (
        <div className="flex gap-3 rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm text-gray-600">
            <Info size={18} className="mt-0.5 shrink-0 text-info" />
            <p>
                {fromZalka
                    ? "Planting date not recorded in the reference study (Zalka 2025 gives only “beginning of the rainy season”). "
                    : "Planting date not recorded. "}
                Tree age and growth phase are computed from the planting date and cannot be
                shown until it is recorded.
            </p>
        </div>
    );
}

/**
 * Spells out, once, what the year bands in the Phase column mean. The wording is
 * the growth phase scale of the reference study; the bands and the phase names
 * both come from the API, and nothing here computes or decides a threshold.
 */
function PhaseLegend() {
    return (
        <p className="px-1 text-xs text-gray-500">
            Growth phases (Zalka 2025): 0–2 yrs establishment · 3–5 yrs gradual
            production · 6+ yrs full production.
        </p>
    );
}

export default function GrowthCalendarPage() {
    // Raw stored block value ("A"); an empty string means "no filter".
    const [selectedBlock, setSelectedBlock] = useState("");

    useEffect(() => {
        document.title = "Growth Calendar — Plants";
    }, []);

    const { data: entries, isPending, isError, refetch, isFetching } = useGrowthCalendar({
        blockCode: selectedBlock || null,
    });
    const { data: blocks } = useGrowthCalendarBlocks();

    return (
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 sm:px-6">
                <header>
                    <p className="font-heading text-xs font-bold tracking-widest text-primary uppercase">
                        Task 2
                    </p>
                    <h2 className="font-heading mt-1 text-3xl font-bold text-gray-900">
                        Planting Calendar &amp; Growth Stages
                    </h2>
                    <p className="mt-2 max-w-xl text-sm text-gray-500">
                        Age is auto-calculated from planting date, never entered manually.
                    </p>
                </header>

                <BlockFilter
                    blocks={blocks ?? []}
                    value={selectedBlock}
                    onChange={setSelectedBlock}
                    disabled={isPending}
                />

                {isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                        <Loader2 size={18} className="animate-spin" />
                        Loading growth calendar…
                    </div>
                )}

                {isError && (
                    <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <TriangleAlert size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-gray-900">
                                Growth calendar could not be loaded
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

                {!isPending && !isError && entries.length === 0 && (
                    <PlantsEmptyState
                        title="No growth calendar records match this filter"
                        hint="Try another block, or select “All blocks”."
                    />
                )}

                {!isPending && !isError && entries.length > 0 && (
                    <>
                        <GrowthCalendarTable entries={entries} />
                        <PhaseLegend />
                        <MissingPlantingDateNote entries={entries} />
                    </>
                )}
            </div>
        </div>
    );
}
