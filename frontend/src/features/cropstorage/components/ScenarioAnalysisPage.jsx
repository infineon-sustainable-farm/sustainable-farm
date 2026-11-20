import { useEffect, useId, useMemo, useState } from "react";
import { FileText, Info, SlidersHorizontal, TrendingUp } from "lucide-react";
import {
    Bar,
    BarChart,
    CartesianGrid,
    Legend,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from "recharts";
import {
    INITIAL_PRESET,
    OUTLOOK,
    OUTLOOK_LABELS,
    SCENARIO_ASSUMPTIONS as A,
    SCENARIO_LIMITS,
    SCENARIO_PRESETS,
    buildScenarioSummary,
    computeScenario,
    formatBreakEven,
    formatFcfa,
    formatPoints,
    formatTonnes,
} from "../utils/scenario";

const PRESET_BUTTONS = [
    { key: "small", label: "Small expansion" },
    { key: "full", label: "Full upgrade" },
    { key: "solar", label: "Solar power only" },
];

const SUMMARY_DURATION_MS = 5500;

const compactFcfa = new Intl.NumberFormat("en-US", {
    notation: "compact",
    maximumFractionDigits: 1,
});

const CARD = "flex flex-col gap-4 rounded-xl border border-gray-200 bg-white p-5";
const FOCUS = "focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2";

function SliderControl({ label, value, unit, limits, onChange }) {
    const id = useId();
    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between gap-3">
                <label htmlFor={id} className="text-sm font-medium text-gray-700">
                    {label}
                </label>
                <output htmlFor={id} className="text-sm font-bold whitespace-nowrap text-primary">
                    {value} {unit}
                </output>
            </div>
            <input
                id={id}
                type="range"
                min={limits.min}
                max={limits.max}
                step={limits.step}
                value={value}
                aria-valuetext={`${value} ${unit}`}
                onChange={(event) => onChange(Number(event.target.value))}
                className={`h-1.5 w-full cursor-pointer accent-primary ${FOCUS}`}
            />
            <div className="flex justify-between text-xs text-gray-400" aria-hidden="true">
                <span>{limits.min}</span>
                <span>{limits.max}</span>
            </div>
        </div>
    );
}

function ToggleControl({ label, hint, checked, onChange }) {
    const id = useId();
    const hintId = `${id}-hint`;
    return (
        <div className="flex items-start justify-between gap-4 rounded-lg border border-gray-100 px-3 py-3">
            <div>
                <label htmlFor={id} className="text-sm font-medium text-gray-700">
                    {label}
                </label>
                <p id={hintId} className="mt-0.5 text-xs text-gray-500">
                    {hint}
                </p>
            </div>
            <input
                id={id}
                type="checkbox"
                role="switch"
                checked={checked}
                aria-describedby={hintId}
                onChange={(event) => onChange(event.target.checked)}
                className={`mt-1 h-5 w-5 shrink-0 cursor-pointer rounded accent-primary ${FOCUS}`}
            />
        </div>
    );
}

function ResultRow({ label, value, tone }) {
    const toneClass =
        tone === "good" ? "text-success" : tone === "bad" ? "text-error" : "text-gray-900";
    return (
        <div className="flex items-center justify-between gap-3 rounded-lg border border-gray-100 bg-gray-50 px-3 py-2.5 text-sm">
            <dt className="text-gray-500">{label}</dt>
            <dd className={`text-right font-bold ${toneClass}`}>{value}</dd>
        </div>
    );
}

export default function ScenarioAnalysisPage() {
    const [inputs, setInputs] = useState({ ...SCENARIO_PRESETS[INITIAL_PRESET] });
    const [activePreset, setActivePreset] = useState(INITIAL_PRESET);
    const [summary, setSummary] = useState(null);

    useEffect(() => {
        document.title = "Scenario Analysis — Crop Storage";
    }, []);

    useEffect(() => {
        if (!summary) return undefined;
        const timer = window.setTimeout(() => setSummary(null), SUMMARY_DURATION_MS);
        return () => window.clearTimeout(timer);
    }, [summary]);

    const result = useMemo(() => computeScenario(inputs), [inputs]);

    function applyPreset(key) {
        setInputs({ ...SCENARIO_PRESETS[key] });
        setActivePreset(key);
    }

    function updateInput(field, value) {
        setInputs((current) => ({ ...current, [field]: value }));
        // As in the prototype, a manual change clears the active preset highlight.
        setActivePreset(null);
    }

    const netTone = result.annualNetBenefit > 0 ? "good" : "bad";
    const fiveYearTone = result.fiveYearNetGain > 0 ? "good" : "bad";
    const outlookTone =
        result.outlook === OUTLOOK.PROFITABLE
            ? "good"
            : result.outlook === OUTLOOK.BEYOND
              ? undefined
              : "bad";

    const chartData = result.projection.map((row) => ({
        name: `Year ${row.year}`,
        cost: Math.round(row.cumulativeCost),
        benefit: Math.round(row.cumulativeBenefit),
    }));

    return (
        <div className="min-h-dvh bg-gray-50">
            <div className="mx-auto flex max-w-7xl flex-col gap-5 px-4 py-8 sm:px-6">
                <header>
                    <p className="font-heading text-xs font-bold tracking-widest text-primary uppercase">
                        Crop Storage
                    </p>
                    <h1 className="font-heading mt-1 text-3xl font-bold text-gray-900">
                        What-if Scenario Analysis
                    </h1>
                    <p className="mt-2 text-sm text-gray-500">
                        Simulate investment hypotheses and projected ROI.
                    </p>
                </header>

                <div
                    role="note"
                    className="flex items-start gap-3 rounded-xl border border-warning/40 bg-warning/10 p-4 text-sm text-gray-800"
                >
                    <Info size={18} className="mt-0.5 shrink-0 text-warning" aria-hidden="true" />
                    <p>
                        <strong>Demonstration simulation.</strong> All values and results are
                        provisional, unvalidated assumptions — not farm measurements and not
                        investment advice. This page calculates locally in your browser: it does not
                        change any real storage capacity, does not write to the database and does not
                        trigger any decision or authorisation.
                    </p>
                </div>

                <div role="group" aria-label="Scenario presets" className="flex flex-wrap gap-2.5">
                    {PRESET_BUTTONS.map(({ key, label }) => (
                        <button
                            key={key}
                            type="button"
                            aria-pressed={activePreset === key}
                            onClick={() => applyPreset(key)}
                            className={`rounded-lg border px-4 py-2.5 text-sm font-semibold transition ${FOCUS} ${
                                activePreset === key
                                    ? "border-primary bg-primary/15 text-primary"
                                    : "border-gray-200 bg-white text-gray-800 hover:border-primary"
                            }`}
                        >
                            {label}
                        </button>
                    ))}
                </div>

                <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1fr_1.2fr]">
                    <section aria-labelledby="scenario-hypothesis-title" className={CARD}>
                        <h2
                            id="scenario-hypothesis-title"
                            className="flex items-center gap-2 text-lg font-semibold text-gray-900"
                        >
                            <SlidersHorizontal size={18} className="text-primary" aria-hidden="true" />
                            Hypothesis
                        </h2>

                        <SliderControl
                            label="Additional storage capacity"
                            value={inputs.storageM2}
                            unit="m²"
                            limits={SCENARIO_LIMITS.storage}
                            onChange={(value) => updateInput("storageM2", value)}
                        />
                        <SliderControl
                            label="Additional sensors"
                            value={inputs.sensors}
                            unit="sensors"
                            limits={SCENARIO_LIMITS.sensors}
                            onChange={(value) => updateInput("sensors", value)}
                        />
                        <ToggleControl
                            label="Cooling upgrade (refrigeration)"
                            hint="Reduces post-harvest losses by 30% (relative)."
                            checked={inputs.cooling}
                            onChange={(value) => updateInput("cooling", value)}
                        />
                        <ToggleControl
                            label="Solar power installation"
                            hint="Saves ~80% of the annual electricity bill."
                            checked={inputs.solar}
                            onChange={(value) => updateInput("solar", value)}
                        />

                        <div className="rounded-lg border border-gray-100 bg-gray-50 p-3">
                            <h3 className="text-sm font-semibold text-gray-900">Data assumptions</h3>
                            <dl className="mt-2 grid grid-cols-1 gap-x-4 gap-y-1 text-xs sm:grid-cols-2">
                                {[
                                    ["Storage cost per m²", formatFcfa(A.costPerM2)],
                                    ["Sensor cost", formatFcfa(A.costPerSensor)],
                                    ["Mango reference value", `${formatFcfa(A.pricePerTonne)} / t`],
                                    ["Baseline loss rate", `${A.baseLossPct} %`],
                                    ["Unmet storage demand", `${A.unmetDemandTonnes} t / year (max)`],
                                    ["Analysis period", `${A.projectionYears} years`],
                                ].map(([label, value]) => (
                                    <div key={label} className="flex justify-between gap-2">
                                        <dt className="text-gray-500">{label}</dt>
                                        <dd className="font-semibold text-gray-800">{value}</dd>
                                    </div>
                                ))}
                            </dl>
                            <details className="mt-3 text-xs">
                                <summary className={`cursor-pointer font-semibold text-primary ${FOCUS}`}>
                                    All assumptions used by the formulas
                                </summary>
                                <dl className="mt-2 grid grid-cols-1 gap-x-4 gap-y-1 sm:grid-cols-2">
                                    {[
                                        ["Cooling investment", formatFcfa(A.coolingCost)],
                                        ["Solar investment", formatFcfa(A.solarCost)],
                                        ["Added capacity", `${A.tonnesPerM2} t / m²`],
                                        [
                                            "Sensor effect",
                                            `−${A.sensorLossReductionPts} pt each, max ${A.maxEffectiveSensors} effective`,
                                        ],
                                        [
                                            "Cooling effect",
                                            `−${A.coolingRelativeReduction * 100} % relative on remaining loss`,
                                        ],
                                        ["Annual electricity bill", formatFcfa(A.annualElectricity)],
                                        ["Solar electricity saving", `${A.solarSavingsRate * 100} %`],
                                        [
                                            "Reference used surface (fictitious)",
                                            `${A.baseStorageM2} m²`,
                                        ],
                                        ["Margin on used added capacity", `${formatFcfa(A.marginPerTonne)} / t`],
                                        ["Storage upkeep", `${formatFcfa(A.storageMaintenancePerM2)} / m² / year`],
                                        ["Sensor upkeep", `${A.sensorMaintenanceRate * 100} % of cost / year`],
                                        ["Cooling operation", `${formatFcfa(A.coolingOperatingCost)} / year`],
                                        ["Solar upkeep", `${A.solarMaintenanceRate * 100} % of cost / year`],
                                    ].map(([label, value]) => (
                                        <div key={label} className="flex justify-between gap-2">
                                            <dt className="text-gray-500">{label}</dt>
                                            <dd className="text-right font-semibold text-gray-800">{value}</dd>
                                        </div>
                                    ))}
                                </dl>
                            </details>
                            <p className="mt-3 text-xs text-gray-500">
                                Annual net benefit equals gross benefit minus operating cost. Break-even
                                is calculated from the initial investment and the annual net benefit.
                            </p>
                            <p className="mt-1 text-xs text-gray-500">
                                Basis: provisional demonstration assumptions. Validation with operational
                                farm data is still required.
                            </p>
                        </div>
                    </section>

                    <section aria-labelledby="scenario-results-title" className={CARD}>
                        <h2
                            id="scenario-results-title"
                            className="flex items-center gap-2 text-lg font-semibold text-gray-900"
                        >
                            <TrendingUp size={18} className="text-primary" aria-hidden="true" />
                            Results
                        </h2>

                        <dl className="flex flex-col gap-2" aria-live="polite">
                            <ResultRow label="Initial investment" value={formatFcfa(result.investment)} />
                            <ResultRow
                                label="Additional capacity"
                                value={formatTonnes(result.addedCapacityTonnes)}
                            />
                            <ResultRow
                                label="Expected capacity used"
                                value={formatTonnes(result.usedAddedCapacityTonnes)}
                            />
                            <ResultRow
                                label="Estimated loss reduction"
                                value={formatPoints(result.lossReductionPts)}
                            />
                            <ResultRow label="Annual operating cost" value={formatFcfa(result.annualCost)} />
                            <ResultRow
                                label="Annual gross benefit"
                                value={formatFcfa(result.annualGrossBenefit)}
                            />
                            <ResultRow
                                label="Annual net benefit"
                                value={formatFcfa(result.annualNetBenefit)}
                                tone={netTone}
                            />
                            <ResultRow
                                label="Break-even point"
                                value={formatBreakEven(result.breakEvenMonths)}
                            />
                            <ResultRow
                                label="Net gain after 5 years"
                                value={formatFcfa(result.fiveYearNetGain, { signed: true })}
                                tone={fiveYearTone}
                            />
                            <ResultRow
                                label="Financial outlook"
                                value={OUTLOOK_LABELS[result.outlook]}
                                tone={outlookTone}
                            />
                        </dl>
                        <p className="text-xs text-gray-500">
                            Results are calculated from the assumptions shown in the Hypothesis panel.
                            Loss reduction is in percentage points of the {A.baseLossPct} % baseline.
                        </p>

                        <figure className="flex flex-col gap-2">
                            <figcaption className="text-sm font-semibold text-gray-900">
                                Cumulative cost vs cumulative benefit (FCFA)
                            </figcaption>
                            <div className="h-64 w-full" aria-hidden="true">
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={chartData} margin={{ top: 8, right: 8, bottom: 24, left: 16 }}>
                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5e7eb" />
                                        <XAxis
                                            dataKey="name"
                                            tick={{ fontSize: 12 }}
                                            label={{ value: "Projection year", position: "insideBottom", offset: -16, fontSize: 12 }}
                                        />
                                        <YAxis
                                            tickFormatter={(value) => compactFcfa.format(value)}
                                            tick={{ fontSize: 12 }}
                                            label={{
                                                value: "Cumulative amount (FCFA)",
                                                angle: -90,
                                                position: "insideLeft",
                                                offset: -8,
                                                fontSize: 12,
                                            }}
                                        />
                                        <Tooltip formatter={(value) => formatFcfa(value)} />
                                        <Legend verticalAlign="top" height={28} />
                                        <Bar dataKey="cost" name="Cumulative cost" fill="#c1503f" radius={[6, 6, 0, 0]} isAnimationActive={false} />
                                        <Bar dataKey="benefit" name="Cumulative benefit" fill="#2e9e6b" radius={[6, 6, 0, 0]} isAnimationActive={false} />
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                            <details className="text-xs" open>
                                <summary className={`cursor-pointer font-semibold text-primary ${FOCUS}`}>
                                    Chart values as a table
                                </summary>
                                <div className="mt-2 overflow-x-auto">
                                    <table className="w-full text-left">
                                        <caption className="sr-only">
                                            Cumulative cost, benefit and net gain per projection year, in FCFA
                                        </caption>
                                        <thead>
                                            <tr className="border-b border-gray-100 text-gray-500">
                                                <th scope="col" className="py-1 pr-3 font-semibold">Year</th>
                                                <th scope="col" className="py-1 pr-3 text-right font-semibold">Cumulative cost</th>
                                                <th scope="col" className="py-1 pr-3 text-right font-semibold">Cumulative benefit</th>
                                                <th scope="col" className="py-1 text-right font-semibold">Cumulative net</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {result.projection.map((row) => (
                                                <tr key={row.year} className="border-b border-gray-50">
                                                    <th scope="row" className="py-1 pr-3 font-medium text-gray-700">
                                                        Year {row.year}
                                                    </th>
                                                    <td className="py-1 pr-3 text-right tabular-nums">{formatFcfa(row.cumulativeCost)}</td>
                                                    <td className="py-1 pr-3 text-right tabular-nums">{formatFcfa(row.cumulativeBenefit)}</td>
                                                    <td className="py-1 text-right tabular-nums">
                                                        {formatFcfa(row.cumulativeNet, { signed: true })}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </details>
                        </figure>

                        <button
                            type="button"
                            onClick={() => setSummary(buildScenarioSummary(result))}
                            className={`inline-flex w-full items-center justify-center gap-2 rounded-lg border border-primary px-4 py-2.5 text-sm font-semibold text-primary hover:bg-primary/10 ${FOCUS}`}
                        >
                            <FileText size={16} aria-hidden="true" />
                            Show summary
                        </button>
                        <p className="-mt-2 text-center text-xs text-gray-400">
                            Displays a temporary on-screen summary; no file is downloaded.
                        </p>
                        <div role="status" aria-live="polite" aria-label="Scenario summary">
                            {summary && (
                                <p className="rounded-lg border border-primary/30 bg-primary/5 p-3 text-sm text-gray-800">
                                    <span className="font-semibold">Summary (simulation)</span> — {summary}
                                </p>
                            )}
                        </div>
                    </section>
                </div>
            </div>
        </div>
    );
}
