import { CircleCheck } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Card, ErrorState, Skeleton } from "../../../shared/ui";
import { useNurseryAlerts } from "../hooks/useNurseryAlerts";
import PlantAlertGroups from "./PlantAlertGroups";
import PlantAlertItem from "./PlantAlertItem";

/**
 * The batches to transplant today, as the API decides it: those with plants
 * left that are ready to transplant, or whose planned transplant date has
 * come. Whether a date has come is never worked out here; the API's message
 * says it.
 *
 * They stay above the destination block filter, as the health alerts do, and
 * cover every block.
 */
export default function NurseryAlertsSection() {
    const { t } = useTranslation("plants");
    const { data, isPending, isError, refetch, isFetching } = useNurseryAlerts();

    return (
        <Card
            aria-labelledby="transplant-alerts-title"
            title={<span id="transplant-alerts-title">{t("nursery.alerts.title")}</span>}
            subtitle={t("nursery.alerts.subtitle")}
        >
            {isPending && (
                <div role="status" className="flex flex-col gap-3">
                    <span className="sr-only">{t("nursery.alerts.loading")}</span>
                    <Skeleton radius="tile" className="h-16 w-full" />
                </div>
            )}

            {isError && (
                <ErrorState
                    className="py-6"
                    message={`${t("nursery.alerts.error")} ${t("common.checkConnection")}`}
                    onRetry={() => refetch()}
                    retrying={isFetching}
                    retryLabel={t("common.retry")}
                />
            )}

            {!isPending && !isError && data.alerts.length === 0 && (
                <p className="flex items-center gap-3 text-sm text-text">
                    <CircleCheck size={18} aria-hidden="true" className="shrink-0 text-success-fg" />
                    {t("nursery.alerts.empty")}
                </p>
            )}

            {!isPending && !isError && data.alerts.length > 0 && (
                <PlantAlertGroups
                    alerts={data.alerts}
                    idPrefix="transplant-alerts"
                    listClassName="flex flex-col gap-3"
                    renderAlert={(alert) => (
                        <PlantAlertItem
                            key={`${alert.type}-${alert.batchId}`}
                            alert={alert}
                            title={t("nursery.alerts.itemTitle")}
                            dateLabel={t("nursery.alerts.dateLabel")}
                        />
                    )}
                />
            )}
        </Card>
    );
}
