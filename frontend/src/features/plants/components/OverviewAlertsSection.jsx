import { CircleCheck } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Card, ErrorState, Skeleton } from "../../../shared/ui";
import { useHealthFindings } from "../hooks/useHealthFindings";
import { useNurseryBatches } from "../hooks/useNurseryBatches";
import { usePlantAlerts } from "../hooks/usePlantAlerts";
import { formatBlock } from "../utils/format";
import OverviewAlertRow from "./OverviewAlertRow";
import { ALERT_COLUMNS } from "./overviewAlertColumns";

/** Values by id, to name the batch or the tree an alert points at; empty until the list is there. */
function byId(items, pick) {
    return new Map((items ?? []).map((item) => [item.id, pick(item)]));
}

/**
 * Every plant alert of the day for the selected block, as the API computes and
 * orders them, one row each: harvest seasons, pre-harvest intervals, low
 * stocks, open health problems and batches to transplant. All of them are
 * listed; the Active Alerts figure above counts the same ones.
 *
 * The batch code and the tree come from the nursery batches (already read by
 * the survival chart) and the health findings; while either is missing, the
 * row names the nursery or the block.
 */
export default function OverviewAlertsSection({ blockCode }) {
    const { t } = useTranslation("plants");
    const { data, isPending, isError, refetch, isFetching } = usePlantAlerts({ blockCode });
    const batchCodes = byId(useNurseryBatches().data, (batch) => batch.batchCode);
    const treeLabels = byId(useHealthFindings().data, (finding) => finding.treeLabel);

    // What the list covers; the day is the one under the page title.
    const scope = blockCode
        ? t("overview.alerts.scopeBlock", { block: formatBlock(blockCode) })
        : t("overview.alerts.scopeAll");

    return (
        <Card
            aria-labelledby="overview-alerts-title"
            title={<span id="overview-alerts-title">{t("overview.alerts.title")}</span>}
            subtitle={scope}
        >
            {isPending && (
                // Gray blocks in the place of the first rows; the sentence is for screen readers.
                <div role="status" className="flex flex-col gap-3 py-2">
                    <span className="sr-only">{t("overview.alerts.loading")}</span>
                    <Skeleton className="h-4 w-full" />
                    <Skeleton className="h-4 w-5/6" />
                    <Skeleton className="h-4 w-2/3" />
                </div>
            )}

            {isError && (
                <ErrorState
                    className="py-6"
                    message={`${t("overview.alerts.error")} ${t("common.checkConnection")}`}
                    onRetry={() => refetch()}
                    retrying={isFetching}
                    retryLabel={t("common.retry")}
                />
            )}

            {!isPending && !isError && data.alerts.length === 0 && (
                <p className="flex items-center gap-2 text-sm text-text">
                    <CircleCheck size={16} aria-hidden="true" className="shrink-0 text-success-fg" />
                    {t("overview.alerts.empty")}
                </p>
            )}

            {!isPending && !isError && data.alerts.length > 0 && (
                <div>
                    {/* Column names for the eye from lg up; each row reads in full on its own. */}
                    <div
                        aria-hidden="true"
                        className="hidden gap-x-4 border-b border-border-divider pb-1 text-sm text-text-muted lg:grid lg:grid-cols-12"
                    >
                        <span className={ALERT_COLUMNS.priority}>{t("overview.alerts.priority")}</span>
                        <span className={ALERT_COLUMNS.place}>{t("overview.alerts.where")}</span>
                        <span className={ALERT_COLUMNS.issue}>{t("overview.alerts.issue")}</span>
                        <span className={ALERT_COLUMNS.when}>{t("overview.alerts.when")}</span>
                        <span className={ALERT_COLUMNS.action} />
                    </div>
                    <ul className="flex flex-col">
                        {data.alerts.map((alert) => (
                            <OverviewAlertRow
                                key={`${alert.type}-${alert.varietyId}-${alert.treatmentId}-${alert.findingId}-${alert.fertilizerId}-${alert.batchId}`}
                                alert={alert}
                                batchCode={alert.batchId == null ? null : batchCodes.get(alert.batchId)}
                                treeLabel={alert.findingId == null ? null : treeLabels.get(alert.findingId)}
                            />
                        ))}
                    </ul>
                </div>
            )}
        </Card>
    );
}
