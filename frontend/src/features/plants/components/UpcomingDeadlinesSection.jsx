import { CircleCheck, Loader2, TriangleAlert } from "lucide-react";
import { useHarvestAlerts } from "../hooks/useHarvestAlerts";
import { formatCode } from "../utils/format";
import PlantAlertItem from "./PlantAlertItem";

// What the date of each alert type is.
const TYPE_LABELS = {
    HARVEST_APPROACHING: { title: "Harvest season", date: "Window from" },
    PRE_HARVEST_INTERVAL: { title: "Pre-harvest interval", date: "Harvest allowed from" },
};

/** One deadline, worded for its type. */
function Deadline({ alert }) {
    const labels = TYPE_LABELS[alert.type] ?? { title: formatCode(alert.type), date: "Date:" };
    return <PlantAlertItem alert={alert} title={labels.title} dateLabel={labels.date} />;
}

/**
 * The harvest deadlines of the selected block, as the API computes them: the
 * harvest seasons in progress or opening within its look-ahead, and the
 * pre-harvest intervals still running, critical while the season of their
 * block is open. They come in the order of the API, the most urgent first;
 * nothing is sorted or dated here.
 *
 * Below the maturity status, as in the mock-up, they follow the block filter
 * like the rest of the page.
 */
export default function UpcomingDeadlinesSection({ blockCode }) {
    const { data, isPending, isError, refetch, isFetching } = useHarvestAlerts({ blockCode });

    return (
        <section aria-labelledby="upcoming-deadlines-title" className="flex flex-col gap-3">
            <div>
                <h2 id="upcoming-deadlines-title" className="font-heading text-lg font-bold text-gray-900">
                    Upcoming deadlines
                </h2>
                {data && (
                    <p className="mt-1 text-sm text-gray-600">
                        {`Harvest seasons and waiting periods in the next ${data.withinDays} days.`}
                    </p>
                )}
            </div>

            {isPending && (
                <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading upcoming deadlines…
                </div>
            )}

            {isError && (
                <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-error/30 bg-error/5 p-6">
                    <div className="flex items-start gap-3">
                        <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                        <div>
                            <p className="text-sm font-bold text-gray-900">Upcoming dates are unavailable.</p>
                            {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1, as in the other sections. */}
                            <p className="mt-0.5 text-sm text-gray-600">Check your connection, then try again.</p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={() => refetch()}
                        disabled={isFetching}
                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                    >
                        {isFetching ? "Retrying…" : "Retry"}
                    </button>
                </div>
            )}

            {!isPending && !isError && data.alerts.length === 0 && (
                <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-700">
                    <CircleCheck size={18} aria-hidden="true" className="shrink-0 text-success" />
                    {`Nothing in the next ${data.withinDays} days.`}
                </div>
            )}

            {!isPending && !isError && data.alerts.length > 0 && (
                <ul className="flex flex-col gap-3">
                    {data.alerts.map((alert) => (
                        <Deadline key={`${alert.type}-${alert.varietyId}-${alert.treatmentId}`} alert={alert} />
                    ))}
                </ul>
            )}
        </section>
    );
}
