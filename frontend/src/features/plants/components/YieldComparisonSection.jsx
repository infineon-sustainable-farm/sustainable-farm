import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Info } from "lucide-react";
import { Card, EmptyState, ErrorState, Skeleton } from "../../../shared/ui";
import { useYieldComparison } from "../hooks/useYieldComparison";
import { formatBlock, formatEnum, formatKilograms, formatMonth, formatPercent, formatText } from "../utils/format";
import MonthPeriodControls from "./MonthPeriodControls";
import YieldComparisonCards from "./YieldComparisonCards";
import YieldComparisonTables from "./YieldComparisonTables";
import { useCountOf } from "./useCountOf";

const DEFAULT_MONTHS = 12;

/**
 * The total of the period, in one sentence. The months are named by their
 * first one and their number, as the API gives them: no last month is worked
 * out here.
 */
function TotalSentence({ comparison, blockCode }) {
    const { t } = useTranslation("plants");
    const countOf = useCountOf();
    const { total } = comparison;
    const scope = blockCode ? formatBlock(blockCode) : t("blockFilter.all");
    const period = t("varieties.comparison.period", {
        months: countOf(comparison.months, "months"),
        month: formatMonth(comparison.from, "long"),
    });
    const expected = formatKilograms(total.expectedKg);
    let figures;
    if (total.actualKg === null) figures = t("varieties.comparison.totalNoHarvest", { expected });
    else if (total.achievedPct === null) {
        figures = t("varieties.comparison.totalHarvested", { actual: formatKilograms(total.actualKg), expected });
    } else {
        figures = t("varieties.comparison.totalHarvestedPct", {
            actual: formatKilograms(total.actualKg),
            expected,
            pct: formatPercent(total.achievedPct),
        });
    }
    return (
        <p className="text-sm text-text-muted">
            {t("varieties.comparison.totalPrefix", { scope, period })}
            <span className="font-semibold text-text">{figures}</span>
        </p>
    );
}

/**
 * Planted variety rows the API does not compare, with their harvests, so that
 * none of them is silently dropped.
 */
function NotComparedNote({ rows }) {
    const { t } = useTranslation("plants");
    const countOf = useCountOf();
    if (rows.length === 0) return null;
    const listed = rows
        .map((row) => {
            const where = row.treeCount === null
                ? formatBlock(row.blockCode)
                : t("varieties.comparison.whereWithTrees", {
                    block: formatBlock(row.blockCode),
                    trees: countOf(row.treeCount, "trees"),
                });
            const harvested = row.harvestCount === 0
                ? t("varieties.comparison.noHarvest")
                : t("varieties.comparison.harvestedIn", {
                    kg: formatKilograms(row.actualKg),
                    harvests: countOf(row.harvestCount, "harvests"),
                });
            return t("varieties.comparison.notComparedRow", {
                variety: formatText(row.varietyName),
                where,
                harvested,
                reason: formatEnum("comparisonReason", row.reason),
            });
        })
        .join(t("varieties.comparison.listSeparator"));
    const key = rows.some((row) => row.reason === "NO_REFERENCE") ? "notComparedWithHint" : "notCompared";

    return (
        <Card className="flex gap-3 text-sm text-text-muted">
            <Info size={18} aria-hidden="true" className="mt-0.5 shrink-0 text-info-fg" />
            <p>{t(`varieties.comparison.${key}`, { list: listed })}</p>
        </Card>
    );
}

/**
 * Expected against actual yield of the planted varieties of the selected
 * block, over whole months: the calendar year until another period is chosen.
 * Every yield, sum and share comes from the API; the browser only lays them
 * out.
 */
export default function YieldComparisonSection({ blockCode }) {
    const { t } = useTranslation("plants");
    // Month sent to the API; null lets the API start at January of its current year.
    const [from, setFrom] = useState(null);
    const [months, setMonths] = useState(DEFAULT_MONTHS);

    const { data: comparison, isPending, isError, refetch, isFetching } = useYieldComparison({
        blockCode,
        from,
        months,
    });

    const isLoaded = !isPending && !isError;

    return (
        <section aria-labelledby="yield-comparison-title" className="flex flex-col gap-4">
            <div>
                <h2 id="yield-comparison-title" className="text-lg font-semibold">
                    {t("varieties.comparison.title")}
                </h2>
                <p className="max-w-3xl text-sm text-text-muted">{t("varieties.comparison.intro")}</p>
            </div>

            <MonthPeriodControls
                idPrefix="yield-comparison"
                titleId="yield-comparison-title"
                answeredFrom={comparison?.from}
                onFromChange={setFrom}
                months={months}
                onMonthsChange={setMonths}
            />

            {isPending && (
                <Card aria-busy="true">
                    <p role="status" className="sr-only">
                        {t("varieties.comparison.loading")}
                    </p>
                    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                        {[0, 1, 2].map((index) => (
                            <Skeleton key={index} radius="tile" className="h-24" />
                        ))}
                    </div>
                </Card>
            )}

            {isError && (
                <Card>
                    <ErrorState
                        message={
                            <>
                                <span className="block font-semibold">{t("varieties.comparison.loadError")}</span>
                                <span className="block">{t("varieties.requestFailed")}</span>
                            </>
                        }
                        onRetry={() => refetch()}
                        retrying={isFetching}
                        retryLabel={t("common.retry")}
                    />
                </Card>
            )}

            {isLoaded && comparison.varieties.length === 0 && comparison.notCompared.length === 0 && (
                <Card>
                    <EmptyState
                        title={t("varieties.comparison.empty")}
                        message={t("varieties.comparison.emptyHint")}
                    />
                </Card>
            )}

            {isLoaded && comparison.varieties.length === 0 && comparison.notCompared.length > 0 && (
                <Card className="text-sm text-text">{t("varieties.comparison.noReference")}</Card>
            )}

            {isLoaded && comparison.varieties.length > 0 && (
                <>
                    <TotalSentence comparison={comparison} blockCode={blockCode} />
                    <YieldComparisonCards lines={comparison.varieties} />
                    <YieldComparisonTables blocks={comparison.blocks} lines={comparison.varieties} />
                </>
            )}

            {isLoaded && <NotComparedNote rows={comparison.notCompared} />}

            {isLoaded && comparison.varieties.length > 0 && (
                <p className="px-1 text-xs text-text-muted">{t("varieties.comparison.footnote")}</p>
            )}
        </section>
    );
}
