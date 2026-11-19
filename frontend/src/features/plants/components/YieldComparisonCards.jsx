import { useTranslation } from "react-i18next";
import { formatBlock, formatKilograms, formatNumber, formatPercent, formatText } from "../utils/format";
import { useCountOf } from "./useCountOf";
import Value from "./Value";

/**
 * The comparison cards, one per planted variety row, in the order the API sends
 * them: actual against expected yield, and the share achieved, all computed by
 * the API.
 *
 * The bar is as wide as that share, held at 100% for the display when the
 * harvests exceed the expectation; the share itself is written beside it.
 */
export default function YieldComparisonCards({ lines }) {
    const { t } = useTranslation("plants");
    const countOf = useCountOf();
    return (
        <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {lines.map((line) => {
                const expected = formatKilograms(line.expectedKg);
                const figures = line.actualKg === null
                    ? t("varieties.comparison.noHarvestExpected", { expected })
                    : t("varieties.comparison.actualOf", { actual: formatNumber(line.actualKg), expected });
                return (
                    <li
                        key={line.varietyId}
                        className="rounded-card border border-border-subtle bg-surface-raised px-4 py-3 shadow-raised"
                    >
                        <h3 className="text-sm font-semibold text-text">
                            {t("varieties.comparison.cardTitle", {
                                variety: formatText(line.varietyName),
                                block: formatBlock(line.blockCode),
                            })}
                        </h3>
                        <div className="mt-2 flex items-baseline justify-between gap-3 text-sm">
                            <span className="text-text-muted">{figures}</span>
                            <span className="font-bold text-text">
                                <Value>{formatPercent(line.achievedPct)}</Value>
                            </span>
                        </div>
                        {line.achievedPct !== null && (
                            <div aria-hidden="true" className="mt-2 h-2 rounded-pill bg-neutral-bg">
                                <div
                                    className="h-2 rounded-pill bg-primary"
                                    style={{ width: `${Math.min(line.achievedPct, 100)}%` }}
                                />
                            </div>
                        )}
                        <p className="mt-2 text-xs text-text-muted">
                            {t("varieties.comparison.countsLine", {
                                trees: countOf(line.treeCount, "trees"),
                                harvests: countOf(line.harvestCount, "harvests"),
                            })}
                        </p>
                    </li>
                );
            })}
        </ul>
    );
}
