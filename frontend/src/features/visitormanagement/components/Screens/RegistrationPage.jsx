import { useEffect, useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
    useAvailability,
    useRegisterVisitor,
    useRegistrationAction,
    useRegistrations,
} from "../../hooks/useRegistrations";
import { useUpdateVisitor, useVisitors } from "../../hooks/useVisitors";
import { useRegisterEventVisitor, useEvents } from "../../hooks/useEvents";
import { useActivities, useCreateBooking } from "../../hooks/useBooking";
import { useTimeSlots } from "../../hooks/useScheduling";
import { toIsoDate } from "../../utils/format";
import Modal from "../Modal";
import RegistrationForm from "../Forms/RegistrationForm";
import RegistrationsTable from "../RegistrationsTable";
import ProspectsSection from "../ProspectsSection";

export default function RegistrationPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const preselectedEventId = searchParams.get("eventId");
    const [mode, setMode] = useState(preselectedEventId ? "EVENT" : "TOUR");
    const [date, setDate] = useState(() => toIsoDate(new Date()));
    const [editing, setEditing] = useState(null);
    const [formOpen, setFormOpen] = useState(Boolean(preselectedEventId));
    const [preselectedEvent, setPreselectedEvent] = useState(
        preselectedEventId ? Number(preselectedEventId) : null,
    );
    const [formKey, setFormKey] = useState(0);
    const [showCancelled, setShowCancelled] = useState(false);

    useEffect(() => {
        document.title = "Visitor Registration — Visitor Management";
    }, []);

    const {
        data: registrations,
        isPending,
        isError,
        refetch,
        isFetching,
    } = useRegistrations();
    const { data: visitors, isError: visitorsError } = useVisitors();
    const { data: timeSlots } = useTimeSlots();
    const { data: events } = useEvents();
    const { data: activities } = useActivities();
    const {
        data: availability,
        isPending: availabilityLoading,
        isError: availabilityError,
        refetch: refetchAvailability,
    } = useAvailability(date);

    const registerMutation = useRegisterVisitor();
    const eventRegisterMutation = useRegisterEventVisitor();
    const createBookingMutation = useCreateBooking();
    const updateVisitorMutation = useUpdateVisitor();
    const actionMutation = useRegistrationAction();

    const visitorsById = new Map((visitors ?? []).map((visitor) => [visitor.id, visitor]));
    const slotsById = new Map((timeSlots ?? []).map((slot) => [slot.id, slot]));
    const eventsById = new Map((events ?? []).map((event) => [event.id, event]));

    // Only published events accept registrations; only active activities can
    // be booked.
    const publishedEvents = (events ?? []).filter((event) => event.status === "PUBLISHED");
    const activeActivities = (activities ?? []).filter((activity) => activity.active);

    /*
     * The next date, at or after today, that has at least one live slot. It is
     * offered to the user when the date being edited has no bookable slot
     * (e.g. today is a closed or empty day), instead of showing a bare error.
     */
    const today = toIsoDate(new Date());
    const nextAvailableDate =
        (timeSlots ?? [])
            .filter((slot) => slot.status !== "CANCELLED" && slot.date >= today)
            .sort((a, b) => a.date.localeCompare(b.date))[0]?.date ?? null;

    /*
     * The active mutation depends on the mode, so the form's pending and error
     * state follows the target the user picked.
     */
    const submitMutation =
        mode === "EVENT"
            ? eventRegisterMutation
            : mode === "ACTIVITY"
              ? createBookingMutation
              : registerMutation;
    const formMutation = editing ? updateVisitorMutation : submitMutation;

    /*
     * Cancelled registrations are hidden by default: the API has no hard
     * delete, so hiding them keeps the queue focused on the live rows.
     */
    const visibleRegistrations = (registrations ?? []).filter(
        (registration) => showCancelled || registration.status !== "CANCELLED",
    );

    function resetMutations() {
        registerMutation.reset();
        eventRegisterMutation.reset();
        createBookingMutation.reset();
        updateVisitorMutation.reset();
    }

    function resetForm() {
        setEditing(null);
        setFormOpen(false);
        setFormKey((current) => current + 1);
        setPreselectedEvent(null);
        resetMutations();
        if (preselectedEventId) {
            navigate("/visitormanagement/registration", { replace: true });
        }
    }

    function openCreate() {
        resetMutations();
        setEditing(null);
        setPreselectedEvent(null);
        setFormOpen(true);
        setFormKey((current) => current + 1);
    }

    function changeMode(nextMode) {
        resetMutations();
        setMode(nextMode);
    }

    function startEdit(registration) {
        resetMutations();
        setEditing(registration);
        setFormOpen(true);
        setFormKey((current) => current + 1);
    }

    function handleSubmit(values) {
        if (editing) {
            updateVisitorMutation.mutate(
                { id: editing.visitorId, visitor: values.visitor },
                { onSuccess: resetForm },
            );
            return;
        }
        if (values.booking) {
            createBookingMutation.mutate(values.booking, { onSuccess: resetForm });
            return;
        }
        if (values.eventId) {
            eventRegisterMutation.mutate(values, { onSuccess: resetForm });
            return;
        }
        registerMutation.mutate(values, { onSuccess: resetForm });
    }

    function handleAction(registration, action) {
        actionMutation.mutate({ id: registration.id, action });
    }

    const pendingAction = actionMutation.isPending ? actionMutation.variables : null;
    const actionError = actionMutation.isError
        ? { id: actionMutation.variables?.id, message: actionMutation.error.message }
        : null;

    return (
        <div className="min-h-full bg-[#F5F7FA] px-8 py-7">
            <section className="rounded-lg border border-line bg-white p-7 animate-fade-up">
                <h2 className="font-heading mb-5 inline-block border-b-[3px] border-accent pb-2 text-2xl font-bold text-primary-dark">
                    Visitor registration
                </h2>

                <div className="mb-4 flex flex-wrap items-center justify-end gap-4">
                    <label className="flex items-center gap-2 text-xs text-ink">
                        <input
                            type="checkbox"
                            checked={showCancelled}
                            onChange={(event) => setShowCancelled(event.target.checked)}
                            className="h-4 w-4 accent-[#0a8276]"
                        />
                        Show cancelled
                    </label>
                    <button
                        type="button"
                        onClick={openCreate}
                        className="rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark"
                    >
                        + New registration
                    </button>
                </div>

                {visitorsError && (
                    <p className="mt-3 text-xs text-muted">
                        Visitor details (language, type) could not be loaded — the queue still
                        works, but editing a visitor is unavailable.
                    </p>
                )}

                <h3 className="font-heading mt-6.5 mb-3 text-[17px] font-bold text-ink">
                    Registered visitors
                </h3>

                {isPending && (
                    <div className="flex items-center justify-center gap-3 rounded-lg border border-line bg-white px-6 py-16 text-sm text-muted">
                        <Loader2 size={18} className="animate-spin" />
                        Loading registrations…
                    </div>
                )}

                {isError && (
                    <div className="flex flex-col items-center gap-4 rounded-lg border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <TriangleAlert size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-ink">
                                Registrations could not be loaded
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
                        <RegistrationsTable
                            registrations={visibleRegistrations}
                            visitorsById={visitorsById}
                            slotsById={slotsById}
                            eventsById={eventsById}
                            pendingAction={pendingAction}
                            actionError={actionError}
                            onAction={handleAction}
                            onEdit={startEdit}
                        />
                        <p className="mt-6 text-[11px] text-muted">
                            The registration is the single source of truth: approve / reject /
                            check-in / edit per visitor. A confirmed registration triggers the
                            safety briefing.
                        </p>
                    </>
                )}

                <ProspectsSection />

                {formOpen && (
                    <Modal
                        title={
                            editing
                                ? `Edit visitor — ${
                                      visitorsById.get(editing.visitorId)?.fullName ?? ""
                                  }`
                                : "New registration"
                        }
                        onClose={resetForm}
                        widthClass="max-w-3xl"
                    >
                        <RegistrationForm
                            key={formKey}
                            mode={mode}
                            onModeChange={changeMode}
                            date={date}
                            onDateChange={setDate}
                            availability={availability ?? []}
                            availabilityLoading={availabilityLoading}
                            availabilityError={availabilityError}
                            onRetryAvailability={refetchAvailability}
                            nextAvailableDate={nextAvailableDate}
                            onUseNextDate={(nextDate) => setDate(nextDate)}
                            events={publishedEvents}
                            activities={activeActivities}
                            visitors={visitors ?? []}
                            preselectedEventId={preselectedEvent}
                            editingVisitor={
                                editing ? visitorsById.get(editing.visitorId) : null
                            }
                            isSubmitting={formMutation.isPending}
                            submitError={formMutation.error?.message}
                            serverFieldErrors={formMutation.error?.data?.fieldErrors}
                            onSubmit={handleSubmit}
                            onCancel={resetForm}
                        />
                    </Modal>
                )}
            </section>
        </div>
    );
}