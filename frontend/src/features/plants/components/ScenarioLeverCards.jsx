import { Loader2 } from "lucide-react";
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

const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none disabled:cursor-not-allowed disabled:bg-gray-50 disabled:text-gray-500";

/*
 * A gain in the module's green and a loss in its red, both darkened for text
 * on white: #257e55 -> 4.96:1 and #9a3f32 -> 7.03:1. The sign says it too.
 */
function toneOf(value) {
    if (value === null || value === undefined || value === 0) return "text-gray-800";
    return value > 0 ? "text-[#257e55]" : "text-[#9a3f32]";
}

/** The three levers, each tried on its own against the baseline. */
const LEVERS = [
    {
        key: "trees",
        param: "treeCountChange",
        title: "More or fewer trees",
        inputLabel: "Trees added (negative to remove)",
        inputMode: "numeric",
        parse: parseTreeCountChange,
    },
    {
        key: "yieldPct",
        param: "yieldPerTreeChangePct",
        title: "Yield per tree",
        inputLabel: "Change of the yield per tree (%)",
        inputMode: "decimal",
        parse: parseYieldChangePct,
    },
    {
        key: "loss",
        param: "lossRate",
        title: "Lower losses",
        inputLabel: "Loss rate (0 to 1)",
        inputMode: "decimal",
        parse: parseLossRate,
    },
];

function Row({ label, value, tone = "text-gray-800", note }) {
    return (
        <div className="flex flex-col gap-0.5 border-b border-gray-100 py-2 last:border-b-0">
            <div className="flex flex-wrap items-baseline justify-between gap-x-3">
                <dt className="text-sm text-gray-600">{label}</dt>
                <dd className={`text-sm font-semibold ${tone}`}>{value}</dd>
            </div>
            {note && <p className="text-xs text-gray-600">{note}</p>}
        </div>
    );
}

/** The six rows of a lever, as the API computes its variant. */
function LeverRows({ analysis }) {
    const { variant, change, currency } = analysis;
    const additional = change.additionalInvestment;
    const hasNoAdditional = additional !== null && (currency === "XOF" ? additional.xof : additional.eur) === 0;
    let payback = formatYears(change.additionalInvestmentPaybackYears);
    if (hasNoAdditional) payback = "No additional investment";
    else if (additional !== null && change.additionalInvestmentPaybackYears === null && change.annualBenefit !== null) {
        payback = "Not recovered";
    }
    return (
        <dl className="flex flex-col">
            <Row
                label="Yield"
                value={`${formatMass(variant.harvestKg)} (${formatSignedKilograms(change.harvestKg)})`}
                tone={toneOf(change.harvestKg)}
            />
            <Row
                label="Revenue"
                value={variant.revenue === null
                    ? formatAmount(null, currency)
                    : `${formatAmount(variant.revenue, currency)} (${formatSignedAmount(change.revenue, currency)})`}
                note={variant.revenue === null ? "Enter a price per kg in the baseline to compute revenue." : null}
            />
            <Row
                label="Annual benefit"
                value={formatSignedAmount(change.annualBenefit, currency)}
                tone={toneOf(change.annualBenefit && (currency === "XOF" ? change.annualBenefit.xof : change.annualBenefit.eur))}
            />
            <Row
                label="Add'l investment"
                value={formatAmount(additional, currency)}
                note={additional === null ? "Enter a cost per added tree in the baseline to compute it." : null}
            />
            <Row
                label="ROI"
                value={variant.roiPct === null
                    ? formatReturn(null)
                    : `${formatReturn(variant.roiPct)} (${formatSignedPoints(change.roiPctPoints)})`}
                tone={toneOf(change.roiPctPoints)}
            />
            <Row label="Payback of add'l investment" value={payback} />
        </dl>
    );
}

function LeverCard({ lever, scenario, text, onChange }) {
    const disabledReason = lever.key === "loss" && scenario.lossRate === null
        ? "Enter the loss rate of the baseline first: a lower rate needs one to compare with."
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
            ? `The baseline loses ${formatShare(scenario.lossRate)} of its harvest. Enter a lower rate to see its effect.`
            : "Enter a value to see its effect.";
    }
    let describedBy;
    if (inputError) describedBy = `${inputId}-error`;
    else if (note) describedBy = `${inputId}-note`;

    return (
        <li className="flex min-w-0 flex-col gap-3 rounded-xl border border-gray-200 bg-white p-4">
            <h4 className="font-heading text-sm font-bold text-gray-900">{lever.title}</h4>
            <div>
                <label htmlFor={inputId} className="text-xs font-semibold tracking-wide text-gray-500 uppercase">
                    {lever.inputLabel}
                </label>
                <input
                    id={inputId}
                    type="text"
                    inputMode={lever.inputMode}
                    value={text}
                    disabled={disabledReason !== null}
                    onChange={(event) => onChange(lever.key, event.target.value)}
                    className={INPUT_CLASS}
                    aria-invalid={inputError ? true : undefined}
                    aria-describedby={describedBy}
                />
                {inputError && (
                    <p id={`${inputId}-error`} className="mt-1 text-xs text-error">
                        {inputError}
                    </p>
                )}
                {!inputError && note && (
                    <p id={`${inputId}-note`} className="mt-1 text-xs text-gray-600">
                        {note}
                    </p>
                )}
            </div>

            {showsResult && !data && !isError && (
                <div className="flex items-center gap-2 text-sm text-gray-500">
                    <Loader2 size={16} className="animate-spin" />
                    Computing…
                </div>
            )}
            {showsResult && isError && !serverError && (
                <div className="flex flex-col items-start gap-2 rounded-lg border border-error/30 bg-error/5 px-3 py-3">
                    <p className="text-sm text-gray-800">{error.message || "The request to the server did not succeed."}</p>
                    {(!error.status || error.status >= 500) && (
                        <button
                            type="button"
                            onClick={() => refetch()}
                            disabled={isFetching}
                            className="rounded-lg bg-primary px-3 py-1.5 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                        >
                            {isFetching ? "Retrying…" : "Retry"}
                        </button>
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
    return (
        <section aria-labelledby="scenario-levers-title" className="flex flex-col gap-4">
            <div>
                <h3 id="scenario-levers-title" className="font-heading text-base font-bold text-gray-900">
                    Production Increase Scenarios
                </h3>
                <p className="mt-1 max-w-3xl text-sm text-gray-600">
                    Each lever on its own, against the baseline. A variant is another plan, planted before year 1
                    like the baseline; removed trees save no investment.
                </p>
            </div>
            <ul className="grid grid-cols-1 gap-4 md:grid-cols-3">
                {LEVERS.map((lever) => (
                    <LeverCard key={lever.key} lever={lever} scenario={scenario} text={levers[lever.key]} onChange={onChange} />
                ))}
            </ul>
        </section>
    );
}
