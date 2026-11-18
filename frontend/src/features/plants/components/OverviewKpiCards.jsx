import { Apple, ChartNoAxesColumnIncreasing, Sprout, TriangleAlert } from "lucide-react";
import { useTranslation } from "react-i18next";
import { ErrorState, KpiCard, Skeleton } from "../../../shared/ui";
import { formatNumber } from "../utils/format";
import { maturityMeaning } from "../utils/overviewLabels";

/**
 * The figures of the API, in this order. Each says in one line what it counts,
 * in the words of the API's definitions: Ready for Harvest is the "Ready for
 * harvest" of the ring, not its "Ripe", which the ring explains.
 */
const FIGURES = [
    {
        field: "totalPlants",
        labelKey: "overview.kpi.totalPlants",
        note: (t) => t("overview.kpi.totalPlantsNote"),
        icon: Sprout,
        tone: "primary",
    },
    {
        field: "inGrowthStage",
        labelKey: "overview.kpi.inGrowthStage",
        note: (t) => t("overview.kpi.inGrowthStageNote"),
        icon: ChartNoAxesColumnIncreasing,
        tone: "primary",
    },
    {
        field: "readyForHarvest",
        labelKey: "overview.kpi.readyForHarvest",
        note: () => maturityMeaning("READY_FOR_HARVEST"),
        icon: Apple,
        tone: "warning",
    },
    {
        field: "activeAlerts",
        labelKey: "overview.kpi.activeAlerts",
        note: (t, overview) => t("overview.kpi.criticalNote", { count: formatNumber(overview.criticalAlerts) }),
        icon: TriangleAlert,
        tone: "error",
    },
];

const GRID = "grid grid-cols-2 gap-4 lg:grid-cols-4";

/**
 * The four figures of the day, as the API computes them for the selected
 * block; none of them is computed or added up here. They do not add up to the
 * total; the line under each value says which trees it counts.
 */
export default function OverviewKpiCards({ query }) {
    const { t } = useTranslation("plants");
    const { data: overview, isPending, isError, refetch, isFetching } = query;

    if (isPending) {
        // Four tiles of gray blocks, laid out as the figures will be, so nothing moves when they arrive.
        return (
            <div role="status" className={GRID}>
                <span className="sr-only">{t("overview.kpi.loading")}</span>
                {FIGURES.map((figure) => (
                    <Skeleton key={figure.field} radius="card" className="h-40" />
                ))}
            </div>
        );
    }

    if (isError) {
        return (
            <ErrorState
                message={`${t("overview.kpi.error")} ${t("common.checkConnection")}`}
                onRetry={() => refetch()}
                retrying={isFetching}
                retryLabel={t("common.retry")}
            />
        );
    }

    return (
        <section aria-label={t("overview.kpi.region")}>
            <ul className={GRID}>
                {FIGURES.map((figure) => (
                    <li key={figure.field} className="flex">
                        <KpiCard
                            className="w-full"
                            icon={figure.icon}
                            tone={figure.tone}
                            value={formatNumber(overview[figure.field])}
                            label={t(figure.labelKey)}
                            hint={figure.note(t, overview)}
                        />
                    </li>
                ))}
            </ul>
        </section>
    );
}
