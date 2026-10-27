import { Loader2, TriangleAlert } from "lucide-react";
import { formatCode, formatShare } from "../utils/format";
import { formatAmount, formatMass, MAX_HORIZON_YEARS } from "../utils/scenarioRequests";

// Narrower cells than the other tables, and headers that may wrap: the ten columns then fit a desktop screen.
const TH_CLASS =
    "border-b border-gray-200 bg-gray-50 px-3 py-3 text-left align-bottom text-xs font-semibold tracking-wide whitespace-normal text-gray-500 uppercase";
const TD_CLASS = "px-3 py-3 text-gray-700";
const COLUMNS = [
    "Year",
    "Age",
    "Growth phase",
    "Yield share",
    "Harvest",
    "Sellable",
    "Revenue",
    "Operating cost",
    "Net benefit",
    "Cumulative",
];
const HORIZONS = Array.from({ length: MAX_HORIZON_YEARS }, (_, index) => index + 1);

/** What the payback of the plan is, as the API found it within the years shown. */
function paybackText(baseline, horizonYears) {
    if (baseline.paybackYear !== null) return `Payback with growth phases: year ${baseline.paybackYear}`;
    if (baseline.netBenefit === null) return "Enter a price per kg to compute the payback.";
    return `Payback with growth phases: not within ${horizonYears} years`;
}

function YearsTable({ analysis }) {
    const { baseline, currency } = analysis;
    return (
        // relative: keeps the scrolling inside the card on a phone, as in the other tables of the module.
        <div className="relative overflow-x-auto rounded-xl border border-gray-200 bg-white">
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th key={column} scope="col" className={TH_CLASS}>
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {baseline.years.map((year) => {
                        const isPayback = year.year === baseline.paybackYear;
                        return (
                            <tr
                                key={year.year}
                                className={`border-b border-gray-100 last:border-b-0 ${isPayback ? "bg-[#e5f7ef]" : ""}`}
                            >
                                <th scope="row" className="px-3 py-3 text-left font-medium text-gray-800">
                                    {year.year}
                                    {isPayback && (
                                        <span className="ml-2 rounded-full bg-white px-2 py-0.5 text-xs font-semibold text-[#257e55]">
                                            Payback
                                        </span>
                                    )}
                                </th>
                                <td className={TD_CLASS}>{`${year.ageYears} ${year.ageYears === 1 ? "yr" : "yrs"}`}</td>
                                <td className={TD_CLASS}>{formatCode(year.growthPhase)}</td>
                                <td className={TD_CLASS}>{formatShare(year.yieldShare)}</td>
                                <td className={TD_CLASS}>{formatMass(year.harvestKg)}</td>
                                <td className={TD_CLASS}>{formatMass(year.sellableKg)}</td>
                                <td className={TD_CLASS}>{formatAmount(year.revenue, currency)}</td>
                                <td className={TD_CLASS}>{formatAmount(year.operatingCost, currency)}</td>
                                <td className={TD_CLASS}>{formatAmount(year.netBenefit, currency)}</td>
                                <td className="px-3 py-3 font-semibold text-gray-800">
                                    {formatAmount(year.cumulativeNetBenefit, currency)}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}

/**
 * The baseline year by year, as the API computes it: the growth phase of the
 * trees, the share of their full yield, the harvest, and the net benefits
 * added up after the investment, until the year they cover it. The number of
 * years is chosen here and sent to the API; the same analysis gives the
 * payback of the baseline cards.
 */
export default function CashFlowByYearTable({ query, horizonYears, onHorizonChange }) {
    const { data, isPending, isError, refetch, isFetching } = query;

    return (
        <section aria-labelledby="cash-flow-title" className="flex flex-col gap-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                    <h3 id="cash-flow-title" className="font-heading text-base font-bold text-gray-900">
                        Cash Flow by Year
                    </h3>
                    {data && !isError && (
                        <p className="mt-1 text-sm font-semibold text-gray-800">
                            {paybackText(data.analysis.baseline, data.analysis.horizonYears)}
                        </p>
                    )}
                </div>
                <div className="flex items-center gap-2">
                    <label htmlFor="cash-flow-years" className="text-xs font-semibold text-gray-500">
                        Years shown:
                    </label>
                    <select
                        id="cash-flow-years"
                        value={horizonYears}
                        onChange={(event) => onHorizonChange(Number(event.target.value))}
                        className="rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm text-gray-800 focus:border-primary focus:outline-none"
                    >
                        {HORIZONS.map((years) => (
                            <option key={years} value={years}>
                                {years}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {isPending && (
                <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-4 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Computing the years…
                </div>
            )}

            {isError && (
                <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-error/30 bg-error/5 px-5 py-4">
                    <div className="flex items-start gap-3">
                        <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                        <div>
                            <p className="text-sm font-bold text-gray-900">The years could not be computed</p>
                            {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1, as in the other sections. */}
                            <p className="mt-0.5 text-sm text-gray-600">The request to the server did not succeed.</p>
                        </div>
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

            {data && !isError && <YearsTable analysis={data.analysis} />}

            <p className="text-xs text-gray-600">
                The trees are all planted before year 1, so in year n they are n − 1 years old and give the share of
                the full yield of their growth phase. The investment is spent before year 1, and the fixed cost runs
                from year 1, harvest or not.
            </p>
        </section>
    );
}
