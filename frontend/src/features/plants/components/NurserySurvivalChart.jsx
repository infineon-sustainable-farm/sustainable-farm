import { useTranslation } from "react-i18next";
import { useNurseryBatches } from "../hooks/useNurseryBatches";
import { formatBlock, formatNumber, formatPercent } from "../utils/format";
import OverviewCard from "./OverviewCard";

/**
 * Survival rate, one row per nursery batch in the API's order (by start date):
 * its code and destination block, a bar as long as the survival rate the API
 * computes, and the counts behind it, "138 of 150 plants survived (92%)". A
 * transplanted plant counts as a survivor. "Lot" reads "Batch", as on the
 * Nursery screen, since a lot is now the number of a harvest. The batches
 * cover every block. The bar repeats the sentence under it, so it is hidden
 * from screen readers.
 */
export default function NurserySurvivalChart() {
    const { t } = useTranslation("plants");
    const query = useNurseryBatches();
    const batches = query.data;

    return (
        <OverviewCard
            titleId="overview-nursery-title"
            title={t("overview.nursery.title")}
            caption={t("overview.nursery.caption")}
            query={query}
            loadingText={t("overview.nursery.loading")}
            errorText={t("overview.nursery.error")}
        >
            {batches && batches.length === 0 && <p className="text-sm text-text">{t("overview.nursery.empty")}</p>}

            {batches && batches.length > 0 && (
                <ul className="flex flex-col gap-3">
                    {batches.map((batch) => {
                        const destination = batch.plannedBlockCode
                            ? t("overview.nursery.destination", { block: formatBlock(batch.plannedBlockCode) })
                            : null;
                        const survived = t("overview.nursery.survived", {
                            count: Number(batch.initialCount),
                            percent: formatPercent(batch.survivalRatePct),
                        });
                        const lost =
                            batch.lossCount > 0 ? t("overview.nursery.lost", { lost: formatNumber(batch.lossCount) }) : null;
                        const rate = Math.min(Math.max(batch.survivalRatePct ?? 0, 0), 100);
                        return (
                            <li key={batch.id} className="flex flex-col gap-1">
                                <p className="text-sm break-words text-text">
                                    <span className="font-semibold">{t("overview.nursery.batch", { code: batch.batchCode })}</span>
                                    {destination && <span className="text-xs text-text-muted">{destination}</span>}
                                </p>
                                <div aria-hidden="true" className="h-2.5 rounded-pill bg-chart-grid">
                                    <div className="h-2.5 rounded-pill bg-chart-1" style={{ width: `${rate}%` }} />
                                </div>
                                <p className="text-xs text-text-muted">
                                    <span className="font-semibold text-text">
                                        {t("overview.nursery.survivedOf", {
                                            survived: formatNumber(batch.survivedCount),
                                            initial: formatNumber(batch.initialCount),
                                        })}
                                    </span>
                                    {survived}
                                    {lost}
                                </p>
                                <p className="text-xs text-text-muted">
                                    {t("overview.nursery.transplanted", {
                                        transplanted: formatNumber(batch.transplantedCount),
                                        current: formatNumber(batch.currentCount),
                                    })}
                                </p>
                            </li>
                        );
                    })}
                </ul>
            )}
        </OverviewCard>
    );
}
