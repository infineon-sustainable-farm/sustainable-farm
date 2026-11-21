import { CircleCheck } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Card } from "../../../shared/ui";
import { useHarvestAlerts } from "../hooks/useHarvestAlerts";
import { formatCode } from "../utils/format";
import { SectionError, SectionLoading } from "./HarvestSectionStates";
import PlantAlertGroups from "./PlantAlertGroups";
import PlantAlertItem from "./PlantAlertItem";

// The alert types of this section, each worded for what its date is.
const KNOWN_TYPES = ["HARVEST_APPROACHING", "PRE_HARVEST_INTERVAL"];

/** One deadline, worded for its type. */
function Deadline({ alert }) {
    const { t } = useTranslation("plants");
    const known = KNOWN_TYPES.includes(alert.type);
    const title = known ? t(`harvest.deadlines.types.${alert.type}.title`) : formatCode(alert.type);
    const dateLabel = known ? t(`harvest.deadlines.types.${alert.type}.date`) : t("harvest.deadlines.otherDate");
    return <PlantAlertItem alert={alert} title={title} dateLabel={dateLabel} />;
}

/**
 * The harvest deadlines of the selected block, as the API computes them: the
 * harvest seasons in progress or opening within its look-ahead, and the
 * pre-harvest intervals still running, critical while the season of their
 * block is open. They come in the order of the API, the most urgent first;
 * nothing is sorted or dated here. They follow the block filter like the rest
 * of the page.
 */
export default function UpcomingDeadlinesSection({ blockCode }) {
    const { t } = useTranslation("plants");
    const query = useHarvestAlerts({ blockCode });
    const { data, isPending, isError } = query;

    return (
        <section aria-labelledby="upcoming-deadlines-title" className="flex flex-col gap-3">
            <div>
                <h2 id="upcoming-deadlines-title" className="text-lg font-semibold">
                    {t("harvest.deadlines.title")}
                </h2>
                {data && (
                    <p className="text-sm text-text-muted">
                        {t("harvest.deadlines.subtitle", { days: data.withinDays })}
                    </p>
                )}
            </div>

            {isPending && <SectionLoading label={t("harvest.deadlines.loading")} />}

            {isError && <SectionError title={t("harvest.deadlines.error")} query={query} />}

            {!isPending && !isError && data.alerts.length === 0 && (
                <Card>
                    <p className="flex items-center gap-3 text-sm text-text">
                        <CircleCheck size={18} aria-hidden="true" className="shrink-0 text-success-fg" />
                        {t("harvest.deadlines.empty", { days: data.withinDays })}
                    </p>
                </Card>
            )}

            {!isPending && !isError && data.alerts.length > 0 && (
                <PlantAlertGroups
                    alerts={data.alerts}
                    idPrefix="upcoming-deadlines"
                    listClassName="flex flex-col gap-3"
                    renderAlert={(alert) => (
                        <Deadline key={`${alert.type}-${alert.varietyId}-${alert.treatmentId}`} alert={alert} />
                    )}
                />
            )}
        </section>
    );
}
