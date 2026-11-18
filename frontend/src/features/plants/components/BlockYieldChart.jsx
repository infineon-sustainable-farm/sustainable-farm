import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useYieldComparison } from "../hooks/useYieldComparison";
import { formatBlock, formatKilograms, formatMonth, formatNumber } from "../utils/format";
import OverviewCard from "./OverviewCard";

/*
 * Expected in the second chart colour, actual in the first, on the chart grid colour as a track, so
 * that 0 kg reads as an empty bar. Each row writes what it is and its value, so the colours are never
 * the only cue.
 */
const EXPECTED_BAR = "bg-chart-2";
const ACTUAL_BAR = "bg-chart-1";

const LINK =
    "text-sm font-semibold text-text-accent underline underline-offset-2 hover:no-underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring";

/**
 * One bar on the card's common scale: as long as its yield against the largest
 * yield written on the card. A yield above 0 always shows a sliver. Its word,
 * its track and its value share one row; the values line up on the right.
 */
function YieldBar({ label, kg, largestKg, barClass }) {
    const width = largestKg > 0 ? `${(kg / largestKg) * 100}%` : "0%";
    return (
        <div className="flex items-center gap-2">
            <span className="w-16 shrink-0 text-xs text-text-muted">{label}</span>
            <div aria-hidden="true" className="h-2.5 min-w-0 flex-1 rounded-pill bg-chart-grid">
                <div className={`h-2.5 rounded-pill ${barClass} ${kg > 0 ? "min-w-1" : ""}`} style={{ width }} />
            </div>
            <span className="text-right text-xs font-semibold whitespace-nowrap text-text">{formatKilograms(kg)}</span>
        </div>
    );
}

function Legend() {
    const { t } = useTranslation("plants");
    return (
        <div aria-hidden="true" className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-text-muted">
            <span className="flex items-center gap-1.5">
                <span className={`h-2.5 w-4 rounded-pill ${EXPECTED_BAR}`} />
                {t("overview.yield.expected")}
            </span>
            <span className="flex items-center gap-1.5">
                <span className={`h-2.5 w-4 rounded-pill ${ACTUAL_BAR}`} />
                {t("overview.yield.actual")}
            </span>
        </div>
    );
}

/**
 * Expected vs Actual Yield by Block: the block lines of the yield comparison,
 * in the API's order, over the API's default period, the calendar year. Each
 * block has its expected bar and, once a harvest is recorded, its actual one;
 * both are written out in full. Every figure is the API's, and only the length
 * of the bars is laid out here. A block without harvest says whether none is
 * expected over the period (young trees) or none is recorded yet.
 */
export default function BlockYieldChart({ blockCode }) {
    const { t } = useTranslation("plants");
    const query = useYieldComparison({ blockCode });
    const comparison = query.data;
    const caption = comparison
        ? t("overview.yield.caption", {
              count: Number(comparison.months),
              months: formatNumber(comparison.months),
              from: formatMonth(comparison.from),
          })
        : null;
    // The common scale of the bars: the largest yield written on the card.
    const largestKg = comparison
        ? Math.max(0, ...comparison.blocks.flatMap((block) => [block.expectedKg, block.actualKg ?? 0]))
        : 0;

    return (
        <OverviewCard
            titleId="overview-yield-title"
            title={t("overview.yield.title")}
            caption={caption}
            query={query}
            loadingText={t("overview.yield.loading")}
            errorText={t("overview.yield.error")}
        >
            {comparison && comparison.blocks.length === 0 && (
                <p className="text-sm text-text">{t("overview.yield.empty")}</p>
            )}

            {comparison && comparison.blocks.length > 0 && (
                <>
                    <Legend />
                    <ul className="flex flex-col gap-3">
                        {comparison.blocks.map((block) => (
                            <li key={`${block.farmId}-${block.blockCode}`} className="flex flex-col gap-1">
                                <p className="text-sm font-semibold text-text">{formatBlock(block.blockCode)}</p>
                                <YieldBar
                                    label={t("overview.yield.expected")}
                                    kg={block.expectedKg}
                                    largestKg={largestKg}
                                    barClass={EXPECTED_BAR}
                                />
                                {block.actualKg === null ? (
                                    <p className="flex gap-2 text-xs text-text-muted">
                                        <span className="w-16 shrink-0">{t("overview.yield.actual")}</span>
                                        <span>
                                            {block.expectedKg === 0
                                                ? t("overview.yield.noHarvestExpected")
                                                : t("overview.yield.noHarvestRecorded")}
                                        </span>
                                    </p>
                                ) : (
                                    <YieldBar
                                        label={t("overview.yield.actual")}
                                        kg={block.actualKg}
                                        largestKg={largestKg}
                                        barClass={ACTUAL_BAR}
                                    />
                                )}
                            </li>
                        ))}
                    </ul>
                </>
            )}

            {comparison && comparison.notCompared.length > 0 && (
                <p className="text-xs text-text-muted">{t("overview.yield.notCompared")}</p>
            )}

            <Link to="/plants/harvest-maturity" className={`mt-auto self-start ${LINK}`}>
                {t("overview.yield.link")}
            </Link>
        </OverviewCard>
    );
}
