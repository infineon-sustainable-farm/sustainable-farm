import { useEffect, useId, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
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
import { Button, PageHeader } from "../../../shared/ui";
import { currentLocale } from "../utils/formatters";
import { usePageTitle } from "../utils/usePageTitle";
import {
    INITIAL_PRESET,
    OUTLOOK,
    SCENARIO_ASSUMPTIONS as A,
    SCENARIO_LIMITS,
    SCENARIO_PRESETS,
    buildScenarioSummary,
    computeScenario,
    formatBreakEven,
    formatFcfa,
    formatPoints,
    formatTonnes,
    outlookLabel,
} from "../utils/scenario";

const PRESET_KEYS = ["small", "full", "solar"];
// Recharts axis label placement (not interface text).
const X_LABEL_POSITION = "insideBottom";
const Y_LABEL_POSITION = "insideLeft";

const SUMMARY_DURATION_MS = 5500;

const CARD = "flex flex-col gap-4 rounded-card border border-border-subtle bg-surface-raised p-5 shadow-raised";
const FOCUS = "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

function SliderControl({ label, value, unit, limits, onChange }) {
    const id = useId();
    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between gap-3">
                <label htmlFor={id} className="text-sm font-medium text-text">
                    {label}
                </label>
                <output htmlFor={id} className="text-sm font-bold whitespace-nowrap text-text-accent">
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
            <div className="flex justify-between text-xs text-text-muted" aria-hidden="true">
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
        <div className="flex items-start justify-between gap-4 rounded-control border border-border-subtle px-3 py-3">
            <div>
                <label htmlFor={id} className="text-sm font-medium text-text">
                    {label}
                </label>
                <p id={hintId} className="mt-0.5 text-xs text-text-muted">
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
    const toneClass = tone === "good" ? "text-success-fg" : tone === "bad" ? "text-error-fg" : "text-text";
    return (
        <div className="flex items-center justify-between gap-3 rounded-control border border-border-subtle bg-surface-muted px-3 py-2.5 text-sm">
            <dt className="text-text-muted">{label}</dt>
            <dd className={`text-right font-bold ${toneClass}`}>{value}</dd>
        </div>
    );
}

export default function ScenarioAnalysisPage() {
    const { t } = useTranslation("cropstorage");
    usePageTitle("scenario.title");
    const [inputs, setInputs] = useState({ ...SCENARIO_PRESETS[INITIAL_PRESET] });
    const [activePreset, setActivePreset] = useState(INITIAL_PRESET);
    const [summary, setSummary] = useState(null);

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
        name: t("scenario.year", { year: row.year }),
        cost: Math.round(row.cumulativeCost),
        benefit: Math.round(row.cumulativeBenefit),
    }));

    const compactFcfa = new Intl.NumberFormat(currentLocale(), { notation: "compact", maximumFractionDigits: 1 });
    const assumptionRows = [
        ["storageCost", formatFcfa(A.costPerM2)],
        ["sensorCost", formatFcfa(A.costPerSensor)],
        ["mangoValue", t("scenario.units.perTonne", { value: formatFcfa(A.pricePerTonne) })],
        ["baselineLoss", t("scenario.units.percent", { value: A.baseLossPct })],
        ["unmetDemand", t("scenario.units.tonnesPerYear", { value: A.unmetDemandTonnes })],
        ["period", t("scenario.units.years", { value: A.projectionYears })],
    ];
    const allAssumptionRows = [
        ["coolingInvestment", formatFcfa(A.coolingCost)],
        ["solarInvestment", formatFcfa(A.solarCost)],
        ["addedCapacity", t("scenario.units.tonnesPerM2", { value: A.tonnesPerM2 })],
        ["sensorEffect", t("scenario.units.sensorEffect", { value: A.sensorLossReductionPts, max: A.maxEffectiveSensors })],
        ["coolingEffect", t("scenario.units.coolingEffect", { value: A.coolingRelativeReduction * 100 })],
        ["electricityBill", formatFcfa(A.annualElectricity)],
        ["solarSaving", t("scenario.units.percent", { value: A.solarSavingsRate * 100 })],
        ["referenceSurface", t("scenario.units.squareMeters", { value: A.baseStorageM2 })],
        ["margin", t("scenario.units.perTonne", { value: formatFcfa(A.marginPerTonne) })],
        ["storageUpkeep", t("scenario.units.perM2PerYear", { value: formatFcfa(A.storageMaintenancePerM2) })],
        ["sensorUpkeep", t("scenario.units.percentOfCostPerYear", { value: A.sensorMaintenanceRate * 100 })],
        ["coolingOperation", t("scenario.units.perYear", { value: formatFcfa(A.coolingOperatingCost) })],
        ["solarUpkeep", t("scenario.units.percentOfCostPerYear", { value: A.solarMaintenanceRate * 100 })],
    ];

    return (
        <div className="flex flex-col gap-5">
            <PageHeader title={t("scenario.heading")} subtitle={t("scenario.subtitle")} />

            <div role="note" className="flex items-start gap-3 rounded-card bg-warning-tint p-4 text-sm text-text">
                <Info size={18} className="mt-0.5 shrink-0 text-warning-fg" aria-hidden="true" />
                <p>
                    <strong>{t("scenario.disclaimerTitle")}</strong> {t("scenario.disclaimer")}
                </p>
            </div>

            <div role="group" aria-label={t("scenario.presets")} className="flex flex-wrap gap-2.5">
                {PRESET_KEYS.map((key) => (
                    <button
                        key={key}
                        type="button"
                        aria-pressed={activePreset === key}
                        onClick={() => applyPreset(key)}
                        className={`rounded-control border px-4 py-2.5 text-sm font-semibold transition duration-(--duration-fast) ease-standard ${FOCUS} ${
                            activePreset === key
                                ? "border-primary bg-primary-tint text-text-accent"
                                : "border-border-input bg-surface-raised text-text hover:border-primary"
                        }`}
                    >
                        {t(`scenario.preset.${key}`)}
                    </button>
                ))}
            </div>

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1fr_1.2fr]">
                <section aria-labelledby="scenario-hypothesis-title" className={CARD}>
                    <h2 id="scenario-hypothesis-title" className="flex items-center gap-2 text-lg font-semibold text-text">
                        <SlidersHorizontal size={18} className="text-text-accent" aria-hidden="true" />
                        {t("scenario.hypothesis")}
                    </h2>

                    <SliderControl
                        label={t("scenario.inputs.storage")}
                        value={inputs.storageM2}
                        unit={t("scenario.units.m2")}
                        limits={SCENARIO_LIMITS.storage}
                        onChange={(value) => updateInput("storageM2", value)}
                    />
                    <SliderControl
                        label={t("scenario.inputs.sensors")}
                        value={inputs.sensors}
                        unit={t("scenario.units.sensors")}
                        limits={SCENARIO_LIMITS.sensors}
                        onChange={(value) => updateInput("sensors", value)}
                    />
                    <ToggleControl
                        label={t("scenario.inputs.cooling")}
                        hint={t("scenario.inputs.coolingHint")}
                        checked={inputs.cooling}
                        onChange={(value) => updateInput("cooling", value)}
                    />
                    <ToggleControl
                        label={t("scenario.inputs.solar")}
                        hint={t("scenario.inputs.solarHint")}
                        checked={inputs.solar}
                        onChange={(value) => updateInput("solar", value)}
                    />

                    <div className="rounded-control border border-border-subtle bg-surface-muted p-3">
                        <h3 className="text-sm font-semibold text-text">{t("scenario.assumptionsTitle")}</h3>
                        <dl className="mt-2 grid grid-cols-1 gap-x-4 gap-y-1 text-xs sm:grid-cols-2">
                            {assumptionRows.map(([key, value]) => (
                                <div key={key} className="flex justify-between gap-2">
                                    <dt className="text-text-muted">{t(`scenario.assumptions.${key}`)}</dt>
                                    <dd className="font-semibold text-text">{value}</dd>
                                </div>
                            ))}
                        </dl>
                        <details className="mt-3 text-xs">
                            <summary className={`cursor-pointer font-semibold text-text-accent ${FOCUS}`}>
                                {t("scenario.allAssumptions")}
                            </summary>
                            <dl className="mt-2 grid grid-cols-1 gap-x-4 gap-y-1 sm:grid-cols-2">
                                {allAssumptionRows.map(([key, value]) => (
                                    <div key={key} className="flex justify-between gap-2">
                                        <dt className="text-text-muted">{t(`scenario.assumptions.${key}`)}</dt>
                                        <dd className="text-right font-semibold text-text">{value}</dd>
                                    </div>
                                ))}
                            </dl>
                        </details>
                        <p className="mt-3 text-xs text-text-muted">{t("scenario.formulaNote")}</p>
                        <p className="mt-1 text-xs text-text-muted">{t("scenario.basisNote")}</p>
                    </div>
                </section>

                <section aria-labelledby="scenario-results-title" className={CARD}>
                    <h2 id="scenario-results-title" className="flex items-center gap-2 text-lg font-semibold text-text">
                        <TrendingUp size={18} className="text-text-accent" aria-hidden="true" />
                        {t("scenario.results")}
                    </h2>

                    <dl className="flex flex-col gap-2" aria-live="polite">
                        <ResultRow label={t("scenario.result.investment")} value={formatFcfa(result.investment)} />
                        <ResultRow label={t("scenario.result.addedCapacity")} value={formatTonnes(result.addedCapacityTonnes)} />
                        <ResultRow label={t("scenario.result.usedCapacity")} value={formatTonnes(result.usedAddedCapacityTonnes)} />
                        <ResultRow label={t("scenario.result.lossReduction")} value={formatPoints(result.lossReductionPts, t)} />
                        <ResultRow label={t("scenario.result.annualCost")} value={formatFcfa(result.annualCost)} />
                        <ResultRow label={t("scenario.result.grossBenefit")} value={formatFcfa(result.annualGrossBenefit)} />
                        <ResultRow label={t("scenario.result.netBenefit")} value={formatFcfa(result.annualNetBenefit)} tone={netTone} />
                        <ResultRow label={t("scenario.result.breakEven")} value={formatBreakEven(result.breakEvenMonths, t)} />
                        <ResultRow
                            label={t("scenario.result.fiveYear")}
                            value={formatFcfa(result.fiveYearNetGain, { signed: true })}
                            tone={fiveYearTone}
                        />
                        <ResultRow label={t("scenario.result.outlook")} value={outlookLabel(result.outlook, t)} tone={outlookTone} />
                    </dl>
                    <p className="text-xs text-text-muted">{t("scenario.resultsNote", { value: A.baseLossPct })}</p>

                    <figure className="flex flex-col gap-2">
                        <figcaption className="text-sm font-semibold text-text">{t("scenario.chart.caption")}</figcaption>
                        <div className="h-64 w-full" aria-hidden="true">
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart data={chartData} margin={{ top: 8, right: 8, bottom: 24, left: 16 }}>
                                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-chart-grid)" />
                                    <XAxis
                                        dataKey="name"
                                        tick={{ fontSize: 12, fill: "var(--color-chart-3)" }}
                                        label={{ value: t("scenario.chart.xAxis"), position: X_LABEL_POSITION, offset: -16, fontSize: 12 }}
                                    />
                                    <YAxis
                                        tickFormatter={(value) => compactFcfa.format(value)}
                                        tick={{ fontSize: 12, fill: "var(--color-chart-3)" }}
                                        label={{ value: t("scenario.chart.yAxis"), angle: -90, position: Y_LABEL_POSITION, offset: -8, fontSize: 12 }}
                                    />
                                    <Tooltip formatter={(value) => formatFcfa(value)} />
                                    <Legend verticalAlign="top" height={28} />
                                    <Bar dataKey="cost" name={t("scenario.chart.cost")} fill="var(--color-chart-5)" radius={[6, 6, 0, 0]} isAnimationActive={false} />
                                    <Bar dataKey="benefit" name={t("scenario.chart.benefit")} fill="var(--color-chart-1)" radius={[6, 6, 0, 0]} isAnimationActive={false} />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                        <details className="text-xs" open>
                            <summary className={`cursor-pointer font-semibold text-text-accent ${FOCUS}`}>{t("scenario.chart.asTable")}</summary>
                            <div className="mt-2 overflow-x-auto">
                                <table className="w-full text-left">
                                    <caption className="sr-only">{t("scenario.chart.tableCaption")}</caption>
                                    <thead>
                                        <tr className="border-b border-border-divider text-text-muted">
                                            <th scope="col" className="py-1 pr-3 font-semibold">{t("scenario.chart.yearColumn")}</th>
                                            <th scope="col" className="py-1 pr-3 text-right font-semibold">{t("scenario.chart.cost")}</th>
                                            <th scope="col" className="py-1 pr-3 text-right font-semibold">{t("scenario.chart.benefit")}</th>
                                            <th scope="col" className="py-1 text-right font-semibold">{t("scenario.chart.net")}</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {result.projection.map((row) => (
                                            <tr key={row.year} className="border-b border-border-subtle">
                                                <th scope="row" className="py-1 pr-3 font-medium text-text">
                                                    {t("scenario.year", { year: row.year })}
                                                </th>
                                                <td className="py-1 pr-3 text-right tabular-nums">{formatFcfa(row.cumulativeCost)}</td>
                                                <td className="py-1 pr-3 text-right tabular-nums">{formatFcfa(row.cumulativeBenefit)}</td>
                                                <td className="py-1 text-right tabular-nums">{formatFcfa(row.cumulativeNet, { signed: true })}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </details>
                    </figure>

                    <Button variant="secondary" icon={FileText} fullWidth onClick={() => setSummary(buildScenarioSummary(result, t))}>
                        {t("scenario.summary.show")}
                    </Button>
                    <p className="-mt-2 text-center text-xs text-text-muted">{t("scenario.summary.note")}</p>
                    <div role="status" aria-live="polite" aria-label={t("scenario.summary.label")}>
                        {summary && (
                            <p className="rounded-control bg-primary-tint p-3 text-sm text-text">
                                <span className="font-semibold">{t("scenario.summary.title")}</span> — {summary}
                            </p>
                        )}
                    </div>
                </section>
            </div>
        </div>
    );
}
