import { Loader2, TriangleAlert } from "lucide-react";
import { useDebouncedValue, useScenarioAnalysis } from "../hooks/useScenarioAnalysis";
import { formatKilograms, formatNumber, formatShare, formatText } from "../utils/format";
import {
    analysisParams,
    benefitLineDetails,
    benefitLineLabel,
    currencyUnit,
    formatAmount,
    formatMoney,
    formatOtherAmount,
    formatSignedAmount,
    formatSignedOtherAmount,
    formatSignedPercent,
    formatSignedTrees,
    formatUnitPrice,
    formatYears,
    otherCurrency,
    parseLossRate,
    parseTreeCountChange,
    parseYieldChangePct,
} from "../utils/scenarioRequests";

const TH_CLASS =
    "border-b border-gray-200 bg-gray-50 px-4 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase";

/** A gain in green, a loss in red, both darkened for text on white; the sign says it too. */
function toneOf(amount, currency) {
    const value = amount?.[currency === "XOF" ? "xof" : "eur"];
    if (value === null || value === undefined || value === 0) return "text-gray-800";
    return value > 0 ? "text-[#257e55]" : "text-[#9a3f32]";
}

/** A value with its source, or what its absence means. */
function sourced(text, source) {
    return `${text} (${formatText(source)})`;
}

/** What the benefit is computed from: the assumptions of the baseline and the levers entered, with their sources. */
function assumptionItems(scenario, levers) {
    const currency = scenario.currency;
    const items = [
        `Trees: ${sourced(formatNumber(scenario.treeCount), scenario.treeCountSource)}`,
        `Yield per tree: ${sourced(formatKilograms(scenario.yieldPerTreeKg), scenario.yieldSource)}`,
        `Price per kg: ${sourced(formatUnitPrice(scenario.pricePerKg, currency), scenario.priceSource)}`,
        scenario.lossRate === null
            ? "Loss rate: not entered, so the quantities are before losses"
            : `Loss rate: ${sourced(formatShare(scenario.lossRate), scenario.lossRateSource)}`,
        scenario.variableCostPerKg === null
            ? "Variable operating cost: not entered"
            : `Variable operating cost: ${sourced(`${formatUnitPrice(scenario.variableCostPerKg, currency)} per kg`,
                scenario.variableCostSource)}`,
        scenario.costPerAdditionalTree === null
            ? "Cost per added tree: not entered"
            : `Cost per added tree: ${sourced(formatMoney(scenario.costPerAdditionalTree, currency),
                scenario.costPerAdditionalTreeSource)}`,
    ];
    if (levers.treeCountChange !== null) items.push(`Trees added: ${formatSignedTrees(levers.treeCountChange)} (entered above)`);
    if (levers.yieldPerTreeChangePct !== null) {
        items.push(`Change of the yield per tree: ${formatSignedPercent(levers.yieldPerTreeChangePct)} (entered above)`);
    }
    if (levers.lossRate !== null) items.push(`Loss rate of the variant: ${formatShare(levers.lossRate)} (entered above)`);
    return items;
}

function BenefitTable({ analysis }) {
    const { change, currency } = analysis;
    return (
        <div className="relative overflow-x-auto rounded-xl border border-gray-200 bg-white">
            <table className="w-full border-collapse text-sm">
                <thead>
                    <tr>
                        <th scope="col" className={TH_CLASS}>Benefit type</th>
                        <th scope="col" className={TH_CLASS}>{`Amount (${currencyUnit(currency)})`}</th>
                        <th scope="col" className={TH_CLASS}>Details</th>
                    </tr>
                </thead>
                <tbody>
                    {change.benefitLines.map((line) => (
                        <tr key={line.type} className="border-b border-gray-100">
                            <th scope="row" className="px-4 py-3 text-left font-medium text-gray-800">
                                {benefitLineLabel(line)}
                            </th>
                            <td className={`px-4 py-3 font-semibold whitespace-nowrap ${toneOf(line.amount, currency)}`}>
                                {formatSignedAmount(line.amount, currency)}
                                {formatSignedOtherAmount(line.amount, currency) && (
                                    <span className="block text-xs font-normal text-gray-600">
                                        {formatSignedOtherAmount(line.amount, currency)}
                                    </span>
                                )}
                            </td>
                            <td className="px-4 py-3 text-gray-700">{benefitLineDetails(line, currency)}</td>
                        </tr>
                    ))}
                    <tr className="bg-gray-50">
                        <th scope="row" className="px-4 py-3 text-left font-bold tracking-wide text-gray-900">
                            TOTAL ANNUAL BENEFIT
                        </th>
                        <td className={`px-4 py-3 font-bold whitespace-nowrap ${toneOf(change.annualBenefit, currency)}`}>
                            {formatSignedAmount(change.annualBenefit, currency)}
                            {formatSignedOtherAmount(change.annualBenefit, currency) && (
                                <span className="block text-xs font-normal text-gray-600">
                                    {formatSignedOtherAmount(change.annualBenefit, currency)}
                                </span>
                            )}
                        </td>
                        <td className="px-4 py-3 text-gray-700">The lines above, added up.</td>
                    </tr>
                </tbody>
            </table>
        </div>
    );
}

/**
 * The annual benefit of the three levers applied together, in the format of
 * the kick-off example: one line per cause, the total, and the assumptions it
 * rests on, each with its source. The API applies the levers one after the
 * other (trees, yield per tree, losses), then counts the variable cost of what
 * is sold in addition, so its lines add up to the total; a line worth 0 is not
 * sent. The additional investment and its payback come below the table, as in
 * the example.
 */
export default function AnnualBenefitTable({ scenario, levers }) {
    const settledTrees = useDebouncedValue(levers.trees);
    const settledYield = useDebouncedValue(levers.yieldPct);
    const settledLoss = useDebouncedValue(levers.loss);
    const treeChange = parseTreeCountChange(settledTrees);
    const yieldChange = parseYieldChangePct(settledYield);
    const lossChange = parseLossRate(settledLoss);
    const values = {
        treeCountChange: treeChange.value,
        yieldPerTreeChangePct: yieldChange.value,
        lossRate: scenario.lossRate === null ? null : lossChange.value,
    };
    const hasLever = Object.values(values).some((value) => value !== null);
    const query = useScenarioAnalysis(scenario.id, analysisParams(values), { enabled: hasLever });
    const { data, isError, error, isFetching, refetch } = query;
    const analysis = data?.analysis;
    const currency = scenario.currency;

    return (
        <section aria-labelledby="annual-benefit-title" className="flex flex-col gap-4">
            <div>
                <h3 id="annual-benefit-title" className="font-heading text-base font-bold text-gray-900">
                    Annual Benefit
                </h3>
                <p className="mt-1 max-w-3xl text-sm text-gray-600">
                    The levers entered above, applied together, in a year of full production. Each line is what one
                    lever adds, in this order: trees, yield per tree, losses, then the variable cost of what is sold in
                    addition. The lines add up to the total.
                </p>
            </div>

            {!hasLever && (
                <p className="rounded-xl border border-gray-200 bg-white px-5 py-4 text-sm text-gray-600">
                    Enter at least one lever above to see the annual benefit of the levers together.
                </p>
            )}

            {hasLever && !data && !isError && (
                <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-4 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Computing the annual benefit…
                </div>
            )}

            {hasLever && isError && (
                <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-error/30 bg-error/5 px-5 py-4">
                    <div className="flex items-start gap-3">
                        <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                        <div>
                            <p className="text-sm font-bold text-gray-900">The annual benefit could not be computed</p>
                            {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1, as in the other sections. */}
                            <p className="mt-0.5 text-sm text-gray-600">{error.message || "The request to the server did not succeed."}</p>
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

            {hasLever && analysis && !isError && (
                <>
                    {analysis.change.annualBenefit === null ? (
                        <p className="rounded-xl border border-gray-200 bg-white px-5 py-4 text-sm text-gray-600">
                            Enter a price per kg in the baseline to compute the annual benefit.
                        </p>
                    ) : (
                        <BenefitTable analysis={analysis} />
                    )}
                    {formatSignedOtherAmount(analysis.change.annualBenefit, currency) && (
                        <p className="-mt-2 px-1 text-xs text-gray-600">
                            {`In ${currencyUnit(otherCurrency(currency))}, each amount is converted on its own: the `
                                + "total may differ by one unit from the sum of the lines."}
                        </p>
                    )}

                    <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                        <div className="rounded-xl border border-gray-200 bg-white px-4 py-3">
                            <dt className="text-xs font-semibold tracking-wide text-gray-500 uppercase">Additional investment</dt>
                            <dd className="mt-1 text-sm font-semibold text-gray-800">
                                {formatAmount(analysis.change.additionalInvestment, currency)}
                                {formatOtherAmount(analysis.change.additionalInvestment, currency)
                                    && ` (${formatOtherAmount(analysis.change.additionalInvestment, currency)})`}
                            </dd>
                            {analysis.change.additionalInvestment === null && (
                                <dd className="mt-1 text-xs text-gray-600">
                                    Enter a cost per added tree in the baseline to compute it.
                                </dd>
                            )}
                        </div>
                        <div className="rounded-xl border border-gray-200 bg-white px-4 py-3">
                            <dt className="text-xs font-semibold tracking-wide text-gray-500 uppercase">
                                Payback of the additional investment
                            </dt>
                            <dd className="mt-1 text-sm font-semibold text-gray-800">
                                {formatYears(analysis.change.additionalInvestmentPaybackYears)}
                            </dd>
                        </div>
                    </dl>

                    <div className="rounded-xl border border-gray-200 bg-white px-5 py-4">
                        <h4 className="font-heading text-sm font-bold text-gray-900">Assumptions</h4>
                        <ul className="mt-2 flex list-disc flex-col gap-1 pl-5 text-sm text-gray-700">
                            {assumptionItems(scenario, values).map((item) => (
                                <li key={item}>{item}</li>
                            ))}
                        </ul>
                    </div>
                </>
            )}
        </section>
    );
}
