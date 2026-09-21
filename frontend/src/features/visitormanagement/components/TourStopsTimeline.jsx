import { formatNumber } from "../utils/format";

/**
 * The mockup's vertical timeline of the standard tour. Stops come from the API
 * already ordered by position; the screen is display-only, exactly like the
 * mockup, which shows no stop management controls.
 */
export default function TourStopsTimeline({ stops }) {
    if (stops.length === 0) {
        return <p className="text-sm text-muted">No tour stops defined yet.</p>;
    }

    return (
        <div className="flex flex-col gap-4.5 border-l-[3px] border-primary pl-4.5">
            {stops.map((stop) => (
                <div key={stop.id} className="relative">
                    <span
                        aria-hidden="true"
                        className="absolute top-1 -left-[25px] h-2.5 w-2.5 rounded-full border-2 border-primary-dark bg-accent"
                    />
                    <p className="font-heading text-base font-bold text-primary-dark">
                        {stop.name}
                    </p>
                    <p className="mt-0.5 text-[11px] text-muted">
                        {stop.durationMinutes} min
                        {stop.location ? ` · ${stop.location}` : ""}
                        {stop.maxCapacity ? ` · max ${formatNumber(stop.maxCapacity)} pers` : ""}
                    </p>
                    {stop.description && (
                        <p className="mt-1 text-sm text-ink">{stop.description}</p>
                    )}
                    {stop.demo && (
                        <p className="mt-1 text-xs text-muted">
                            <span className="font-semibold">Demo:</span> {stop.demo}
                        </p>
                    )}
                    {stop.safetyNotes && (
                        <p className="mt-1 text-xs text-muted">
                            <span className="font-semibold">Safety:</span> {stop.safetyNotes}
                        </p>
                    )}
                </div>
            ))}
        </div>
    );
}