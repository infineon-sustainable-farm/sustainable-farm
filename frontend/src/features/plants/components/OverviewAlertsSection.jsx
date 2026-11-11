import { CircleCheck, TriangleAlert } from "lucide-react";
import { useHealthFindings } from "../hooks/useHealthFindings";
import { useNurseryBatches } from "../hooks/useNurseryBatches";
import { usePlantAlerts } from "../hooks/usePlantAlerts";
import { formatBlock } from "../utils/format";
import OverviewAlertRow from "./OverviewAlertRow";
import { ALERT_COLUMNS, CARD, CARD_CAPTION, CARD_TITLE, ERROR_BOX, ICON_SIZE, PRIMARY_BUTTON, SKELETON } from "./plantsStyles";

/** What the list covers; the day is the one under the page title. */
function scopeSentence(blockCode) {
    return blockCode ? `${formatBlock(blockCode)}. Stock alerts show under All blocks.` : "Most urgent first.";
}

/** Values by id, to name the batch or the tree an alert points at; empty until the list is there. */
function byId(items, pick) {
    return new Map((items ?? []).map((item) => [item.id, pick(item)]));
}

/**
 * Every plant alert of the day for the selected block, as the API computes and
 * orders them, one row each: harvest seasons, pre-harvest intervals, low
 * stocks, open health problems and batches to transplant. All of them are
 * listed; the Active Alerts figure above counts the same ones.
 *
 * The batch code and the tree come from the nursery batches (already read by
 * the survival chart) and the health findings; while either is missing, the
 * row names the nursery or the block.
 */
export default function OverviewAlertsSection({ blockCode }) {
    const { data, isPending, isError, refetch, isFetching } = usePlantAlerts({ blockCode });
    const batchCodes = byId(useNurseryBatches().data, (batch) => batch.batchCode);
    const treeLabels = byId(useHealthFindings().data, (finding) => finding.treeLabel);

    return (
        // A dashboard tile: the title sits in the card, as on the charts below.
        <section aria-labelledby="overview-alerts-title" className={`flex flex-col gap-2 ${CARD}`}>
            <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                <h2 id="overview-alerts-title" className={CARD_TITLE}>
                    Alerts
                </h2>
                <p className={CARD_CAPTION}>{scopeSentence(blockCode)}</p>
            </div>

            {isPending && (
                // Gray blocks in the place of the first rows; the sentence is for screen readers.
                <div role="status" className="flex flex-col gap-3 py-2">
                    <span className="sr-only">Loading alerts…</span>
                    <div aria-hidden="true" className={`h-4 w-full ${SKELETON}`} />
                    <div aria-hidden="true" className={`h-4 w-5/6 ${SKELETON}`} />
                    <div aria-hidden="true" className={`h-4 w-2/3 ${SKELETON}`} />
                </div>
            )}

            {isError && (
                <div className={ERROR_BOX}>
                    <div className="flex items-start gap-2">
                        <TriangleAlert size={ICON_SIZE} className="mt-0.5 shrink-0 text-error" />
                        <div>
                            <p className="text-sm font-bold text-on-background">Alerts are unavailable.</p>
                            <p className="mt-0.5 text-sm text-gray-600">Check your connection, then try again.</p>
                        </div>
                    </div>
                    <button type="button" onClick={() => refetch()} disabled={isFetching} className={PRIMARY_BUTTON}>
                        {isFetching ? "Retrying…" : "Retry"}
                    </button>
                </div>
            )}

            {!isPending && !isError && data.alerts.length === 0 && (
                <div className="flex items-center gap-2 text-sm text-gray-700">
                    <CircleCheck size={ICON_SIZE} aria-hidden="true" className="shrink-0 text-success" />
                    Nothing needs attention today.
                </div>
            )}

            {!isPending && !isError && data.alerts.length > 0 && (
                <div>
                    {/* Column names for the eye from lg up; each row reads in full on its own. */}
                    <div
                        aria-hidden="true"
                        className={`hidden gap-x-4 border-b border-gray-200 pb-1 text-xs font-semibold text-gray-600 lg:grid ${ALERT_COLUMNS}`}
                    >
                        <span>Priority</span>
                        <span>Where</span>
                        <span>Issue</span>
                        <span>When</span>
                        <span />
                    </div>
                    <ul className="flex flex-col divide-y divide-gray-200">
                        {data.alerts.map((alert) => (
                            <OverviewAlertRow
                                key={`${alert.type}-${alert.varietyId}-${alert.treatmentId}-${alert.findingId}-${alert.fertilizerId}-${alert.batchId}`}
                                alert={alert}
                                batchCode={alert.batchId == null ? null : batchCodes.get(alert.batchId)}
                                treeLabel={alert.findingId == null ? null : treeLabels.get(alert.findingId)}
                            />
                        ))}
                    </ul>
                </div>
            )}
        </section>
    );
}
