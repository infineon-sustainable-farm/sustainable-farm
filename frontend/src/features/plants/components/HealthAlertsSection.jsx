import { CircleCheck } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Card } from "../../../shared/ui";
import { useHealthAlerts } from "../hooks/useHealthAlerts";
import { formatCode } from "../utils/format";
import { HealthSectionError, HealthSectionLoading } from "./HealthSectionStates";
import PlantAlertGroups from "./PlantAlertGroups";
import PlantAlertItem from "./PlantAlertItem";

// What the date of each alert type is; labels from plants.json.
const TYPE_LABELS = {
    OPEN_HEALTH_ISSUE: { titleKey: "health.alerts.types.OPEN_HEALTH_ISSUE", dateKey: "health.alerts.seenOn" },
    PRE_HARVEST_INTERVAL: {
        titleKey: "health.alerts.types.PRE_HARVEST_INTERVAL",
        dateKey: "health.alerts.harvestAllowedFrom",
    },
};

/** One alert, worded for its type. */
function HealthAlert({ alert }) {
    const { t } = useTranslation("plants");
    const labels = TYPE_LABELS[alert.type];
    const title = labels ? t(labels.titleKey) : formatCode(alert.type);
    const dateLabel = t(labels ? labels.dateKey : "health.alerts.date");
    return <PlantAlertItem alert={alert} title={title} dateLabel={dateLabel} />;
}

/**
 * The health alerts of the day, as the API computes them: the findings not
 * resolved yet, and the treatments whose pre-harvest interval is still running,
 * critical while the harvest season of their block is open.
 *
 * They stay above the block filter and cover every block: an urgent alert
 * stays in view whatever block is selected.
 */
export default function HealthAlertsSection() {
    const { t } = useTranslation("plants");
    const query = useHealthAlerts();
    const { data, isPending, isError } = query;

    return (
        <Card
            aria-labelledby="health-alerts-title"
            title={<span id="health-alerts-title">{t("health.alerts.title")}</span>}
            subtitle={t("health.alerts.allBlocks")}
        >
            {isPending && <HealthSectionLoading>{t("health.alerts.loading")}</HealthSectionLoading>}

            {isError && (
                <HealthSectionError
                    title={t("health.alerts.loadError")}
                    detail={t("common.checkConnection")}
                    query={query}
                />
            )}

            {!isPending && !isError && data.alerts.length === 0 && (
                <p className="flex items-center gap-3 text-sm text-text">
                    <CircleCheck size={18} aria-hidden="true" className="shrink-0 text-success-fg" />
                    {t("health.alerts.empty")}
                </p>
            )}

            {!isPending && !isError && data.alerts.length > 0 && (
                <PlantAlertGroups
                    alerts={data.alerts}
                    idPrefix="health-alerts"
                    listClassName="flex flex-col gap-3"
                    renderAlert={(alert) => (
                        <HealthAlert key={`${alert.type}-${alert.findingId}-${alert.treatmentId}`} alert={alert} />
                    )}
                />
            )}
        </Card>
    );
}
