import { CircleCheck, Loader2, TriangleAlert } from "lucide-react";
import { usePlantAlerts } from "../hooks/usePlantAlerts";
import { formatBlock, formatCode, formatDate } from "../utils/format";
import PlantAlertItem from "./PlantAlertItem";

// The five alert types, worded as on the screens that list them; a stock alert has no date.
const TYPE_LABELS = {
    HARVEST_APPROACHING: { title: "Harvest season", date: "Window from" },
    PRE_HARVEST_INTERVAL: { title: "Pre-harvest interval", date: "Harvest allowed from" },
    OPEN_HEALTH_ISSUE: { title: "Open health problem", date: "Seen on" },
    NURSERY_READY: { title: "Batch to transplant", date: "Planned transplant" },
    LOW_STOCK: { title: "Low stock", date: null },
};

/** One alert, worded for its type. */
function OverviewAlert({ alert }) {
    const labels = TYPE_LABELS[alert.type] ?? { title: formatCode(alert.type), date: "Date:" };
    return <PlantAlertItem alert={alert} title={labels.title} dateLabel={labels.date} />;
}

/** What the list covers, and the day the API computed it for once it is known. */
function scopeSentence(blockCode, data) {
    const scope = blockCode ? formatBlock(blockCode) : "Every block";
    const day = data ? `, as of ${formatDate(data.today)}` : "";
    const stock = blockCode ? " Stock alerts are listed for all blocks only: a fertilizer belongs to no block." : "";
    return `${scope}${day}, the most urgent first.${stock}`;
}

/**
 * Every plant alert of the day for the selected block, as the API computes and
 * orders them: harvest seasons, pre-harvest intervals, low stocks, open health
 * problems and batches to transplant. All of them are listed; the Active
 * Alerts figure above counts the same ones.
 */
export default function OverviewAlertsSection({ blockCode }) {
    const { data, isPending, isError, refetch, isFetching } = usePlantAlerts({ blockCode });

    return (
        <section aria-labelledby="overview-alerts-title" className="flex flex-col gap-3">
            <div>
                <h3 id="overview-alerts-title" className="font-heading text-base font-bold text-gray-900">
                    Alerts
                </h3>
                <p className="mt-1 text-sm text-gray-600">{scopeSentence(blockCode, data)}</p>
            </div>

            {isPending && (
                <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-4 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading alerts…
                </div>
            )}

            {isError && (
                <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-error/30 bg-error/5 px-5 py-4">
                    <div className="flex items-start gap-3">
                        <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                        <div>
                            <p className="text-sm font-bold text-gray-900">Alerts could not be loaded</p>
                            {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1, as in the other sections. */}
                            <p className="mt-0.5 text-sm text-gray-600">The request to the server did not succeed.</p>
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
                <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-4 text-sm text-gray-700">
                    <CircleCheck size={18} aria-hidden="true" className="shrink-0 text-success" />
                    No alert today.
                </div>
            )}

            {!isPending && !isError && data.alerts.length > 0 && (
                <ul className="flex flex-col gap-3">
                    {data.alerts.map((alert) => (
                        <OverviewAlert
                            key={`${alert.type}-${alert.varietyId}-${alert.treatmentId}-${alert.findingId}-${alert.fertilizerId}-${alert.batchId}`}
                            alert={alert}
                        />
                    ))}
                </ul>
            )}
        </section>
    );
}
