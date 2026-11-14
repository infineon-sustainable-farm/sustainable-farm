import { CircleCheck, Loader2, TriangleAlert } from "lucide-react";
import { useNurseryAlerts } from "../hooks/useNurseryAlerts";
import PlantAlertItem from "./PlantAlertItem";
import { PRIMARY_BUTTON } from "./plantsStyles";

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
    const { data, isPending, isError, refetch, isFetching } = useNurseryAlerts();

    return (
        <section aria-labelledby="transplant-alerts-title" className="flex flex-col gap-3">
            <div>
                <h2 id="transplant-alerts-title" className="font-heading text-lg font-bold text-on-background">
                    Transplant alerts
                </h2>
                <p className="mt-1 text-sm text-gray-600">
                    All batches.
                </p>
            </div>

            {isPending && (
                <div className="flex items-center gap-3 rounded-2xl border border-gray-200 bg-white shadow-card p-6 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading transplant alerts…
                </div>
            )}

            {isError && (
                <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-error/30 bg-error/5 p-6">
                    <div className="flex items-start gap-3">
                        <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                        <div>
                            <p className="text-sm font-bold text-on-background">Alerts are unavailable.</p>
                            {/* gray-600: #4a5565 on the tinted box (#f5f1f5) -> 6.76:1, as in the other sections. */}
                            <p className="mt-0.5 text-sm text-gray-600">Check your connection, then try again.</p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={() => refetch()}
                        disabled={isFetching}
                        className={PRIMARY_BUTTON}
                    >
                        {isFetching ? "Retrying…" : "Retry"}
                    </button>
                </div>
            )}

            {!isPending && !isError && data.alerts.length === 0 && (
                <div className="flex items-center gap-3 rounded-2xl border border-gray-200 bg-white shadow-card p-6 text-sm text-gray-700">
                    <CircleCheck size={18} aria-hidden="true" className="shrink-0 text-success" />
                    No batch to transplant today.
                </div>
            )}

            {!isPending && !isError && data.alerts.length > 0 && (
                <ul className="flex flex-col gap-3">
                    {data.alerts.map((alert) => (
                        <PlantAlertItem
                            key={`${alert.type}-${alert.batchId}`}
                            alert={alert}
                            title="Batch to transplant"
                            dateLabel="Planned transplant"
                        />
                    ))}
                </ul>
            )}
        </section>
    );
}
