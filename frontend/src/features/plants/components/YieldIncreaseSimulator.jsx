import { CircleAlert, Loader2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Card, Input } from "../../../shared/ui";
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
import { ErrorBox, LoadingBox, RetryButton } from "./ScenarioStatus";
import { FIGURE_LABEL_CLASS, NOTE_CLASS, toneOf } from "./scenarioStyles";

function amountValue(amount, currency) {
    return amount?.[currency === "XOF" ? "xof" : "eur"];
}

/** The slider as it reads: "Baseline (0%)", "+10%". */
function increaseLabel(t, increase) {
    return increase === 0 ? t("scenarios.simulator.baselineStep") : t("scenarios.simulator.step", { value: increase });
}

/** Which variant the figures shown are for, from the parameters the API computed them with. */
function resultsLabel(t, params, currency) {
    const increase = params.yieldPerTreeChangePct === 0
        ? t("scenarios.simulator.resultsBaseline")
        : t("scenarios.simulator.resultsIncrease", { value: params.yieldPerTreeChangePct });
    const investment = params.additionalInvestment === undefined
        ? t("scenarios.simulator.resultsNoInvestment")
        : t("scenarios.simulator.resultsInvestment", { amount: formatMoney(params.additionalInvestment, currency) });
    return t("scenarios.simulator.results", { increase, investment });
}

function ResultCard({ label, value, valueTone = "text-text", change, changeTone, notes = [] }) {
    return (
        <li className="flex min-w-0 flex-col gap-1 rounded-tile border border-border-subtle bg-surface-raised p-4">
            <p className={FIGURE_LABEL_CLASS}>{label}</p>
            <p className={`text-lg font-bold break-words ${valueTone}`}>{value}</p>
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
    const { t } = useTranslation("plants");
    const { variant, change, currency } = analysis;
    const noPrice = variant.revenue === null ? t("scenarios.enterPriceRevenue") : null;
    const additional = change.additionalInvestment;
    const investment = formatAmount(variant.investment, currency);
    return (
        <div className="flex flex-col gap-2">
            <ul className="grid grid-cols-2 gap-3 lg:grid-cols-4">
                <ResultCard
                    label={t("scenarios.simulator.projectedYield")}
                    value={formatMass(variant.harvestKg)}
                    change={formatSignedKilograms(change.harvestKg)}
                    changeTone={toneOf(change.harvestKg, "text-text-muted")}
                />
                <ResultCard
                    label={t("scenarios.simulator.projectedRevenue")}
                    value={formatAmount(variant.revenue, currency)}
                    change={variant.revenue === null ? null : formatSignedAmount(change.revenue, currency)}
                    changeTone={toneOf(amountValue(change.revenue, currency), "text-text-muted")}
                    notes={[formatOtherAmount(variant.revenue, currency), noPrice]}
                />
                <ResultCard
                    label={t("scenarios.simulator.annualBenefit")}
                    value={formatSignedAmount(change.annualBenefit, currency)}
                    valueTone={toneOf(amountValue(change.annualBenefit, currency), "text-text-muted")}
                    notes={[t("scenarios.simulator.netBenefit", { amount: formatAmount(variant.netBenefit, currency) }), noPrice]}
                />
                <ResultCard
                    label={t("scenarios.simulator.resultingRoi")}
                    value={formatReturn(variant.roiPct)}
                    change={variant.roiPct === null ? null : formatSignedPoints(change.roiPctPoints)}
                    changeTone={toneOf(change.roiPctPoints, "text-text-muted")}
                />
            </ul>
            <p className={`px-1 ${NOTE_CLASS}`}>
                {params.additionalInvestment === undefined
                    ? t("scenarios.simulator.investmentBaseline", { amount: investment })
                    : t("scenarios.simulator.investmentAdditional", {
                        amount: investment,
                        additional: formatSignedAmount(additional, currency),
                    })}
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
    const { t } = useTranslation("plants");
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
    const label = increaseLabel(t, yieldIncrease);

    return (
        <Card
            aria-labelledby="yield-simulator-title"
            title={<span id="yield-simulator-title">{t("scenarios.simulator.title")}</span>}
            className="flex flex-col gap-4"
        >
            <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between gap-3 text-sm font-semibold text-text">
                    <label htmlFor="yield-increase">{t("scenarios.simulator.yieldIncrease")}</label>
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
                <p className={NOTE_CLASS}>{t("scenarios.simulator.computedNote")}</p>
            </div>

            <div>
                <label htmlFor="additional-investment" className="block text-sm font-medium">
                    {t("scenarios.simulator.additionalInvestment", { unit: currencyUnit(scenario.currency) })}
                    <span className="font-normal text-text-muted">{t("scenarios.optionalSuffix")}</span>
                </label>
                <div className="mt-1 max-w-56">
                    <Input
                        id="additional-investment"
                        type="text"
                        inputMode="decimal"
                        value={investmentText}
                        onChange={(event) => onInvestmentChange(event.target.value)}
                        aria-invalid={investmentError ? true : undefined}
                        aria-describedby={investmentError ? "additional-investment-error" : "additional-investment-note"}
                    />
                </div>
                {investmentError ? (
                    <p id="additional-investment-error" className="mt-1 flex items-center gap-1 text-xs text-error-fg">
                        <CircleAlert size={14} aria-hidden="true" className="shrink-0" />
                        <span>{investmentError}</span>
                    </p>
                ) : (
                    <p id="additional-investment-note" className={`mt-1 ${NOTE_CLASS}`}>
                        {investmentText.trim()
                            ? t("scenarios.simulator.investmentAdded")
                            : t("scenarios.simulator.investmentNone")}
                    </p>
                )}
            </div>

            {isPending && <LoadingBox bare label={t("scenarios.simulator.computing")} />}

            {isError && !serverError && (
                <ErrorBox
                    title={t("scenarios.simulator.error")}
                    message={error.message || t("scenarios.requestFailed")}
                    action={<RetryButton onClick={() => refetch()} isFetching={isFetching} />}
                />
            )}

            {data && !isError && (
                <div className="flex flex-col gap-3">
                    <p className="flex flex-wrap items-center gap-2 text-sm font-semibold text-text" aria-live="polite">
                        <span id="simulator-results-label">{resultsLabel(t, data.params, scenario.currency)}</span>
                        {isPlaceholderData && isFetching && (
                            <span className="inline-flex items-center gap-1 font-normal text-text-muted">
                                <Loader2 size={14} aria-hidden="true" className="animate-spin motion-reduce:animate-none" />
                                {t("scenarios.simulator.updating")}
                            </span>
                        )}
                    </p>
                    <Results analysis={data.analysis} params={data.params} />
                </div>
            )}
        </Card>
    );
}
