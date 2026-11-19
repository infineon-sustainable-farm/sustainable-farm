import { useTranslation } from "react-i18next";
import { formatBlock, formatNumber, formatPercent } from "../utils/format";

/** Width classes of the survival bar, one per 5 % step, so no inline style is needed. */
const BAR_WIDTHS = [
    "w-0", "w-1/20", "w-2/20", "w-3/20", "w-4/20", "w-5/20", "w-6/20", "w-7/20", "w-8/20", "w-9/20", "w-10/20",
    "w-11/20", "w-12/20", "w-13/20", "w-14/20", "w-15/20", "w-16/20", "w-17/20", "w-18/20", "w-19/20", "w-full",
];

function barWidth(rate) {
    const value = Number(rate);
    if (!Number.isFinite(value)) return BAR_WIDTHS[0];
    const step = Math.round(Math.min(100, Math.max(0, value)) / 5);
    return BAR_WIDTHS[step];
}

/**
 * Survival rate of each batch, as in the mock-up's cards: how many of the
 * starting plants survived the nursery, the rate, and a bar as wide as the
 * rate. All three come from the API, which counts a transplanted plant as a
 * survivor.
 *
 * One colour for every bar: the mock-up turns a rate amber below about 90%,
 * but no source sets such a threshold and the API sends none. The bar repeats
 * the rate written next to it, so it is hidden from screen readers.
 */
export default function NurserySurvivalCards({ batches }) {
    const { t } = useTranslation("plants");

    /** "P1 · Keitt (Block E)", or "P1 · Keitt" for a batch with no planned block. */
    function cardTitle(batch) {
        const names = { code: batch.batchCode, variety: batch.varietyName };
        return batch.plannedBlockCode
            ? t("nursery.survival.cardTitleWithBlock", { ...names, block: formatBlock(batch.plannedBlockCode) })
            : t("nursery.survival.cardTitle", names);
    }

    return (
        <div className="flex flex-col gap-3">
            <h3 id="survival-rate-title" className="text-sm font-semibold text-text">
                {t("nursery.survival.title")}
            </h3>
            <ul aria-labelledby="survival-rate-title" className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {batches.map((batch) => (
                    <li key={batch.id} className="rounded-tile border border-border-subtle bg-surface-raised p-4">
                        <p className="text-sm font-semibold break-words text-text">{cardTitle(batch)}</p>
                        <div className="mt-2 flex items-baseline justify-between gap-3">
                            <span className="text-sm text-text-muted">
                                {t("nursery.survival.survived", {
                                    survived: formatNumber(batch.survivedCount),
                                    initial: formatNumber(batch.initialCount),
                                })}
                            </span>
                            <span className="text-lg font-bold text-text">{formatPercent(batch.survivalRatePct)}</span>
                        </div>
                        <div aria-hidden="true" className="mt-2 h-2 overflow-hidden rounded-pill bg-neutral-bg">
                            <div className={`h-full rounded-pill bg-primary ${barWidth(batch.survivalRatePct)}`} />
                        </div>
                        <p className="mt-2 text-xs text-text-muted">
                            {t("nursery.survival.lostTransplanted", {
                                lost: formatNumber(batch.lossCount),
                                transplanted: formatNumber(batch.transplantedCount),
                            })}
                        </p>
                    </li>
                ))}
            </ul>
        </div>
    );
}
