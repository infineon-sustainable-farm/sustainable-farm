import { useState } from "react";
import { Info, Loader2, TriangleAlert } from "lucide-react";
import { useYieldForecast } from "../hooks/useYieldForecast";
import { formatBlock, formatKilograms, formatMonth, formatNumber, formatText } from "../utils/format";
import MonthPeriodControls from "./MonthPeriodControls";
import PlantsEmptyState from "./PlantsEmptyState";
import YieldForecastChart from "./YieldForecastChart";
import YieldForecastTable from "./YieldForecastTable";

const DEFAULT_MONTHS = 12;

const TH_CLASS =
    "border-b border-gray-200 bg-gray-50 px-4 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase";

/**
 * The API gives no total. The monthly totals are added here and the sum is
 * rounded to 0.1 kg, like each of them, so no floating-point residue shows.
 */
function periodTotal(monthlyTotals) {
    const sum = monthlyTotals.reduce((total, month) => total + month.expectedKg, 0);
    return Math.round(sum * 10) / 10;
}

function formatTrees(count) {
    return `${formatNumber(count)} ${Number(count) === 1 ? "tree" : "trees"}`;
}

/**
 * The same values as the chart, month by month, as a table. Closed by default:
 * the chart already shows every value, and the table is there for whoever
 * prefers to read them in rows.
 */
function MonthlyTotalsTable({ monthlyTotals, totalKg }) {
    return (
        <details className="rounded-xl border border-gray-200 bg-white">
            <summary className="cursor-pointer px-4 py-3 text-sm font-semibold text-gray-700">
                Show monthly totals as a table
            </summary>
            <div className="overflow-x-auto border-t border-gray-200">
                <table className="w-full border-collapse text-sm whitespace-nowrap">
                    <thead>
                        <tr>
                            <th scope="col" className={TH_CLASS}>Month</th>
                            <th scope="col" className={TH_CLASS}>Expected yield</th>
                        </tr>
                    </thead>
                    <tbody>
                        {monthlyTotals.map((month) => (
                            <tr key={month.month} className="border-b border-gray-100">
                                <td className="px-4 py-2.5 text-gray-700">{formatMonth(month.month)}</td>
                                <td className="px-4 py-2.5 text-gray-700">{formatKilograms(month.expectedKg)}</td>
                            </tr>
                        ))}
                    </tbody>
                    <tfoot>
                        <tr>
                            <td className="px-4 py-2.5 font-semibold text-gray-900">Total</td>
                            <td className="px-4 py-2.5 font-semibold text-gray-900">{formatKilograms(totalKg)}</td>
                        </tr>
                    </tfoot>
                </table>
            </div>
        </details>
    );
}

/**
 * Planted varieties the forecast leaves out because the agronomic reference
 * does not know them. They are named rather than silently dropped.
 */
function VarietiesWithoutReferenceNote({ varieties }) {
    if (varieties.length === 0) return null;
    const listed = varieties
        .map((variety) =>
            `${formatText(variety.varietyName)} (${formatBlock(variety.blockCode)}, ${formatTrees(variety.treeCount)})`)
        .join(", ");
    const subject = varieties.length === 1 ? "this variety" : "these varieties";

    return (
        <div className="flex gap-3 rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm text-gray-600">
            <Info size={18} className="mt-0.5 shrink-0 text-info" />
            <p>
                {`Not in the forecast: ${listed}. No reference value is entered for ${subject} yet: `
                    + "add it under Reference values below."}
            </p>
        </div>
    );
}

/**
 * Expected yield by month for the selected block: a column chart with the
 * period total, the same values as a table, and the forecast line by line
 * with its sources.
 */
export default function YieldForecastSection({ blockCode }) {
    // Month sent to the API; null lets the API start at its current month.
    const [from, setFrom] = useState(null);
    const [months, setMonths] = useState(DEFAULT_MONTHS);

    const { data: forecast, isPending, isError, refetch, isFetching } = useYieldForecast({
        blockCode,
        from,
        months,
    });

    const isEmpty = !isPending && !isError && forecast.monthlyTotals.every((month) => month.expectedKg === 0);
    const totalKg = !isPending && !isError ? periodTotal(forecast.monthlyTotals) : null;

    return (
        <section aria-labelledby="yield-forecast-title" className="flex flex-col gap-4">
            <h3 id="yield-forecast-title" className="font-heading text-base font-bold text-gray-900">
                Expected yield by month
            </h3>

            <MonthPeriodControls
                idPrefix="yield-forecast"
                titleId="yield-forecast-title"
                answeredFrom={forecast?.from}
                onFromChange={setFrom}
                months={months}
                onMonthsChange={setMonths}
            />

            {isPending && (
                <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading yield forecast…
                </div>
            )}

            {isError && (
                <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                    <TriangleAlert size={28} className="text-error" />
                    <div>
                        <p className="font-heading text-base font-bold text-gray-900">
                            Yield forecast could not be loaded
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

            {isEmpty && (
                <>
                    <PlantsEmptyState
                        title="No yield expected over this period"
                        hint="The forecast counts recorded plantings only. Record a planting from the Varieties screen, or choose another period."
                    />
                    <VarietiesWithoutReferenceNote varieties={forecast.varietiesWithoutReference} />
                </>
            )}

            {!isPending && !isError && !isEmpty && (
                <>
                    <div className="rounded-xl border border-gray-200 bg-white p-4 sm:p-6">
                        <p className="mb-4 text-sm text-gray-600">
                            Total over the period:{" "}
                            <span className="font-semibold text-gray-900">{formatKilograms(totalKg)}</span>
                        </p>
                        <YieldForecastChart monthlyTotals={forecast.monthlyTotals} totalKg={totalKg} />
                    </div>
                    <MonthlyTotalsTable monthlyTotals={forecast.monthlyTotals} totalKg={totalKg} />
                    <VarietiesWithoutReferenceNote varieties={forecast.varietiesWithoutReference} />
                    <p className="px-1 text-xs text-gray-500">
                        Based on the recorded plantings only. Yield per tree, harvest season and growth-phase
                        shares come from the reference values further down this page; the source of each is
                        listed in the details below.
                    </p>
                    <YieldForecastTable entries={forecast.entries} />
                </>
            )}
        </section>
    );
}
