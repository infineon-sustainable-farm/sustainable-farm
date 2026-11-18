import { CircleAlert } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Input } from "../../../shared/ui";
import { useDebouncedValue, useScenarioAnalysis } from "../hooks/useScenarioAnalysis";
import { formatShare } from "../utils/format";
import {
    analysisParams,
    formatAmount,
    formatMass,
    formatReturn,
    formatSignedAmount,
    formatSignedKilograms,
    formatSignedPoints,
    formatYears,
    parseLossRate,
    parseTreeCountChange,
    parseYieldChangePct,
} from "../utils/scenarioRequests";
import { LoadingBox, RetryButton, SectionHeading } from "./ScenarioStatus";
import { toneOf } from "./scenarioStyles";

/** The three levers, each tried on its own against the baseline. */
const LEVERS = [
    {
        key: "trees",
        param: "treeCountChange",
        inputMode: "numeric",
        parse: parseTreeCountChange,
    },
    {
        key: "yieldPct",
        param: "yieldPerTreeChangePct",
        inputMode: "decimal",
        parse: parseYieldChangePct,
    },
    {
        key: "loss",
        param: "lossRate",
        inputMode: "decimal",
        parse: parseLossRate,
    },
];

function Row({ label, value, tone = "text-text", note }) {
    return (
        <div className="flex flex-col gap-0.5 border-b border-border-subtle py-2 last:border-b-0">
            <div className="flex flex-wrap items-baseline justify-between gap-x-3">
                <dt className="text-sm text-text-muted">{label}</dt>
                <dd className={`text-sm font-semibold ${tone}`}>{value}</dd>
            </div>
            {note && <p className="text-xs text-text-muted">{note}</p>}
        </div>
    );
}

/** The six rows of a lever, as the API computes its variant. */
function LeverRows({ analysis }) {
    const { t } = useTranslation("plants");
    const { variant, change, currency } = analysis;
    const additional = change.additionalInvestment;
    const hasNoAdditional = additional !== null && (currency === "XOF" ? additional.xof : additional.eur) === 0;
    let payback = formatYears(change.additionalInvestmentPaybackYears);
    if (hasNoAdditional) payback = t("scenarios.levers.noAdditional");
    else if (additional !== null && change.additionalInvestmentPaybackYears === null && change.annualBenefit !== null) {
        payback = t("scenarios.notRecovered");
    }
    return (
        <dl className="flex flex-col">
            <Row
                label={t("scenarios.levers.yield")}
                value={`${formatMass(variant.harvestKg)} (${formatSignedKilograms(change.harvestKg)})`}
                tone={toneOf(change.harvestKg)}
            />
            <Row
                label={t("scenarios.levers.revenue")}
                value={variant.revenue === null
                    ? formatAmount(null, currency)
                    : `${formatAmount(variant.revenue, currency)} (${formatSignedAmount(change.revenue, currency)})`}
                note={variant.revenue === null ? t("scenarios.levers.noPrice") : null}
            />
            <Row
                label={t("scenarios.levers.annualBenefit")}
                value={formatSignedAmount(change.annualBenefit, currency)}
                tone={toneOf(change.annualBenefit && (currency === "XOF" ? change.annualBenefit.xof : change.annualBenefit.eur))}
            />
            <Row
                label={t("scenarios.levers.additionalInvestment")}
                value={formatAmount(additional, currency)}
                note={additional === null ? t("scenarios.enterCostPerTree") : null}
            />
            <Row
                label={t("scenarios.levers.roi")}
                value={variant.roiPct === null
                    ? formatReturn(null)
                    : `${formatReturn(variant.roiPct)} (${formatSignedPoints(change.roiPctPoints)})`}
                tone={toneOf(change.roiPctPoints)}
            />
            <Row label={t("scenarios.levers.additionalPayback")} value={payback} />
        </dl>
    );
}

function LeverCard({ lever, scenario, text, onChange }) {
    const { t } = useTranslation("plants");
    const disabledReason = lever.key === "loss" && scenario.lossRate === null
        ? t("scenarios.levers.lossDisabled")
        : null;
    const settledText = useDebouncedValue(text);
    const typed = lever.parse(text);
    const settled = lever.parse(settledText);
    const query = useScenarioAnalysis(scenario.id, analysisParams({ [lever.param]: settled.value }), {
        enabled: disabledReason === null && settled.value !== null,
    });
    const { data, isError, error, isFetching, refetch } = query;
    const serverError = isError && error.status === 400 ? error.data?.fieldErrors?.[lever.param] : null;
    const inputError = typed.error ?? serverError;
    const inputId = `lever-${lever.key}`;
    const showsResult = disabledReason === null && typed.error === null && settled.value !== null;
    let note = disabledReason;
    if (note === null && text.trim() === "") {
        note = lever.key === "loss"
            ? t("scenarios.levers.lossNote", { rate: formatShare(scenario.lossRate) })
            : t("scenarios.levers.enterValue");
    }
    let describedBy;
    if (inputError) describedBy = `${inputId}-error`;
    else if (note) describedBy = `${inputId}-note`;

    return (
        <li className="flex min-w-0 flex-col gap-3 rounded-tile border border-border-subtle bg-surface-raised p-4">
            <h3 className="text-sm font-semibold text-text">{t(`scenarios.levers.${lever.key}.title`)}</h3>
            <div>
                <label htmlFor={inputId} className="text-sm font-medium">
                    {t(`scenarios.levers.${lever.key}.input`)}
                </label>
                <Input
                    id={inputId}
                    type="text"
                    inputMode={lever.inputMode}
                    value={text}
                    disabled={disabledReason !== null}
                    onChange={(event) => onChange(lever.key, event.target.value)}
                    className="mt-1"
                    aria-invalid={inputError ? true : undefined}
                    aria-describedby={describedBy}
                />
                {inputError && (
                    <p id={`${inputId}-error`} className="mt-1 flex items-center gap-1 text-xs text-error-fg">
                        <CircleAlert size={14} aria-hidden="true" className="shrink-0" />
                        <span>{inputError}</span>
                    </p>
                )}
                {!inputError && note && (
                    <p id={`${inputId}-note`} className="mt-1 text-xs text-text-muted">
                        {note}
                    </p>
                )}
            </div>

            {showsResult && !data && !isError && <LoadingBox bare label={t("scenarios.levers.computing")} />}
            {showsResult && isError && !serverError && (
                <div className="flex flex-col items-start gap-2 rounded-control bg-error-tint px-3 py-3">
                    <p className="text-sm text-text">{error.message || t("scenarios.requestFailed")}</p>
                    {(!error.status || error.status >= 500) && (
                        <RetryButton size="sm" onClick={() => refetch()} isFetching={isFetching} />
                    )}
                </div>
            )}
            {showsResult && data && !isError && <LeverRows analysis={data.analysis} />}
        </li>
    );
}

/**
 * The mock-up's three production scenarios, as levers the user enters: trees
 * added or removed, a change of the yield per tree, a lower loss rate. Each is
 * tried on its own against the baseline, and its six rows come from the API.
 * A card stays empty until a value is entered.
 */
export default function ScenarioLeverCards({ scenario, levers, onChange }) {
    const { t } = useTranslation("plants");
    return (
        <section aria-labelledby="scenario-levers-title" className="flex flex-col gap-4">
            <SectionHeading id="scenario-levers-title" title={t("scenarios.levers.title")}>
                {t("scenarios.levers.intro")}
            </SectionHeading>
            <ul className="grid grid-cols-1 gap-4 md:grid-cols-3">
                {LEVERS.map((lever) => (
                    <LeverCard key={lever.key} lever={lever} scenario={scenario} text={levers[lever.key]} onChange={onChange} />
                ))}
            </ul>
        </section>
    );
}
