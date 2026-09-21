import { useEffect, useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import {
    useCreateEvent,
    useEventStatusAction,
    useEvents,
    useUpdateEvent,
} from "../../hooks/useEvents";
import EventCard from "../EventCard";
import EventForm from "../Forms/EventForm";
import EventParticipants from "../EventParticipants";
import { formatEnumLabel } from "../../../../shared/utils/formatEnumLabel";

const EVENT_TYPES = ["OPEN_DAY", "PARTNER_BUYER", "SCHOOL", "COMMUNITY"];

export default function EventsPage() {
    const [editingEvent, setEditingEvent] = useState(null);
    const [selectedEventId, setSelectedEventId] = useState(null);
    const [formKey, setFormKey] = useState(0);
    const [filters, setFilters] = useState({ type: "", date: "" });

    /*
     * The API applies only the first non-null filter, so the type and the date
     * clear each other.
     */
    function updateFilter(key, value) {
        setFilters({ type: key === "type" ? value : "", date: key === "date" ? value : "" });
    }

    const hasActiveFilter = Boolean(filters.type || filters.date);

    useEffect(() => {
        document.title = "Events — Visitor Management";
    }, []);

    const {
        data: events,
        isPending,
        isError,
        refetch,
        isFetching,
    } = useEvents(filters);

    const createMutation = useCreateEvent();
    const updateMutation = useUpdateEvent();
    const statusMutation = useEventStatusAction();

    const formMutation = editingEvent ? updateMutation : createMutation;

    // Derived from the list so the panel stays fresh after an invalidation.
    const selectedEvent = (events ?? []).find((event) => event.id === selectedEventId) ?? null;

    function resetForm() {
        setEditingEvent(null);
        setFormKey((current) => current + 1);
        createMutation.reset();
        updateMutation.reset();
    }

    function openEdit(event) {
        createMutation.reset();
        updateMutation.reset();
        setSelectedEventId(null);
        setEditingEvent(event);
        setFormKey((current) => current + 1);
    }

    function handleSubmit(values) {
        if (editingEvent) {
            updateMutation.mutate(
                { id: editingEvent.id, data: values },
                { onSuccess: resetForm },
            );
        } else {
            createMutation.mutate(values, { onSuccess: resetForm });
        }
    }

    const pendingAction = statusMutation.isPending ? statusMutation.variables : null;
    const actionError = statusMutation.isError
        ? { id: statusMutation.variables?.id, message: statusMutation.error.message }
        : null;

    return (
        <div className="min-h-full bg-[#F5F7FA] px-8 py-7">
            <section className="rounded-lg border border-line bg-white p-7">
                <h2 className="font-heading mb-5 inline-block border-b-[3px] border-accent pb-2 text-2xl font-bold text-primary-dark">
                    Events
                </h2>

                <div className="mb-5 flex flex-wrap items-end gap-3.5">
                    <div>
                        <label
                            htmlFor="ev-filter-type"
                            className="mb-1 block text-xs tracking-wide text-primary uppercase"
                        >
                            Type
                        </label>
                        <select
                            id="ev-filter-type"
                            value={filters.type}
                            onChange={(event) => updateFilter("type", event.target.value)}
                            className="min-w-[170px] rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                        >
                            <option value="">All types</option>
                            {EVENT_TYPES.map((type) => (
                                <option key={type} value={type}>
                                    {formatEnumLabel(type)}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label
                            htmlFor="ev-filter-date"
                            className="mb-1 block text-xs tracking-wide text-primary uppercase"
                        >
                            Date
                        </label>
                        <input
                            id="ev-filter-date"
                            type="date"
                            value={filters.date}
                            onChange={(event) => updateFilter("date", event.target.value)}
                            className="rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                        />
                    </div>

                    {hasActiveFilter && (
                        <button
                            type="button"
                            onClick={() => setFilters({ type: "", date: "" })}
                            className="rounded-md border border-line bg-white px-3.5 py-2 text-xs text-primary hover:bg-[#F2FBF9]"
                        >
                            Clear filters
                        </button>
                    )}
                </div>

                {isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-lg border border-line bg-white px-6 py-16 text-sm text-muted">
                        <Loader2 size={18} className="animate-spin" />
                        Loading events…
                    </div>
                )}

                {isError && (
                    <div className="flex flex-col items-center gap-4 rounded-lg border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <TriangleAlert size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-ink">
                                Events could not be loaded
                            </p>
                            <p className="mt-1 text-sm text-muted">
                                The request to the server did not succeed.
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={() => refetch()}
                            disabled={isFetching}
                            className="rounded-md bg-primary px-4 py-2 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:opacity-60"
                        >
                            {isFetching ? "Retrying…" : "Retry"}
                        </button>
                    </div>
                )}

                {!isPending && !isError && (
                    <>
                        <div className="mb-6 grid grid-cols-1 gap-3.5 sm:grid-cols-2 xl:grid-cols-3">
                            {events.length === 0 && (
                                <p className="text-sm text-muted">No event yet.</p>
                            )}
                            {events.map((event) => (
                                <EventCard
                                    key={event.id}
                                    event={event}
                                    pendingAction={pendingAction}
                                    actionError={
                                        actionError?.id === event.id ? actionError : null
                                    }
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

                        {selectedEvent && (
                            <EventParticipants
                                event={selectedEvent}
                                onClose={() => setSelectedEventId(null)}
                            />
                        )}

                        <h3 className="font-heading mb-3 text-[17px] font-bold text-ink">
                            {editingEvent ? `Edit event: ${editingEvent.title}` : "Create new event"}
                        </h3>

                        {/* The mockup keeps the create form visible at all times; it
                            switches to edit mode when a card's edit action is used. */}
                        <EventForm
                            key={formKey}
                            event={editingEvent}
                            isSubmitting={formMutation.isPending}
                            submitError={formMutation.error?.message}
                            serverFieldErrors={formMutation.error?.data?.fieldErrors}
                            onSubmit={handleSubmit}
                            onCancel={resetForm}
                        />

                        <p className="mt-6 text-[11px] text-muted">
                            Events bring several visitors or groups together under one occasion.
                            Registrations, safety briefings and feedback stay linked to each
                            visitor's own record, and past events are completed automatically.
                        </p>
                    </>
                )}
            </section>
        </div>
    );
}