import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Card, EmptyState, ErrorState, Input, Select } from "../../../shared/ui";
import { useNurseryBatches } from "../hooks/useNurseryBatches";
import { useNurseryEvents } from "../hooks/useNurseryEvents";
import { formatEnum } from "../utils/format";
import { NURSERY_EVENT_TYPES } from "../utils/nurseryRequests";
import NurseryEventsTable from "./NurseryEventsTable";

/**
 * One filter of the history. Its accessible name joins the section title and
 * its own label, so "From" here is told apart from any other "From".
 */
function FilterField({ id, label, children }) {
    return (
        <div className="flex max-w-full min-w-0 items-center gap-3">
            <label id={`${id}-label`} htmlFor={id} className="shrink-0 text-sm font-medium">
                {label}
            </label>
            <div className="min-w-0">{children}</div>
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
    const { t } = useTranslation("plants");
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
    const typeOptions = NURSERY_EVENT_TYPES.map((type) => ({ value: type, label: formatEnum("nurseryEvent", type) }));

    return (
        <Card
            aria-labelledby="nursery-history-title"
            title={<span id="nursery-history-title">{t("nursery.history.title")}</span>}
            subtitle={t("nursery.history.subtitle")}
        >
            <div className="mb-4 flex flex-wrap items-center gap-x-6 gap-y-3">
                <FilterField id="nursery-history-batch" label={t("nursery.history.batch")}>
                    <Select
                        id="nursery-history-batch"
                        value={batchId}
                        onChange={(event) => setBatchId(event.target.value)}
                        aria-labelledby={labelledBy("nursery-history-batch")}
                    >
                        <option value="">{t("nursery.history.allBatches")}</option>
                        {(batches ?? []).map((batch) => (
                            <option key={batch.id} value={batch.id}>
                                {t("nursery.batches.batchOption", { code: batch.batchCode, variety: batch.varietyName })}
                            </option>
                        ))}
                    </Select>
                </FilterField>

                <FilterField id="nursery-history-event" label={t("nursery.history.event")}>
                    <Select
                        id="nursery-history-event"
                        value={eventType}
                        onChange={(event) => setEventType(event.target.value)}
                        aria-labelledby={labelledBy("nursery-history-event")}
                    >
                        <option value="">{t("nursery.history.allEvents")}</option>
                        {typeOptions.map((option) => (
                            <option key={option.value} value={option.value}>
                                {option.label}
                            </option>
                        ))}
                    </Select>
                </FilterField>

                <FilterField id="nursery-history-from" label={t("nursery.history.from")}>
                    <Input
                        id="nursery-history-from"
                        type="date"
                        value={from}
                        onChange={(event) => setFrom(event.target.value)}
                        aria-labelledby={labelledBy("nursery-history-from")}
                    />
                </FilterField>

                <FilterField id="nursery-history-to" label={t("nursery.history.to")}>
                    <Input
                        id="nursery-history-to"
                        type="date"
                        value={to}
                        onChange={(event) => setTo(event.target.value)}
                        aria-labelledby={labelledBy("nursery-history-to")}
                    />
                </FilterField>
            </div>

            {isPending && <NurseryEventsTable loading />}

            {isError && (
                <ErrorState
                    message={`${t("nursery.history.error")}. ${t("nursery.batches.errorHint")}`}
                    onRetry={() => refetch()}
                    retrying={isFetching}
                    retryLabel={t("common.retry")}
                />
            )}

            {!isPending && !isError && events.length === 0 && (
                isFiltered ? (
                    <EmptyState
                        title={t("nursery.history.emptyFiltered")}
                        message={t("nursery.history.emptyFilteredHint")}
                    />
                ) : (
                    <EmptyState message={t("nursery.history.empty")} />
                )
            )}

            {!isPending && !isError && events.length > 0 && <NurseryEventsTable events={events} />}
        </Card>
    );
}
