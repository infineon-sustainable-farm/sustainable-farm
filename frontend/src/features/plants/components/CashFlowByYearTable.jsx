import { useTranslation } from "react-i18next";
import { Badge, Select } from "../../../shared/ui";
import { formatEnum, formatNumber, formatShare } from "../utils/format";
import { currencyUnit, formatAmountFigure, MAX_HORIZON_YEARS } from "../utils/scenarioRequests";
import { ErrorBox, LoadingBox, RetryButton, SectionHeading } from "./ScenarioStatus";
import { HEAD_ROW_CLASS, NOTE_CLASS, ROW_CLASS } from "./scenarioStyles";

/*
 * Narrower cells than the other tables, headers that may wrap, and the units
 * in the headers rather than in every cell: the ten columns then fit a desktop
 * screen, Cumulative included.
 */
const TH_CLASS = "py-2 pr-3 text-left align-bottom text-sm font-normal whitespace-normal text-text-muted";
const TD_CLASS = "py-2 pr-3 text-text";

/** The column headers, in the order of the cells; the amount columns name their unit. */
const COLUMN_KEYS = [
    "year",
    "age",
    "growthPhase",
    "yieldShare",
    "harvest",
    "sellable",
    "revenue",
    "operatingCost",
    "netBenefit",
    "cumulative",
];

const HORIZONS = Array.from({ length: MAX_HORIZON_YEARS }, (_, index) => index + 1);

/** Growth phase code of a year, as an enum key ("full production" or "FULL_PRODUCTION"). */
function phaseCode(phase) {
    return phase === null || phase === undefined ? phase : String(phase).trim().toUpperCase().replaceAll(" ", "_");
}

/** Translated growth phase of a year. */
function phaseLabel(phase) {
    return formatEnum("growthPhase", phaseCode(phase));
}

/** What the payback of the plan is, as the API found it within the years shown. */
function paybackText(t, baseline, horizonYears) {
    if (baseline.paybackYear !== null) return t("scenarios.cashFlow.paybackYear", { year: baseline.paybackYear });
    if (baseline.netBenefit === null) return t("scenarios.cashFlow.enterPrice");
    return t("scenarios.cashFlow.paybackNotWithin", { years: horizonYears });
}

function YearsTable({ analysis }) {
    const { t } = useTranslation("plants");
    const { baseline, currency } = analysis;
    const unit = currencyUnit(currency);
    return (
        <div className="overflow-x-auto rounded-tile border border-border-subtle bg-surface-raised px-4 py-2">
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr className={HEAD_ROW_CLASS}>
                        {COLUMN_KEYS.map((key) => (
                            <th key={key} scope="col" className={TH_CLASS}>
                                {t(`scenarios.cashFlow.columns.${key}`, { unit })}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {baseline.years.map((year) => {
                        const isPayback = year.year === baseline.paybackYear;
                        const phase = phaseLabel(year.growthPhase);
                        return (
                            <tr key={year.year} className={`${ROW_CLASS} ${isPayback ? "bg-success-bg" : ""}`}>
                                <th scope="row" className="py-2 pr-3 text-left font-medium text-text">
                                    {year.year}
                                    {isPayback && (
                                        <Badge tone="success" className="ml-2">
                                            {t("scenarios.cashFlow.payback")}
                                        </Badge>
                                    )}
                                </th>
                                <td className={TD_CLASS}>{t("scenarios.cashFlow.age", { count: year.ageYears })}</td>
                                <td className={TD_CLASS}>{phase}</td>
                                <td className={TD_CLASS}>{formatShare(year.yieldShare)}</td>
                                <td className={TD_CLASS}>{formatNumber(year.harvestKg)}</td>
                                <td className={TD_CLASS}>{formatNumber(year.sellableKg)}</td>
                                <td className={TD_CLASS}>{formatAmountFigure(year.revenue, currency)}</td>
                                <td className={TD_CLASS}>{formatAmountFigure(year.operatingCost, currency)}</td>
                                <td className={TD_CLASS}>{formatAmountFigure(year.netBenefit, currency)}</td>
                                <td className={`${TD_CLASS} font-semibold`}>
                                    {formatAmountFigure(year.cumulativeNetBenefit, currency)}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}

/**
 * The baseline year by year, as the API computes it: the growth phase of the
 * trees, the share of their full yield, the harvest, and the net benefits
 * added up after the investment, until the year they cover it. The number of
 * years is chosen here and sent to the API; the same analysis gives the
 * payback of the baseline cards.
 */
export default function CashFlowByYearTable({ query, horizonYears, onHorizonChange }) {
    const { t } = useTranslation("plants");
    const { data, isPending, isError, refetch, isFetching } = query;

    return (
        <section aria-labelledby="cash-flow-title" className="flex flex-col gap-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                    <SectionHeading id="cash-flow-title" title={t("scenarios.cashFlow.title")} />
                    {data && !isError && (
                        <p className="mt-1 text-sm font-semibold text-text">
                            {paybackText(t, data.analysis.baseline, data.analysis.horizonYears)}
                        </p>
                    )}
                </div>
                <div className="flex items-center gap-2">
                    <label htmlFor="cash-flow-years" className="text-sm font-medium">
                        {t("scenarios.cashFlow.yearsShown")}
                    </label>
                    <div>
                        <Select
                            id="cash-flow-years"
                            value={horizonYears}
                            onChange={(event) => onHorizonChange(Number(event.target.value))}
                        >
                            {HORIZONS.map((years) => (
                                <option key={years} value={years}>
                                    {years}
                                </option>
                            ))}
                        </Select>
                    </div>
                </div>
            </div>

            {isPending && <LoadingBox label={t("scenarios.cashFlow.computing")} />}

            {isError && (
                <ErrorBox
                    title={t("scenarios.cashFlow.error")}
                    message={t("scenarios.requestFailed")}
                    action={<RetryButton onClick={() => refetch()} isFetching={isFetching} />}
                />
            )}

            {data && !isError && <YearsTable analysis={data.analysis} />}

            <p className={NOTE_CLASS}>{t("scenarios.cashFlow.note")}</p>
        </section>
    );
}
