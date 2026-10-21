import { useState } from "react";
import { Info, Loader2, TriangleAlert } from "lucide-react";
import { useYieldComparison } from "../hooks/useYieldComparison";
import { formatBlock, formatCode, formatKilograms, formatMonth, formatNumber, formatPercent, formatText } from "../utils/format";
import MonthPeriodControls from "./MonthPeriodControls";
import PlantsEmptyState from "./PlantsEmptyState";
import YieldComparisonCards from "./YieldComparisonCards";
import YieldComparisonTables from "./YieldComparisonTables";

const DEFAULT_MONTHS = 12;

// Why a planted variety row has no expected yield, keyed by the API's code.
const REASONS = { NO_REFERENCE: "no reference value", NO_TREE_LEFT: "no tree left" };

function countOf(count, word) {
    return `${formatNumber(count)} ${Number(count) === 1 ? word : `${word}s`}`;
}

/**
 * The total of the period, in one sentence. The months are named by their
 * first one and their number, as the API gives them: no last month is worked
 * out here.
 */
function TotalSentence({ comparison, blockCode }) {
    const { total } = comparison;
    const scope = blockCode ? formatBlock(blockCode) : "All blocks";
    const period = `${countOf(comparison.months, "month")} from ${formatMonth(comparison.from, "long")}`;
    const figures = total.actualKg === null
        ? `no harvest recorded, ${formatKilograms(total.expectedKg)} expected.`
        : `${formatKilograms(total.actualKg)} harvested of ${formatKilograms(total.expectedKg)} expected`
            + (total.achievedPct === null ? "." : ` (${formatPercent(total.achievedPct)}).`);
    return (
        <p className="text-sm text-gray-600">
            {`${scope}, ${period}: `}
            <span className="font-semibold text-gray-900">{figures}</span>
        </p>
    );
}

/**
 * Planted variety rows the API does not compare, with their harvests, so that
 * none of them is silently dropped.
 */
function NotComparedNote({ rows }) {
    if (rows.length === 0) return null;
    const listed = rows
        .map((row) => {
            const where = row.treeCount === null
                ? formatBlock(row.blockCode)
                : `${formatBlock(row.blockCode)}, ${countOf(row.treeCount, "tree")}`;
            const harvested = row.harvestCount === 0
                ? "no harvest"
                : `${formatKilograms(row.actualKg)} in ${countOf(row.harvestCount, "harvest")}`;
            return `${formatText(row.varietyName)} (${where}): ${harvested}, ${REASONS[row.reason] ?? formatCode(row.reason)}`;
        })
        .join("; ");
    const hint = rows.some((row) => row.reason === "NO_REFERENCE")
        ? " Reference values are entered on the Harvest & Maturity screen."
        : "";

    return (
        <div className="flex gap-3 rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm text-gray-600">
            <Info size={18} className="mt-0.5 shrink-0 text-info" />
            <p>{`Not compared: ${listed}.${hint}`}</p>
        </div>
    );
}

/**
 * Expected against actual yield of the planted varieties of the selected
 * block, over whole months: the calendar year until another period is chosen.
 * Every yield, sum and share comes from the API; the browser only lays them
 * out.
 */
export default function YieldComparisonSection({ blockCode }) {
    // Month sent to the API; null lets the API start at January of its current year.
    const [from, setFrom] = useState(null);
    const [months, setMonths] = useState(DEFAULT_MONTHS);

    const { data: comparison, isPending, isError, refetch, isFetching } = useYieldComparison({
        blockCode,
        from,
        months,
    });

    const isLoaded = !isPending && !isError;

    return (
        <section aria-labelledby="yield-comparison-title" className="flex flex-col gap-4">
            <div>
                <h3 id="yield-comparison-title" className="font-heading text-base font-bold text-gray-900">
                    Expected vs actual yield
                </h3>
                <p className="mt-1 max-w-3xl text-sm text-gray-600">
                    The harvests recorded over the period, against the yield the forecast expects for the same
                    months.
                </p>
            </div>

            <MonthPeriodControls
                idPrefix="yield-comparison"
                titleId="yield-comparison-title"
                answeredFrom={comparison?.from}
                onFromChange={setFrom}
                months={months}
                onMonthsChange={setMonths}
            />

            {isPending && (
                <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading yield comparison…
                </div>
            )}

            {isError && (
                <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                    <TriangleAlert size={28} className="text-error" />
                    <div>
                        <p className="font-heading text-base font-bold text-gray-900">
                            Yield comparison could not be loaded
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

            {isLoaded && comparison.varieties.length === 0 && comparison.notCompared.length === 0 && (
                <PlantsEmptyState
                    title="No planted variety to compare over this period"
                    hint="The comparison counts the recorded plantings and harvests. Record a planting, or choose another period."
                />
            )}

            {isLoaded && comparison.varieties.length === 0 && comparison.notCompared.length > 0 && (
                <p className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm text-gray-700">
                    No planted variety with a reference value over this period.
                </p>
            )}

            {isLoaded && comparison.varieties.length > 0 && (
                <>
                    <TotalSentence comparison={comparison} blockCode={blockCode} />
                    <YieldComparisonCards lines={comparison.varieties} />
                    <YieldComparisonTables blocks={comparison.blocks} lines={comparison.varieties} />
                </>
            )}

            {isLoaded && <NotComparedNote rows={comparison.notCompared} />}

            {isLoaded && comparison.varieties.length > 0 && (
                <p className="px-1 text-xs text-gray-500">
                    Expected yield: the rule of the forecast applied to today&apos;s trees and reference values, past
                    months included. Actual yield: the harvests dated in the months.
                </p>
            )}
        </section>
    );
}
