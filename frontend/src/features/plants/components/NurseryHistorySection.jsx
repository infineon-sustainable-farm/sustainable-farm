import { useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import { useNurseryBatches } from "../hooks/useNurseryBatches";
import { useNurseryEvents } from "../hooks/useNurseryEvents";
import { formatCode } from "../utils/format";
import NurseryEventsTable from "./NurseryEventsTable";
import PlantsEmptyState from "./PlantsEmptyState";

// The event types the API records.
const EVENT_TYPES = ["STAGE_CHANGE", "LOSS", "TRANSPLANT"];

const LABEL_CLASS = "text-xs font-semibold text-gray-500";
const CONTROL_CLASS =
    "min-w-0 max-w-full rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm text-gray-800 focus:border-primary focus:outline-none";

/**
 * One filter of the history. Its accessible name joins the section title and
 * its own label, so "From" here is told apart from any other "From".
 */
function FilterField({ id, label, children }) {
    return (
        <div className="flex max-w-full min-w-0 items-center gap-3">
            <label id={`${id}-label`} htmlFor={id} className={`${LABEL_CLASS} shrink-0`}>
                {label}
            </label>
            {children}
        </div>
    );
}

/**
 * What happened to the batches: their stage changes, the initial one
 * included, their losses with the reason, and their transplants with the
 * block. Filtered by batch, event type and dates, both included, all sent to
 * the API. Filters the API cannot match give an empty list.
 *
 * The events have no destination block, so the page's block filter does not
 * apply here.
 */
export default function NurseryHistorySection() {
    // Raw values as the API takes them; an empty string means "no filter".
    const [batchId, setBatchId] = useState("");
    const [eventType, setEventType] = useState("");
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");

    const { data: batches } = useNurseryBatches();
    const { data: events, isPending, isError, refetch, isFetching } = useNurseryEvents({
        batchId: batchId ? Number(batchId) : null,
        eventType: eventType || null,
        from: from || null,
        to: to || null,
    });
    const isFiltered = Boolean(batchId || eventType || from || to);
    const labelledBy = (id) => `nursery-history-title ${id}-label`;

    return (
        <section aria-labelledby="nursery-history-title" className="flex flex-col gap-4">
            <div>
                <h3 id="nursery-history-title" className="font-heading text-base font-bold text-gray-900">
                    Nursery history
                </h3>
                <p className="mt-1 text-sm text-gray-600">The destination block filter does not apply here.</p>
            </div>

            <div className="flex flex-wrap items-center gap-x-6 gap-y-3 rounded-xl border border-gray-200 bg-white px-4 py-3">
                <FilterField id="nursery-history-batch" label="Batch">
                    <select
                        id="nursery-history-batch"
                        value={batchId}
                        onChange={(event) => setBatchId(event.target.value)}
                        aria-labelledby={labelledBy("nursery-history-batch")}
                        className={CONTROL_CLASS}
                    >
                        <option value="">All batches</option>
                        {(batches ?? []).map((batch) => (
                            <option key={batch.id} value={batch.id}>
                                {`${batch.batchCode} · ${batch.varietyName}`}
                            </option>
                        ))}
                    </select>
                </FilterField>

                <FilterField id="nursery-history-event" label="Event">
                    <select
                        id="nursery-history-event"
                        value={eventType}
                        onChange={(event) => setEventType(event.target.value)}
                        aria-labelledby={labelledBy("nursery-history-event")}
                        className={CONTROL_CLASS}
                    >
                        <option value="">All events</option>
                        {EVENT_TYPES.map((type) => (
                            <option key={type} value={type}>
                                {formatCode(type)}
                            </option>
                        ))}
                    </select>
                </FilterField>

                <FilterField id="nursery-history-from" label="From">
                    <input
                        id="nursery-history-from"
                        type="date"
                        value={from}
                        onChange={(event) => setFrom(event.target.value)}
                        aria-labelledby={labelledBy("nursery-history-from")}
                        className={CONTROL_CLASS}
                    />
                </FilterField>

                <FilterField id="nursery-history-to" label="To">
                    <input
                        id="nursery-history-to"
                        type="date"
                        value={to}
                        onChange={(event) => setTo(event.target.value)}
                        aria-labelledby={labelledBy("nursery-history-to")}
                        className={CONTROL_CLASS}
                    />
                </FilterField>
            </div>

            {isPending && (
                <div className="flex items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white px-6 py-16 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading nursery events…
                </div>
            )}

            {isError && (
                <div className="flex flex-col items-center gap-4 rounded-xl border border-error/30 bg-error/5 px-6 py-16 text-center">
                    <TriangleAlert size={28} className="text-error" />
                    <div>
                        <p className="font-heading text-base font-bold text-gray-900">
                            Nursery events could not be loaded
                        </p>
                        {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1; gray-500 gave 4.32:1. */}
                        <p className="mt-1 text-sm text-gray-600">
                            The request to the server did not succeed.
                        </p>
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

            {!isPending && !isError && events.length === 0 && (
                isFiltered ? (
                    <PlantsEmptyState
                        title="No event matches these filters"
                        hint="Try another batch, event or date range."
                    />
                ) : (
                    <PlantsEmptyState title="No nursery event recorded yet" />
                )
            )}

            {!isPending && !isError && events.length > 0 && <NurseryEventsTable events={events} />}
        </section>
    );
}
