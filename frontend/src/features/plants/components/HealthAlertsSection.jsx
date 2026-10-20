import { CircleCheck, Loader2, TriangleAlert } from "lucide-react";
import { useHealthAlerts } from "../hooks/useHealthAlerts";
import { formatCode, formatDate } from "../utils/format";

/*
 * The mock-up's callout: a tinted box and a solid pill. Its red tint and border
 * hold the critical alerts; the warnings take the amber tint of the badges.
 * Pills in the darkened hues, white text: #9a3f32 -> 6.71:1, #8e661c -> 5.16:1
 * (white on the mock-up's #c1503f gave 4.68:1, and on #d9a441 2.25:1).
 */
const SEVERITY_STYLES = {
    CRITICAL: { box: "border-[#edcdc8] bg-[#f7e9e7]", icon: "text-error", pill: "bg-[#9a3f32]" },
    WARNING: { box: "border-[#ecd9b0] bg-[#f8efde]", icon: "text-[#8e661c]", pill: "bg-[#8e661c]" },
};
const NEUTRAL = { box: "border-gray-200 bg-white", icon: "text-gray-600", pill: "bg-gray-700" };

// What the date of each alert type is.
const TYPE_LABELS = {
    OPEN_HEALTH_ISSUE: { title: "Open health problem", date: "Seen on" },
    PRE_HARVEST_INTERVAL: { title: "Pre-harvest interval", date: "Harvest allowed from" },
};

/** One alert: its kind and severity, the API's message as it is, and its date. */
function HealthAlert({ alert }) {
    const style = SEVERITY_STYLES[alert.severity] ?? NEUTRAL;
    const labels = TYPE_LABELS[alert.type] ?? { title: formatCode(alert.type), date: "Date:" };
    return (
        <li className={`flex items-start gap-4 rounded-xl border px-4 py-4 ${style.box}`}>
            <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-white">
                <TriangleAlert size={20} aria-hidden="true" className={style.icon} />
            </div>
            <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                    <p className="text-sm font-bold text-gray-900">{labels.title}</p>
                    <span
                        className={`rounded-full px-2.5 py-0.5 text-[11px] font-bold tracking-wide text-white uppercase ${style.pill}`}
                    >
                        {formatCode(alert.severity)}
                    </span>
                </div>
                <p className="mt-1 text-sm break-words text-gray-800">{alert.message}</p>
                <p className="mt-1 text-xs text-gray-600">{`${labels.date} ${formatDate(alert.date)}`}</p>
            </div>
        </li>
    );
}

/**
 * The health alerts of the day, as the API computes them: the findings not
 * resolved yet, and the treatments whose pre-harvest interval is still running,
 * critical while the harvest season of their block is open.
 *
 * They stay above the block filter, as in the mock-up, and cover every block:
 * an urgent alert stays in view whatever block is selected.
 */
export default function HealthAlertsSection() {
    const { data, isPending, isError, refetch, isFetching } = useHealthAlerts();

    return (
        <section aria-labelledby="health-alerts-title" className="flex flex-col gap-3">
            <div>
                <h3 id="health-alerts-title" className="font-heading text-base font-bold text-gray-900">
                    Health alerts
                </h3>
                <p className="mt-1 text-sm text-gray-600">
                    {data
                        ? `Every block, as of ${formatDate(data.today)}. The block filter does not apply here.`
                        : "Every block. The block filter does not apply here."}
                </p>
            </div>

            {isPending && (
                <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-4 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading health alerts…
                </div>
            )}

            {isError && (
                <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-error/30 bg-error/5 px-5 py-4">
                    <div className="flex items-start gap-3">
                        <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                        <div>
                            <p className="text-sm font-bold text-gray-900">Health alerts could not be loaded</p>
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
                    No health alert today.
                </div>
            )}

            {!isPending && !isError && data.alerts.length > 0 && (
                <ul className="flex flex-col gap-3">
                    {data.alerts.map((alert) => (
                        <HealthAlert key={`${alert.type}-${alert.findingId}-${alert.treatmentId}`} alert={alert} />
                    ))}
                </ul>
            )}
        </section>
    );
}
