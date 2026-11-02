import { useEffect, useState } from "react";
import { Info, Loader2, TriangleAlert } from "lucide-react";
import { Link } from "react-router-dom";
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
                        + "Tree age and growth phase are computed from the planting date and cannot be shown until it is recorded."
                    : "Planting date not entered yet. "
                        + "Tree age and growth phase are computed from the planting date and appear once it is entered."}
            </p>
        </div>
    );
}

/**
 * Says what comes from where. The age bands of the phases are the reference
 * study's, in every profile; the study puts year 5 in two phases and stops at
 * year 7, and the legend says how both are read. The share of the full yield
 * of each phase has sources of its own, shown with it on Harvest & Maturity.
 * The bands and the phase names both come from the API, and nothing here
 * computes or decides a threshold.
 */
function PhaseLegend() {
    return (
        <p className="px-1 text-xs text-gray-500">
            Age bands of the growth phases, from the Zalka 2025 study: 0–2 yrs establishment · 3–5 yrs gradual
            production · 6+ yrs full production. The study gives “years 5 to 7” for full production: year 5 is
            counted as gradual production, and full production continues after year 7. The share of the full yield
            in each phase, with its source, is under Reference values on{" "}
            <Link
                to="/plants/harvest-maturity"
                className="font-semibold text-primary-dark underline underline-offset-2 hover:no-underline"
            >
                Harvest &amp; Maturity
            </Link>
            .
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
            {/* pt-20 under md: the fixed menu button would otherwise cover the title on a phone. */}
            <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 pt-20 pb-8 sm:px-6 md:pt-8">
                <header>
                    {/* A gray 12 px overline: #4a5565 on the gray-50 page -> 7.23:1. */}
                    <p className="font-heading text-xs font-semibold tracking-widest text-gray-600 uppercase">
                        Task 2
                    </p>
                    <h1 className="font-heading mt-1 text-2xl font-bold text-gray-900">
                        Planting Calendar &amp; Growth Stages
                    </h1>
                    <p className="mt-2 max-w-xl text-sm text-gray-500">
                        Tree age and growth stage, from the planting date.
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
                            <p className="font-heading text-lg font-bold text-gray-900">
                                Growth calendar could not be loaded
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

                {!isPending && !isError && entries.length === 0 && (
                    selectedBlock ? (
                        <PlantsEmptyState
                            title="No growth calendar records match this filter"
                            hint="Try another block, or select “All blocks”."
                        />
                    ) : (
                        <PlantsEmptyState
                            title="No planting recorded yet"
                            hint="Record a planting from the Varieties screen."
                        />
                    )
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
