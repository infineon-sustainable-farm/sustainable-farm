import { Loader2, Pencil, Plus, TriangleAlert } from "lucide-react";
import { formatNumber, formatText } from "../utils/format";
import {
    formatAmount,
    formatMass,
    formatOtherAmount,
    formatReturn,
    formatUnitPrice,
    formatYears,
} from "../utils/scenarioRequests";

const BUTTON_CLASS =
    "inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm font-semibold text-gray-700 hover:bg-gray-50";
const CARD_LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const CARD_VALUE_CLASS = "font-heading text-xl font-bold break-words text-gray-900";
const CARD_NOTE_CLASS = "text-xs text-gray-600";

function FigureCard({ label, value, notes = [], children }) {
    return (
        <li className="flex min-w-0 flex-col gap-1 rounded-xl border border-gray-200 bg-white px-4 py-4">
            {label && <p className={CARD_LABEL_CLASS}>{label}</p>}
            {value !== undefined && <p className={CARD_VALUE_CLASS}>{value}</p>}
            {notes.filter(Boolean).map((note) => (
                <p key={note} className={CARD_NOTE_CLASS}>
                    {note}
                </p>
            ))}
            {children}
        </li>
    );
}

/** The two paybacks of the API, under the names that say what each assumes. */
function PaybackCard({ baseline, horizonYears }) {
    const hasNetBenefit = baseline.netBenefit !== null;
    let simple = formatYears(baseline.simplePaybackYears);
    let withPhases = baseline.paybackYear === null ? null : `Year ${baseline.paybackYear}`;
    if (hasNetBenefit && baseline.simplePaybackYears === null) simple = "Not recovered";
    if (withPhases === null) withPhases = hasNetBenefit ? `Not within ${horizonYears} years` : formatText(null);
    return (
        <FigureCard label="Payback">
            <div className="mt-1 flex flex-col gap-2">
                <div>
                    <p className={CARD_NOTE_CLASS}>Simple payback (full production from year 1)</p>
                    <p className="font-heading text-base font-bold text-gray-900">{simple}</p>
                </div>
                <div>
                    <p className={CARD_NOTE_CLASS}>Payback with growth phases</p>
                    <p className="font-heading text-base font-bold text-gray-900">{withPhases}</p>
                </div>
            </div>
        </FigureCard>
    );
}

function BaselineFigures({ scenario, analysis }) {
    const { baseline, currency, horizonYears } = analysis;
    const yieldNote = baseline.lossKg === null
        ? "Before losses (no loss rate entered)"
        : `${formatMass(baseline.sellableKg)} sold after ${formatMass(baseline.lossKg)} of losses`;
    const noPrice = baseline.revenue === null ? "Enter a price per kg to compute revenue." : null;
    const otherInvestment = formatOtherAmount(baseline.investment, currency);

    return (
        <div className="flex flex-col gap-3">
            <ul className="grid grid-cols-2 gap-3 min-[900px]:grid-cols-3">
                <FigureCard
                    label="Total Trees"
                    value={formatNumber(baseline.treeCount)}
                    notes={[scenario.varietyName ? formatText(scenario.varietyName) : null]}
                />
                <FigureCard label="Annual Yield" value={formatMass(baseline.harvestKg)} notes={[yieldNote]} />
                <FigureCard
                    label="Annual Revenue"
                    value={formatAmount(baseline.revenue, currency)}
                    notes={[formatOtherAmount(baseline.revenue, currency), noPrice]}
                />
                <FigureCard
                    label="Annual Net Benefit"
                    value={formatAmount(baseline.netBenefit, currency)}
                    notes={[
                        formatOtherAmount(baseline.netBenefit, currency),
                        `Operating cost ${formatAmount(baseline.operatingCost, currency)}`,
                    ]}
                />
                <FigureCard
                    label="ROI"
                    value={formatReturn(baseline.roiPct)}
                    notes={["Net benefit of a year in full production, as a share of the investment."]}
                />
                <PaybackCard baseline={baseline} horizonYears={horizonYears} />
            </ul>
            <p className="px-1 text-xs text-gray-600">
                {`Total investment ${formatAmount(baseline.investment, currency)}`
                    + (otherInvestment ? ` (${otherInvestment})` : "")
                    + (scenario.pricePerKg === null
                        ? " · No price per kg entered"
                        : ` · Price ${formatUnitPrice(scenario.pricePerKg, currency)} per kg`)}
            </p>
            <p className="px-1 text-xs text-gray-600">
                Simple payback (full production from year 1): the investment divided by the net benefit of a year in
                full production, as if the trees gave their full yield from year 1. Payback with growth phases: the
                first year whose net benefits, added up year by year with the yield share of each growth phase, cover
                the investment.
            </p>
        </div>
    );
}

/**
 * The baseline scenario: its name, the scenarios to choose from when there
 * are several, and its figures in a year of full production, as the API
 * computes them. Nothing is computed here; an amount is shown in the currency
 * of the scenario, and in the other one when the API converted it.
 */
export default function ScenarioBaselineSection({
    scenarios,
    scenario,
    query,
    onSelect,
    onEdit,
    onAdd,
    editButtonRef,
    addButtonRef,
}) {
    const { data, isPending, isError, refetch, isFetching } = query;

    return (
        <section aria-labelledby="scenario-baseline-title" className="flex flex-col gap-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
                <h3 id="scenario-baseline-title" className="font-heading text-base font-bold break-words text-gray-900">
                    {`Baseline — ${scenario.name}`}
                </h3>
                <div className="flex flex-wrap items-center gap-2">
                    {scenarios.length > 1 && (
                        <div className="flex items-center gap-2">
                            <label htmlFor="scenario-select" className="text-xs font-semibold text-gray-500">
                                Scenario:
                            </label>
                            <select
                                id="scenario-select"
                                value={scenario.id}
                                onChange={(event) => onSelect(Number(event.target.value))}
                                className="rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm text-gray-800 focus:border-primary focus:outline-none"
                            >
                                {scenarios.map((candidate) => (
                                    <option key={candidate.id} value={candidate.id}>
                                        {candidate.name}
                                    </option>
                                ))}
                            </select>
                        </div>
                    )}
                    <button
                        ref={editButtonRef}
                        type="button"
                        onClick={onEdit}
                        aria-label={`Edit ${scenario.name}`}
                        className={BUTTON_CLASS}
                    >
                        <Pencil size={14} />
                        Edit
                    </button>
                    <button ref={addButtonRef} type="button" onClick={onAdd} className={BUTTON_CLASS}>
                        <Plus size={14} />
                        Add a scenario
                    </button>
                </div>
            </div>

            {isPending && (
                <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-4 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Computing the baseline…
                </div>
            )}

            {isError && (
                <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-error/30 bg-error/5 px-5 py-4">
                    <div className="flex items-start gap-3">
                        <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                        <div>
                            <p className="text-sm font-bold text-gray-900">The baseline could not be computed</p>
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

            {data && !isError && <BaselineFigures scenario={scenario} analysis={data.analysis} />}
        </section>
    );
}
