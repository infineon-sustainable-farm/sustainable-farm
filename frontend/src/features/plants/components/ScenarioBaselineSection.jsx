import { Pencil, Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Select } from "../../../shared/ui";
import { formatNumber, formatText } from "../utils/format";
import {
    formatAmount,
    formatMass,
    formatOtherAmount,
    formatReturn,
    formatUnitPrice,
    formatYears,
} from "../utils/scenarioRequests";
import { ErrorBox, LoadingBox, RetryButton } from "./ScenarioStatus";
import { FIGURE_LABEL_CLASS, NOTE_CLASS } from "./scenarioStyles";

function FigureCard({ label, value, notes = [], children }) {
    return (
        <li className="flex min-w-0 flex-col gap-1 rounded-tile border border-border-subtle bg-surface-raised p-4">
            {label && <p className={FIGURE_LABEL_CLASS}>{label}</p>}
            {value !== undefined && <p className="text-2xl font-bold break-words text-text">{value}</p>}
            {notes.filter(Boolean).map((note) => (
                <p key={note} className={NOTE_CLASS}>
                    {note}
                </p>
            ))}
            {children}
        </li>
    );
}

/** The two paybacks of the API, under the names that say what each assumes. */
function PaybackCard({ baseline, horizonYears }) {
    const { t } = useTranslation("plants");
    const hasNetBenefit = baseline.netBenefit !== null;
    let simple = formatYears(baseline.simplePaybackYears);
    let withPhases = baseline.paybackYear === null ? null : t("scenarios.baseline.year", { year: baseline.paybackYear });
    if (hasNetBenefit && baseline.simplePaybackYears === null) simple = t("scenarios.notRecovered");
    if (withPhases === null) {
        withPhases = hasNetBenefit ? t("scenarios.baseline.notWithin", { years: horizonYears }) : formatText(null);
    }
    return (
        <FigureCard label={t("scenarios.baseline.payback")}>
            <div className="mt-1 flex flex-col gap-2">
                <div>
                    <p className={NOTE_CLASS}>{t("scenarios.baseline.simplePayback")}</p>
                    <p className="text-lg font-bold text-text">{simple}</p>
                </div>
                <div>
                    <p className={NOTE_CLASS}>{t("scenarios.baseline.phasesPayback")}</p>
                    <p className="text-lg font-bold text-text">{withPhases}</p>
                </div>
            </div>
        </FigureCard>
    );
}

function BaselineFigures({ scenario, analysis }) {
    const { t } = useTranslation("plants");
    const { baseline, currency, horizonYears } = analysis;
    const yieldNote = baseline.lossKg === null
        ? t("scenarios.baseline.beforeLosses")
        : t("scenarios.baseline.soldAfterLosses", { sold: formatMass(baseline.sellableKg), lost: formatMass(baseline.lossKg) });
    const noPrice = baseline.revenue === null ? t("scenarios.enterPriceRevenue") : null;
    const otherInvestment = formatOtherAmount(baseline.investment, currency);
    const investment = formatAmount(baseline.investment, currency);
    const investmentText = otherInvestment
        ? t("scenarios.baseline.investmentWithOther", { amount: investment, other: otherInvestment })
        : t("scenarios.baseline.investment", { amount: investment });
    const priceText = scenario.pricePerKg === null
        ? t("scenarios.baseline.noPrice")
        : t("scenarios.baseline.price", { price: formatUnitPrice(scenario.pricePerKg, currency) });

    return (
        <div className="flex flex-col gap-3">
            <ul className="grid grid-cols-2 gap-3 lg:grid-cols-3">
                <FigureCard
                    label={t("scenarios.baseline.totalTrees")}
                    value={formatNumber(baseline.treeCount)}
                    notes={[scenario.varietyName ? formatText(scenario.varietyName) : null]}
                />
                <FigureCard label={t("scenarios.baseline.annualYield")} value={formatMass(baseline.harvestKg)} notes={[yieldNote]} />
                <FigureCard
                    label={t("scenarios.baseline.annualRevenue")}
                    value={formatAmount(baseline.revenue, currency)}
                    notes={[formatOtherAmount(baseline.revenue, currency), noPrice]}
                />
                <FigureCard
                    label={t("scenarios.baseline.annualNetBenefit")}
                    value={formatAmount(baseline.netBenefit, currency)}
                    notes={[
                        formatOtherAmount(baseline.netBenefit, currency),
                        t("scenarios.baseline.operatingCost", { amount: formatAmount(baseline.operatingCost, currency) }),
                    ]}
                />
                <FigureCard
                    label={t("scenarios.baseline.roi")}
                    value={formatReturn(baseline.roiPct)}
                    notes={[t("scenarios.baseline.roiNote")]}
                />
                <PaybackCard baseline={baseline} horizonYears={horizonYears} />
            </ul>
            <p className={`px-1 ${NOTE_CLASS}`}>{t("scenarios.baseline.investmentLine", { investment: investmentText, price: priceText })}</p>
            <p className={`px-1 ${NOTE_CLASS}`}>{t("scenarios.baseline.paybackExplained")}</p>
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
    const { t } = useTranslation("plants");
    const { data, isPending, isError, refetch, isFetching } = query;

    return (
        <section aria-labelledby="scenario-baseline-title" className="flex flex-col gap-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
                <h2 id="scenario-baseline-title" className="text-lg font-semibold break-words text-text">
                    {t("scenarios.baseline.title", { name: scenario.name })}
                </h2>
                <div className="flex flex-wrap items-center gap-2">
                    {scenarios.length > 1 && (
                        <div className="flex items-center gap-2">
                            <label htmlFor="scenario-select" className="text-sm font-medium">
                                {t("scenarios.baseline.scenarioLabel")}
                            </label>
                            <div>
                                <Select
                                    id="scenario-select"
                                    value={scenario.id}
                                    onChange={(event) => onSelect(Number(event.target.value))}
                                >
                                    {scenarios.map((candidate) => (
                                        <option key={candidate.id} value={candidate.id}>
                                            {candidate.name}
                                        </option>
                                    ))}
                                </Select>
                            </div>
                        </div>
                    )}
                    <Button
                        ref={editButtonRef}
                        variant="secondary"
                        size="sm"
                        icon={Pencil}
                        onClick={onEdit}
                        aria-label={t("scenarios.editName", { name: scenario.name })}
                    >
                        {t("scenarios.baseline.edit")}
                    </Button>
                    <Button ref={addButtonRef} variant="secondary" size="sm" icon={Plus} onClick={onAdd}>
                        {t("scenarios.baseline.add")}
                    </Button>
                </div>
            </div>

            {isPending && <LoadingBox label={t("scenarios.baseline.computing")} />}

            {isError && (
                <ErrorBox
                    title={t("scenarios.baseline.error")}
                    message={t("scenarios.requestFailed")}
                    action={<RetryButton onClick={() => refetch()} isFetching={isFetching} />}
                />
            )}

            {data && !isError && <BaselineFigures scenario={scenario} analysis={data.analysis} />}
        </section>
    );
}
