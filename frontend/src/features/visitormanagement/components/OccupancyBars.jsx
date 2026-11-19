import { useTranslation } from "react-i18next";
import { useVisitorFormat } from "../utils/format";

/**
 * The occupancy bars: one row per activity, filled to the occupancy percentage the API computed.
 * The percentage is never recomputed here.
 */
export default function OccupancyBars({ occupancy }) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();

    if (occupancy.length === 0) {
        return <p className="text-sm text-text-muted">{t("occupancy.empty")}</p>;
    }

    return (
        <div className="flex flex-col gap-2">
            {occupancy.map((entry) => (
                <div key={entry.activityId} className="flex items-center gap-2.5 text-xs">
                    <span className="w-28 shrink-0 truncate text-text-muted" title={entry.activityName}>
                        {entry.activityName}
                    </span>
                    <div className="h-3 flex-1 overflow-hidden rounded-control bg-neutral-bg">
                        <div
                            className="h-full rounded-control bg-primary"
                            style={{ width: `${Math.min(100, Math.max(0, entry.occupancyPercent))}%` }}
                        />
                    </div>
                    <span className="w-9 shrink-0 text-right font-semibold text-text">
                        {t("occupancy.percent", { value: entry.occupancyPercent })}
                    </span>
                    <span className="w-16 shrink-0 text-right text-text-muted">
                        {t("occupancy.ratio", {
                            booked: format.number(entry.booked),
                            capacity: format.number(entry.capacity),
                        })}
                    </span>
                </div>
            ))}
        </div>
    );
}
