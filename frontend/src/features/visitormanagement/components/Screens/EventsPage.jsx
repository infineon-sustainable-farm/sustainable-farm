import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import {
    useCreateEvent,
    useEventStatusAction,
    useEvents,
    useUpdateEvent,
} from "../../hooks/useEvents";
import { Button, Field, Input, Modal, PageHeader, Select } from "../../../../shared/ui";
import { useLabels } from "../../utils/labels";
import { LoadError, LoadingBlock } from "../LoadState";
import EventCard from "../EventCard";
import EventForm from "../Forms/EventForm";
import EventParticipants from "../EventParticipants";

const EVENT_TYPES = ["OPEN_DAY", "PARTNER_BUYER", "SCHOOL"];

export default function EventsPage() {
    const { t } = useTranslation("visitormanagement");
    const labels = useLabels();
    const [editingEvent, setEditingEvent] = useState(null);
    const [formOpen, setFormOpen] = useState(false);
    const [selectedEventId, setSelectedEventId] = useState(null);
    const [formKey, setFormKey] = useState(0);
    const [filters, setFilters] = useState({ type: "", date: "" });
    const [showCancelledEvents, setShowCancelledEvents] = useState(false);

    /*
     * The API applies only the first non-null filter, so the type and the date clear each other.
     */
    function updateFilter(key, value) {
        setFilters({ type: key === "type" ? value : "", date: key === "date" ? value : "" });
    }

    const hasActiveFilter = Boolean(filters.type || filters.date);

    useEffect(() => {
        document.title = t("events.documentTitle");
    }, [t]);

    const { data: events, isPending, isError, refetch, isFetching } = useEvents(filters);

    const createMutation = useCreateEvent();
    const updateMutation = useUpdateEvent();
    const statusMutation = useEventStatusAction();

    const formMutation = editingEvent ? updateMutation : createMutation;

    // Derived from the list so the panel stays fresh after an invalidation.
    const selectedEvent = (events ?? []).find((event) => event.id === selectedEventId) ?? null;

    // Cancelled events stay out of the grid unless asked for.
    const visibleEvents = (events ?? []).filter(
        (event) => showCancelledEvents || event.status !== "CANCELLED",
    );

    function resetForm() {
        setEditingEvent(null);
        setFormOpen(false);
        setFormKey((current) => current + 1);
        createMutation.reset();
        updateMutation.reset();
    }

    function openCreate() {
        createMutation.reset();
        updateMutation.reset();
        setEditingEvent(null);
        setFormOpen(true);
        setFormKey((current) => current + 1);
    }

    function openEdit(event) {
        createMutation.reset();
        updateMutation.reset();
        setSelectedEventId(null);
        setEditingEvent(event);
        setFormOpen(true);
        setFormKey((current) => current + 1);
    }

    function handleSubmit(values) {
        if (editingEvent) {
            updateMutation.mutate({ id: editingEvent.id, data: values }, { onSuccess: resetForm });
        } else {
            createMutation.mutate(values, { onSuccess: resetForm });
        }
    }

    const pendingAction = statusMutation.isPending ? statusMutation.variables : null;
    const actionError = statusMutation.isError
        ? { id: statusMutation.variables?.id, message: statusMutation.error.message }
        : null;

    return (
        <div className="p-6 space-y-6">
            <PageHeader
                title={t("events.title")}
                actions={
                    <Button icon={Plus} onClick={openCreate}>
                        {t("events.add")}
                    </Button>
                }
            />

            <div className="flex flex-wrap items-end gap-4">
                <Field id="ev-filter-type" className="min-w-44" label={t("events.filters.type")}>
                    <Select value={filters.type} onChange={(event) => updateFilter("type", event.target.value)}>
                        <option value="">{t("events.filters.allTypes")}</option>
                        {EVENT_TYPES.map((type) => (
                            <option key={type} value={type}>
                                {labels.enum(type)}
                            </option>
                        ))}
                    </Select>
                </Field>

                <Field id="ev-filter-date" label={t("events.filters.date")}>
                    <Input
                        type="date"
                        value={filters.date}
                        onChange={(event) => updateFilter("date", event.target.value)}
                    />
                </Field>

                {hasActiveFilter && (
                    <Button variant="secondary" onClick={() => setFilters({ type: "", date: "" })}>
                        {t("events.filters.clear")}
                    </Button>
                )}

                <label className="flex min-h-10 items-center gap-2 text-sm text-text">
                    <input
                        type="checkbox"
                        checked={showCancelledEvents}
                        onChange={(event) => setShowCancelledEvents(event.target.checked)}
                        className="h-4 w-4 accent-primary"
                    />
                    {t("events.filters.showCancelled")}
                </label>
            </div>

            {isPending && <LoadingBlock label={t("events.loading")} />}

            {isError && (
                <LoadError title={t("events.loadError")} onRetry={() => refetch()} retrying={isFetching} />
            )}

            {!isPending && !isError && (
                <>
                    {visibleEvents.length === 0 ? (
                        <p className="text-sm text-text-muted">{t("events.empty")}</p>
                    ) : (
                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
                            {visibleEvents.map((event) => (
                                <EventCard
                                    key={event.id}
                                    event={event}
                                    pendingAction={pendingAction}
                                    actionError={actionError?.id === event.id ? actionError : null}
                                    onAction={(target, action) =>
                                        statusMutation.mutate({ id: target.id, action })
                                    }
                                    onEdit={openEdit}
                                    onParticipants={(target) =>
                                        setSelectedEventId((current) =>
                                            current === target.id ? null : target.id,
                                        )
                                    }
                                />
                            ))}
                        </div>
                    )}

                    {selectedEvent && (
                        <EventParticipants event={selectedEvent} onClose={() => setSelectedEventId(null)} />
                    )}

                    <p className="text-xs text-text-muted">{t("events.footnote")}</p>
                </>
            )}

            <Modal
                open={formOpen}
                onClose={resetForm}
                size="xl"
                title={
                    editingEvent
                        ? t("events.editTitle", { title: editingEvent.title })
                        : t("events.newTitle")
                }
            >
                <EventForm
                    key={formKey}
                    event={editingEvent}
                    isSubmitting={formMutation.isPending}
                    submitError={formMutation.error?.message}
                    serverFieldErrors={formMutation.error?.data?.fieldErrors}
                    onSubmit={handleSubmit}
                    onCancel={resetForm}
                />
            </Modal>
        </div>
    );
}
