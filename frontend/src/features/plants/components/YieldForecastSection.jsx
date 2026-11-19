import { useState } from "react";
import { Info } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Card, EmptyState } from "../../../shared/ui";
import { useYieldForecast } from "../hooks/useYieldForecast";
import { formatBlock, formatKilograms, formatMonth, formatNumber, formatText } from "../utils/format";
import { SectionError, SectionLoading } from "./HarvestSectionStates";
import MonthPeriodControls from "./MonthPeriodControls";
import YieldForecastChart from "./YieldForecastChart";
import YieldForecastTable from "./YieldForecastTable";

const DEFAULT_MONTHS = 12;

/**
 * The API gives no total. The monthly totals are added here and the sum is
 * rounded to 0.1 kg, like each of them, so no floating-point residue shows.
 */
function periodTotal(monthlyTotals) {
    const sum = monthlyTotals.reduce((total, month) => total + month.expectedKg, 0);
    return Math.round(sum * 10) / 10;
}

/**
 * The same values as the chart, month by month, as a table. Closed by default:
 * the chart already shows every value, and the table is there for whoever
 * prefers to read them in rows.
 */
function MonthlyTotalsTable({ monthlyTotals, totalKg }) {
    const { t } = useTranslation("plants");
    return (
        <details className="rounded-card border border-border-subtle bg-surface-raised shadow-raised">
            <summary className="cursor-pointer rounded-card px-5 py-3 text-sm font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring">
                {t("harvest.forecast.showTable")}
            </summary>
            <div className="overflow-x-auto px-5 pb-3">
                <table className="w-full border-collapse text-left text-sm whitespace-nowrap">
                    <thead>
                        <tr className="border-b border-border-divider text-text-muted">
                            <th scope="col" className="py-2 pr-4 font-normal">{t("harvest.forecast.month")}</th>
                            <th scope="col" className="py-2 pr-4 font-normal">{t("harvest.forecast.expectedYield")}</th>
                        </tr>
                    </thead>
                    <tbody>
                        {monthlyTotals.map((month) => (
                            <tr key={month.month} className="border-b border-border-subtle">
                                <td className="py-2 pr-4">{formatMonth(month.month)}</td>
                                <td className="py-2 pr-4">{formatKilograms(month.expectedKg)}</td>
                            </tr>
                        ))}
                    </tbody>
                    <tfoot>
                        <tr>
                            <td className="py-2 pr-4 font-semibold">{t("harvest.forecast.total")}</td>
                            <td className="py-2 pr-4 font-semibold">{formatKilograms(totalKg)}</td>
                        </tr>
                    </tfoot>
                </table>
            </div>
        </details>
    );
}

/**
 * Planted varieties the forecast leaves out because the agronomic reference
 * does not know them. They are named rather than silently dropped.
 */
function VarietiesWithoutReferenceNote({ varieties }) {
    const { t } = useTranslation("plants");
    if (varieties.length === 0) return null;
    const listed = varieties
        .map((variety) =>
            t("harvest.forecast.listedVariety", {
                variety: formatText(variety.varietyName),
                block: formatBlock(variety.blockCode),
                trees: t("harvest.forecast.trees", { count: Number(variety.treeCount), value: formatNumber(variety.treeCount) }),
            }))
        .join(t("format.listSeparator"));

    return (
        <Card>
            <div className="flex gap-3 text-sm text-text-muted">
                <Info size={18} aria-hidden="true" className="mt-0.5 shrink-0 text-info-fg" />
                <p>{t("harvest.forecast.notInForecast", { count: varieties.length, list: listed })}</p>
            </div>
        </Card>
    );
}

/**
 * Expected yield by month for the selected block: a column chart with the
 * period total, the same values as a table, and the forecast line by line
 * with its sources.
 */
export default function YieldForecastSection({ blockCode }) {
    const { t } = useTranslation("plants");
    // Month sent to the API; null lets the API start at its current month.
    const [from, setFrom] = useState(null);
    const [months, setMonths] = useState(DEFAULT_MONTHS);

    const query = useYieldForecast({ blockCode, from, months });
    const { data: forecast, isPending, isError } = query;

    const isEmpty = !isPending && !isError && forecast.monthlyTotals.every((month) => month.expectedKg === 0);
    const totalKg = !isPending && !isError ? periodTotal(forecast.monthlyTotals) : null;

    return (
        <section aria-labelledby="yield-forecast-title" className="flex flex-col gap-4">
            <h2 id="yield-forecast-title" className="text-lg font-semibold">
                {t("harvest.forecast.title")}
            </h2>

            <MonthPeriodControls
                idPrefix="yield-forecast"
                titleId="yield-forecast-title"
                answeredFrom={forecast?.from}
                onFromChange={setFrom}
                months={months}
                onMonthsChange={setMonths}
            />

            {isPending && <SectionLoading label={t("harvest.forecast.loading")} />}

            {isError && <SectionError title={t("harvest.forecast.error")} query={query} />}

            {isEmpty && (
                <>
                    <Card>
                        <EmptyState title={t("harvest.forecast.emptyTitle")} message={t("harvest.forecast.emptyHint")} />
                    </Card>
                    <VarietiesWithoutReferenceNote varieties={forecast.varietiesWithoutReference} />
                </>
            )}

            {!isPending && !isError && !isEmpty && (
                <>
                    <Card>
                        <p className="mb-4 text-sm text-text-muted">
                            {t("harvest.forecast.totalOverPeriod")}{" "}
                            <span className="font-semibold text-text">{formatKilograms(totalKg)}</span>
                        </p>
                        <YieldForecastChart monthlyTotals={forecast.monthlyTotals} totalKg={totalKg} />
                    </Card>
                    <MonthlyTotalsTable monthlyTotals={forecast.monthlyTotals} totalKg={totalKg} />
                    <VarietiesWithoutReferenceNote varieties={forecast.varietiesWithoutReference} />
                    <p className="px-1 text-xs text-text-muted">{t("harvest.forecast.basedOn")}</p>
                    <YieldForecastTable entries={forecast.entries} />
                </>
            )}
        </section>
    );
}
