import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
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
import { Button, Modal, PageHeader } from "../../../../shared/ui";
import { toIsoDate } from "../../utils/format";
import { LoadError, LoadingBlock } from "../LoadState";
import RegistrationForm from "../Forms/RegistrationForm";
import RegistrationsTable from "../RegistrationsTable";
import ProspectsSection from "../ProspectsSection";

export default function RegistrationPage() {
    const { t } = useTranslation("visitormanagement");
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
        document.title = t("registration.documentTitle");
    }, [t]);

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
     * (for example today is a closed or empty day), instead of showing a bare error.
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
        <div className="p-6">
            <PageHeader
                className="mb-4"
                title={t("registration.title")}
                actions={
                    <>
                        <label className="flex items-center gap-2 text-sm text-text">
                            <input
                                type="checkbox"
                                checked={showCancelled}
                                onChange={(event) => setShowCancelled(event.target.checked)}
                                className="h-4 w-4 accent-primary"
                            />
                            {t("registration.showCancelled")}
                        </label>
                        <Button icon={Plus} onClick={openCreate}>
                            {t("registration.new")}
                        </Button>
                    </>
                }
            />

            {visitorsError && <p className="mb-3 text-xs text-text-muted">{t("registration.visitorsError")}</p>}

            <h2 className="mt-6 mb-3 text-lg font-semibold text-text">{t("registration.tableTitle")}</h2>

            {isPending && <LoadingBlock label={t("registration.loading")} />}

            {isError && (
                <LoadError title={t("registration.loadError")} onRetry={() => refetch()} retrying={isFetching} />
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
                    <p className="mt-6 text-xs text-text-muted">{t("registration.footnote")}</p>
                </>
            )}

            <ProspectsSection />

            <Modal
                open={formOpen}
                onClose={resetForm}
                size="xl"
                dismissible={!formMutation.isPending}
                title={
                    editing
                        ? t("registration.editTitle", {
                              name: visitorsById.get(editing.visitorId)?.fullName ?? "",
                          })
                        : t("registration.newTitle")
                }
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
                    editingVisitor={editing ? visitorsById.get(editing.visitorId) : null}
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
