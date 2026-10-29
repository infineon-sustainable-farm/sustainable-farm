import { Loader2, TriangleAlert } from "lucide-react";
import { useDebouncedValue, useScenarioAnalysis } from "../hooks/useScenarioAnalysis";
import {
    analysisParams,
    currencyUnit,
    formatAmount,
    formatMass,
    formatMoney,
    formatOtherAmount,
    formatReturn,
    formatSignedAmount,
    formatSignedKilograms,
    formatSignedPoints,
    MAX_YIELD_INCREASE,
    parseAdditionalInvestment,
} from "../utils/scenarioRequests";

const CARD_LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const NOTE_CLASS = "text-xs text-gray-600";
const INPUT_CLASS =
    "mt-1 w-full max-w-56 rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none";

/*
 * A gain in the module's green and a loss in its red, both darkened for text
 * on white: #257e55 -> 4.96:1 and #9a3f32 -> 7.03:1. The sign says it too.
 */
function toneOf(value) {
    if (value === null || value === undefined || value === 0) return "text-gray-700";
    return value > 0 ? "text-[#257e55]" : "text-[#9a3f32]";
}

function amountValue(amount, currency) {
    return amount?.[currency === "XOF" ? "xof" : "eur"];
}

/** The slider as it reads: "Baseline (0%)", "+10%". */
function increaseLabel(increase) {
    return increase === 0 ? "Baseline (0%)" : `+${increase}%`;
}

/** Which variant the figures shown are for, from the parameters the API computed them with. */
function resultsLabel(params, currency) {
    const increase = params.yieldPerTreeChangePct === 0 ? "the baseline yield (0%)" : `+${params.yieldPerTreeChangePct}% yield per tree`;
    const investment = params.additionalInvestment === undefined
        ? "no additional investment"
        : `${formatMoney(params.additionalInvestment, currency)} of additional investment`;
    return `Results for ${increase}, ${investment}`;
}

function ResultCard({ label, value, valueTone = "text-gray-900", change, changeTone, notes = [] }) {
    return (
        <li className="flex min-w-0 flex-col gap-1 rounded-xl border border-gray-200 bg-white px-4 py-4">
            <p className={CARD_LABEL_CLASS}>{label}</p>
            <p className={`font-heading text-lg font-bold break-words ${valueTone}`}>{value}</p>
            {change && <p className={`text-sm font-semibold ${changeTone}`}>{change}</p>}
            {notes.filter(Boolean).map((note) => (
                <p key={note} className={NOTE_CLASS}>
                    {note}
                </p>
            ))}
        </li>
    );
}

function Results({ analysis, params }) {
    const { variant, change, currency } = analysis;
    const noPrice = variant.revenue === null ? "Enter a price per kg to compute revenue." : null;
    const additional = change.additionalInvestment;
    return (
        <div className="flex flex-col gap-2">
            <ul className="grid grid-cols-2 gap-3 min-[900px]:grid-cols-4">
                <ResultCard
                    label="Projected Yield"
                    value={formatMass(variant.harvestKg)}
                    change={formatSignedKilograms(change.harvestKg)}
                    changeTone={toneOf(change.harvestKg)}
                />
                <ResultCard
                    label="Projected Revenue"
                    value={formatAmount(variant.revenue, currency)}
                    change={variant.revenue === null ? null : formatSignedAmount(change.revenue, currency)}
                    changeTone={toneOf(amountValue(change.revenue, currency))}
                    notes={[formatOtherAmount(variant.revenue, currency), noPrice]}
                />
                <ResultCard
                    label="Annual Benefit"
                    value={formatSignedAmount(change.annualBenefit, currency)}
                    valueTone={toneOf(amountValue(change.annualBenefit, currency))}
                    notes={[`Net benefit ${formatAmount(variant.netBenefit, currency)}`, noPrice]}
                />
                <ResultCard
                    label="Resulting ROI"
                    value={formatReturn(variant.roiPct)}
                    change={variant.roiPct === null ? null : formatSignedPoints(change.roiPctPoints)}
                    changeTone={toneOf(change.roiPctPoints)}
                />
            </ul>
            <p className="px-1 text-xs text-gray-600">
                {`Investment ${formatAmount(variant.investment, currency)}`
                    + (params.additionalInvestment === undefined
                        ? ", the baseline's"
                        : ` (${formatSignedAmount(additional, currency)} additional)`)}
            </p>
        </div>
    );
}

/**
 * The mock-up's simulator: the yield per tree raised by 0 to 50 %, with an
 * optional additional investment, against the baseline. The API computes the
 * variant once the slider stops; the figures of the previous value stay shown
 * meanwhile, under a label that names the variant they are for. The changes
 * come from the API with their sign.
 */
export default function YieldIncreaseSimulator({ scenario, yieldIncrease, onYieldIncreaseChange, investmentText, onInvestmentChange }) {
    const settledIncrease = useDebouncedValue(yieldIncrease);
    const settledInvestmentText = useDebouncedValue(investmentText);
    const settledInvestment = parseAdditionalInvestment(settledInvestmentText);
    const typedInvestment = parseAdditionalInvestment(investmentText);
    const query = useScenarioAnalysis(
        scenario.id,
        analysisParams({ yieldPerTreeChangePct: settledIncrease, additionalInvestment: settledInvestment.value }),
        { keepPrevious: true },
    );
    const { data, isPending, isError, error, refetch, isFetching, isPlaceholderData } = query;
    const serverError = isError && error.status === 400 ? error.data?.fieldErrors?.additionalInvestment : null;
    const investmentError = typedInvestment.error ?? serverError;
    const label = increaseLabel(yieldIncrease);

    return (
        <section aria-labelledby="yield-simulator-title" className="flex flex-col gap-4 rounded-xl border border-gray-200 bg-white p-5">
            <h3 id="yield-simulator-title" className="font-heading text-base font-bold text-gray-900">
                Yield Increase Simulator
            </h3>

            <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between gap-3 text-sm font-semibold text-gray-800">
                    <label htmlFor="yield-increase">Yield increase</label>
                    <span id="yield-increase-value">{label}</span>
                </div>
                <input
                    id="yield-increase"
                    type="range"
                    min="0"
                    max={MAX_YIELD_INCREASE}
                    step="1"
                    value={yieldIncrease}
                    onChange={(event) => onYieldIncreaseChange(Number(event.target.value))}
                    aria-valuetext={label}
                    className="w-full accent-primary"
                />
                <p className={NOTE_CLASS}>Computed by the server from the baseline assumptions.</p>
            </div>

            <div>
                <label htmlFor="additional-investment" className={`block ${CARD_LABEL_CLASS}`}>
                    {`Additional investment (${currencyUnit(scenario.currency)})`}
                    <span className="font-normal normal-case"> (optional)</span>
                </label>
                <input
                    id="additional-investment"
                    type="text"
                    inputMode="decimal"
                    value={investmentText}
                    onChange={(event) => onInvestmentChange(event.target.value)}
                    className={INPUT_CLASS}
                    aria-invalid={investmentError ? true : undefined}
                    aria-describedby={investmentError ? "additional-investment-error" : "additional-investment-note"}
                />
                {investmentError ? (
                    <p id="additional-investment-error" className="mt-1 text-xs text-error">
                        {investmentError}
                    </p>
                ) : (
                    <p id="additional-investment-note" className={`mt-1 ${NOTE_CLASS}`}>
                        {investmentText.trim() ? "Added to the investment of the baseline." : "No additional investment entered"}
                    </p>
                )}
            </div>

            {isPending && (
                <div className="flex items-center gap-3 py-4 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Computing the variant…
                </div>
            )}

            {isError && !serverError && (
                <div className="flex flex-col items-start gap-3 rounded-lg border border-error/30 bg-error/5 px-4 py-4">
                    <div className="flex items-start gap-3">
                        <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                        <div>
                            <p className="text-sm font-bold text-gray-900">The variant could not be computed</p>
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

            {data && !isError && (
                <div className="flex flex-col gap-3">
                    <p className="flex flex-wrap items-center gap-2 text-sm font-semibold text-gray-800" aria-live="polite">
                        <span id="simulator-results-label">{resultsLabel(data.params, scenario.currency)}</span>
                        {isPlaceholderData && isFetching && (
                            <span className="inline-flex items-center gap-1 font-normal text-gray-500">
                                <Loader2 size={14} className="animate-spin" />
                                Updating…
                            </span>
                        )}
                    </p>
                    <Results analysis={data.analysis} params={data.params} />
                </div>
            )}
        </section>
    );
}
