import { formatNumber } from "../utils/format";

/**
 * The mockup's occupancy bars: one row per activity, filled to the occupancy
 * percentage the API computed. The percentage is never recomputed here.
 */
export default function OccupancyBars({ occupancy }) {
    if (occupancy.length === 0) {
        return <p className="text-sm text-muted">No activity to measure yet.</p>;
    }

    return (
        <div className="flex flex-col gap-2">
            {occupancy.map((entry) => (
                <div key={entry.activityId} className="flex items-center gap-2.5 text-xs">
                    <span className="w-[110px] shrink-0 truncate text-muted" title={entry.activityName}>
                        {entry.activityName}
                    </span>
                    <div className="h-3 flex-1 overflow-hidden rounded-md bg-[#E8EFED]">
                        <div
                            className="h-full rounded-md bg-primary"
                            style={{ width: `${Math.min(100, Math.max(0, entry.occupancyPercent))}%` }}
                        />
                    </div>
                    <span className="w-9 shrink-0 text-right font-semibold text-primary-dark">
                        {entry.occupancyPercent}%
                    </span>
                    <span className="w-16 shrink-0 text-right text-muted">
                        {formatNumber(entry.booked)}/{formatNumber(entry.capacity)}
                    </span>
                </div>
            ))}
        </div>
    );
}