import { Link } from "react-router-dom";
import { formatCode, formatKilograms, formatNumber, formatShare, formatText, formatTimestamp, NO_VALUE } from "../utils/format";
import { formatMoney, formatUnitPrice } from "../utils/scenarioRequests";

const TH_CLASS =
    "border-b border-gray-200 bg-gray-50 px-4 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase";
const CURRENCY_NAMES = { XOF: "XOF (FCFA)", EUR: "EUR (€)" };

/** A value followed by its unit, or the dash alone when it is not entered. */
function withUnit(value, text, unit) {
    return value === null || value === undefined ? NO_VALUE : `${text} ${unit}`;
}

/** The assumptions of a scenario, in the order of its form, each with the source the API recorded. */
function assumptionRows(scenario) {
    const currency = scenario.currency;
    return [
        { label: "Variety", value: formatText(scenario.varietyName), source: null },
        { label: "Trees", value: formatNumber(scenario.treeCount), source: scenario.treeCountSource },
        {
            label: "Yield per tree",
            value: withUnit(scenario.yieldPerTreeKg, formatKilograms(scenario.yieldPerTreeKg), "per year in full production"),
            source: scenario.yieldSource,
        },
        {
            label: "Loss rate",
            value: withUnit(scenario.lossRate, formatShare(scenario.lossRate), "of the harvest"),
            source: scenario.lossRateSource,
        },
        { label: "Price per kg", value: formatUnitPrice(scenario.pricePerKg, currency), source: scenario.priceSource },
        { label: "Currency", value: CURRENCY_NAMES[currency] ?? formatText(currency), source: null },
        {
            label: "Fixed operating cost",
            value: withUnit(scenario.annualFixedCost, formatMoney(scenario.annualFixedCost, currency), "per year"),
            source: scenario.annualFixedCostSource,
        },
        {
            label: "Variable operating cost",
            value: withUnit(scenario.variableCostPerKg, formatUnitPrice(scenario.variableCostPerKg, currency),
                "per kg sold"),
            source: scenario.variableCostSource,
        },
        { label: "Investment", value: formatMoney(scenario.initialInvestment, currency), source: scenario.investmentSource },
        {
            label: "Cost per added tree",
            value: formatMoney(scenario.costPerAdditionalTree, currency),
            source: scenario.costPerAdditionalTreeSource,
        },
    ];
}

/**
 * The assumptions of the scenario as entered, each with its source, then the
 * yield share of each growth phase that its years use, as the API gives them.
 * The shares are those of the forecast: they are corrected on Harvest &
 * Maturity, and a correction counts here at once.
 */
export default function ScenarioAssumptionsTable({ scenario, phaseShares }) {
    return (
        <section aria-labelledby="scenario-assumptions-title" className="flex flex-col gap-4">
            <div>
                <h3 id="scenario-assumptions-title" className="font-heading text-base font-bold text-gray-900">
                    Assumptions
                </h3>
                <p className="mt-1 max-w-3xl text-sm text-gray-600">
                    {`As entered, each with its source. Last updated ${formatTimestamp(scenario.lastUpdated)}. A dash `
                        + "means not entered: the results that need the value are not computed."}
                </p>
            </div>

            <div className="relative overflow-x-auto rounded-xl border border-gray-200 bg-white">
                <table className="w-full border-collapse text-sm">
                    <thead>
                        <tr>
                            <th scope="col" className={TH_CLASS}>Assumption</th>
                            <th scope="col" className={TH_CLASS}>Value</th>
                            <th scope="col" className={TH_CLASS}>Source</th>
                        </tr>
                    </thead>
                    <tbody>
                        {assumptionRows(scenario).map((row) => (
                            <tr key={row.label} className="border-b border-gray-100 last:border-b-0">
                                <th scope="row" className="px-4 py-3 text-left font-medium whitespace-nowrap text-gray-800">
                                    {row.label}
                                </th>
                                <td className="px-4 py-3 text-gray-700">{row.value}</td>
                                <td className="px-4 py-3 break-words text-gray-700">{formatText(row.source)}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <div className="flex flex-col gap-3">
                <h4 className="font-heading text-sm font-bold text-gray-900">Yield share by growth phase</h4>
                {phaseShares ? (
                    <div className="relative overflow-x-auto rounded-xl border border-gray-200 bg-white">
                        <table className="w-full border-collapse text-sm whitespace-nowrap">
                            <thead>
                                <tr>
                                    <th scope="col" className={TH_CLASS}>Growth phase</th>
                                    <th scope="col" className={TH_CLASS}>Age</th>
                                    <th scope="col" className={TH_CLASS}>Share of the full yield</th>
                                    <th scope="col" className={TH_CLASS}>Source</th>
                                </tr>
                            </thead>
                            <tbody>
                                {phaseShares.map((share) => (
                                    <tr key={share.code} className="border-b border-gray-100 last:border-b-0">
                                        <th scope="row" className="px-4 py-3 text-left font-medium text-gray-800">
                                            {formatCode(share.code)}
                                        </th>
                                        <td className="px-4 py-3 text-gray-700">{formatText(share.yearsBand)}</td>
                                        <td className="px-4 py-3 text-gray-700">{formatShare(share.yieldShare)}</td>
                                        <td className="px-4 py-3 text-gray-700">{formatText(share.source)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <p className="text-sm text-gray-600">The shares come with the analysis of the scenario.</p>
                )}
                <p className="text-sm text-gray-600">
                    The yield forecast uses the same shares. They are corrected on{" "}
                    <Link to="/plants/harvest-maturity" className="font-semibold text-primary-dark underline">
                        Harvest &amp; Maturity
                    </Link>
                    ; a correction counts here at once.
                </p>
            </div>
        </section>
    );
}
