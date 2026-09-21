import { Fragment, useEffect, useState } from "react";
import { Loader2, TriangleAlert } from "lucide-react";
import {
    useActivities,
    useBookingAction,
    useBookings,
    useCreateActivity,
    useCreateBooking,
    useDeactivateActivity,
    useOccupancy,
    useUpdateActivity,
    useUpdateBooking,
} from "../../hooks/useBooking";
import ActivitiesTable from "../ActivitiesTable";
import ActivityForm from "../Forms/ActivityForm";
import BookingForm from "../Forms/BookingForm";
import BookingsTable from "../BookingsTable";
import OccupancyBars from "../OccupancyBars";
import { formatEnumLabel } from "../../../../shared/utils/formatEnumLabel";

/*
 * The four steps of the booking journey, exactly as the mockup's flow strip
 * describes them. They are explanatory, not interactive: the mockup does not
 * define a booking creation form on this screen.
 */
const FLOW_STEPS = [
    "Choose time slot & tour type",
    "Registration form",
    "Email confirmation",
    "Reminder 24h before",
];

export default function BookingPage() {
    const [activityFormState, setActivityFormState] = useState({ open: false, activity: null });
    const [activityFormKey, setActivityFormKey] = useState(0);
    const [createFormOpen, setCreateFormOpen] = useState(false);
    const [editing, setEditing] = useState(null);
    const [formKey, setFormKey] = useState(0);
    const [filters, setFilters] = useState({ status: "", activityId: "", date: "" });

    /*
     * The API applies only the first non-null filter, so the controls clear one
     * another: choosing a status drops the activity and the date, and so on.
     */
    function updateFilter(key, value) {
        setFilters({
            status: key === "status" ? value : "",
            activityId: key === "activityId" ? value : "",
            date: key === "date" ? value : "",
        });
    }

    const hasActiveFilter = Boolean(filters.status || filters.activityId || filters.date);

    useEffect(() => {
        document.title = "Agritourism Booking System — Visitor Management";
    }, []);

    const {
        data: bookings,
        isPending: bookingsPending,
        isError: bookingsError,
        refetch: refetchBookings,
        isFetching: bookingsFetching,
    } = useBookings(filters);
    const { data: activities, isError: activitiesError } = useActivities();
    const {
        data: occupancy,
        isPending: occupancyPending,
        isError: occupancyError,
        refetch: refetchOccupancy,
        isFetching: occupancyFetching,
    } = useOccupancy();

    const createActivityMutation = useCreateActivity();
    const updateActivityMutation = useUpdateActivity();
    const deactivateActivityMutation = useDeactivateActivity();
    const createBookingMutation = useCreateBooking();
    const updateBookingMutation = useUpdateBooking();
    const actionMutation = useBookingAction();

    const editingActivity = activityFormState.activity;
    const activityFormMutation = editingActivity ? updateActivityMutation : createActivityMutation;

    function resetActivityForm() {
        setActivityFormState({ open: false, activity: null });
        setActivityFormKey((current) => current + 1);
        createActivityMutation.reset();
        updateActivityMutation.reset();
    }

    function openCreateActivity() {
        createActivityMutation.reset();
        updateActivityMutation.reset();
        setActivityFormState({ open: true, activity: null });
        setActivityFormKey((current) => current + 1);
    }

    function openEditActivity(activity) {
        createActivityMutation.reset();
        updateActivityMutation.reset();
        setActivityFormState({ open: true, activity });
        setActivityFormKey((current) => current + 1);
    }

    function handleActivitySubmit(values) {
        if (editingActivity) {
            updateActivityMutation.mutate(
                { id: editingActivity.id, data: values },
                { onSuccess: resetActivityForm },
            );
        } else {
            createActivityMutation.mutate(values, { onSuccess: resetActivityForm });
        }
    }

    const pendingDeactivateId = deactivateActivityMutation.isPending
        ? deactivateActivityMutation.variables
        : null;
    const deactivateActivityError = deactivateActivityMutation.isError
        ? {
              id: deactivateActivityMutation.variables,
              message: deactivateActivityMutation.error.message,
          }
        : null;

    function resetEdit() {
        setEditing(null);
        setFormKey((current) => current + 1);
        updateBookingMutation.reset();
    }

    function startEdit(booking) {
        updateBookingMutation.reset();
        createBookingMutation.reset();
        setCreateFormOpen(false);
        setEditing(booking);
        setFormKey((current) => current + 1);
    }

    function openCreateBooking() {
        createBookingMutation.reset();
        updateBookingMutation.reset();
        setEditing(null);
        setCreateFormOpen(true);
        setFormKey((current) => current + 1);
    }

    function resetCreateBooking() {
        setCreateFormOpen(false);
        setFormKey((current) => current + 1);
        createBookingMutation.reset();
    }

    const pendingAction = actionMutation.isPending ? actionMutation.variables : null;
    const actionError = actionMutation.isError
        ? { id: actionMutation.variables?.id, message: actionMutation.error.message }
        : null;

    return (
        <div className="min-h-full bg-[#F5F7FA] px-8 py-7">
            <section className="rounded-lg border border-line bg-white p-7">
                <h2 className="font-heading mb-5 inline-block border-b-[3px] border-accent pb-2 text-2xl font-bold text-primary-dark">
                    Agritourism booking system
                </h2>

                <div className="flex flex-col gap-2 sm:flex-row sm:items-stretch sm:gap-0">
                    {FLOW_STEPS.map((step, index) => (
                        <Fragment key={step}>
                            {index > 0 && (
                                <span className="hidden self-center px-2.5 text-lg text-accent sm:block">
                                    →
                                </span>
                            )}
                            <div className="border border-line border-t-[3px] border-t-primary bg-[#F7FDFB] px-4 py-3.5 text-[13px] sm:min-w-[140px]">
                                <div className="mb-1 text-[11px] text-primary">Step {index + 1}</div>
                                {step}
                            </div>
                        </Fragment>
                    ))}
                </div>

                <p className="mt-6 text-[11px] text-muted">
                    Each activity carries its own price and capacity; payment is accepted in cash or
                    mobile money. A booking must be paid before it can be confirmed.
                </p>

                <h3 className="font-heading mt-6.5 mb-3 text-[17px] font-bold text-ink">Bookings</h3>

                <div className="mb-4 flex flex-wrap gap-2.5">
                    <button
                        type="button"
                        onClick={openCreateBooking}
                        className="rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark"
                    >
                        + New booking
                    </button>
                </div>

                <div className="mb-4 flex flex-wrap items-end gap-3.5">
                    <div>
                        <label
                            htmlFor="bk-filter-status"
                            className="mb-1 block text-xs tracking-wide text-primary uppercase"
                        >
                            Status
                        </label>
                        <select
                            id="bk-filter-status"
                            value={filters.status}
                            onChange={(event) => updateFilter("status", event.target.value)}
                            className="min-w-[150px] rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                        >
                            <option value="">All statuses</option>
                            {["PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"].map((status) => (
                                <option key={status} value={status}>
                                    {formatEnumLabel(status)}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label
                            htmlFor="bk-filter-activity"
                            className="mb-1 block text-xs tracking-wide text-primary uppercase"
                        >
                            Activity
                        </label>
                        <select
                            id="bk-filter-activity"
                            value={filters.activityId}
                            onChange={(event) => updateFilter("activityId", event.target.value)}
                            className="min-w-[180px] rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                        >
                            <option value="">All activities</option>
                            {(activities ?? []).map((activity) => (
                                <option key={activity.id} value={activity.id}>
                                    {activity.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label
                            htmlFor="bk-filter-date"
                            className="mb-1 block text-xs tracking-wide text-primary uppercase"
                        >
                            Date
                        </label>
                        <input
                            id="bk-filter-date"
                            type="date"
                            value={filters.date}
                            onChange={(event) => updateFilter("date", event.target.value)}
                            className="rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm"
                        />
                    </div>

                    {hasActiveFilter && (
                        <button
                            type="button"
                            onClick={() => setFilters({ status: "", activityId: "", date: "" })}
                            className="rounded-md border border-line bg-white px-3.5 py-2 text-xs text-primary hover:bg-[#F2FBF9]"
                        >
                            Clear filters
                        </button>
                    )}
                </div>

                {createFormOpen && (
                    <>
                        <h4 className="font-heading mb-2 text-sm font-bold text-ink">
                            New booking
                        </h4>
                        <BookingForm
                            key={`create-${formKey}`}
                            booking={null}
                            activities={activities ?? []}
                            isSubmitting={createBookingMutation.isPending}
                            submitError={createBookingMutation.error?.message}
                            serverFieldErrors={createBookingMutation.error?.data?.fieldErrors}
                            onSubmit={(values) =>
                                createBookingMutation.mutate(values, {
                                    onSuccess: resetCreateBooking,
                                })
                            }
                            onCancel={resetCreateBooking}
                        />
                    </>
                )}

                {editing && (
                    <>
                        <h4 className="font-heading mb-2 text-sm font-bold text-ink">
                            Edit booking {editing.reference}
                        </h4>
                        <BookingForm
                            key={formKey}
                            booking={editing}
                            activities={activities ?? []}
                            isSubmitting={updateBookingMutation.isPending}
                            submitError={updateBookingMutation.error?.message}
                            serverFieldErrors={updateBookingMutation.error?.data?.fieldErrors}
                            onSubmit={(values) =>
                                updateBookingMutation.mutate(
                                    { id: editing.id, data: values },
                                    { onSuccess: resetEdit },
                                )
                            }
                            onCancel={resetEdit}
                        />
                    </>
                )}

                {activitiesError && (
                    <p className="mb-3 text-xs text-muted">
                        Activities could not be loaded — editing a booking is unavailable until the
                        list returns.
                    </p>
                )}

                {bookingsPending && (
                    <div className="flex items-center justify-center gap-3 rounded-lg border border-line bg-white px-6 py-16 text-sm text-muted">
                        <Loader2 size={18} className="animate-spin" />
                        Loading bookings…
                    </div>
                )}

                {bookingsError && (
                    <div className="flex flex-col items-center gap-4 rounded-lg border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <TriangleAlert size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-ink">
                                Bookings could not be loaded
                            </p>
                            <p className="mt-1 text-sm text-muted">
                                The request to the server did not succeed.
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={() => refetchBookings()}
                            disabled={bookingsFetching}
                            className="rounded-md bg-primary px-4 py-2 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:opacity-60"
                        >
                            {bookingsFetching ? "Retrying…" : "Retry"}
                        </button>
                    </div>
                )}

                {!bookingsPending && !bookingsError && (
                    <BookingsTable
                        bookings={bookings ?? []}
                        pendingAction={pendingAction}
                        actionError={actionError}
                        onAction={(booking, action) =>
                            actionMutation.mutate({ id: booking.id, action })
                        }
                        onEdit={startEdit}
                    />
                )}

                <h3 className="font-heading mt-6.5 mb-3 text-[17px] font-bold text-ink">
                    Activities
                </h3>

                <div className="mb-4 flex flex-wrap gap-2.5">
                    <button
                        type="button"
                        onClick={openCreateActivity}
                        className="rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark"
                    >
                        + New activity
                    </button>
                </div>

                {activityFormState.open && (
                    <>
                        {editingActivity && (
                            <h4 className="font-heading mb-2 text-sm font-bold text-ink">
                                Edit activity: {editingActivity.name}
                            </h4>
                        )}
                        <ActivityForm
                            key={activityFormKey}
                            activity={editingActivity}
                            isSubmitting={activityFormMutation.isPending}
                            submitError={activityFormMutation.error?.message}
                            serverFieldErrors={activityFormMutation.error?.data?.fieldErrors}
                            onSubmit={handleActivitySubmit}
                            onCancel={resetActivityForm}
                        />
                    </>
                )}

                {activitiesError ? (
                    <p className="text-sm text-muted">Activities could not be loaded.</p>
                ) : (
                    <>
                        <ActivitiesTable
                            activities={activities ?? []}
                            pendingDeactivateId={pendingDeactivateId}
                            deactivateError={deactivateActivityError}
                            onDeactivate={(activity) =>
                                deactivateActivityMutation.mutate(activity.id)
                            }
                            onEdit={openEditActivity}
                        />
                        <p className="mt-3 text-[11px] text-muted">
                            Deactivating an activity cancels all its bookings and cannot be undone.
                        </p>
                    </>
                )}

                <h3 className="font-heading mt-6.5 mb-3 text-[17px] font-bold text-ink">
                    Occupancy per activity
                </h3>

                {occupancyPending && (
                    <div className="flex items-center justify-center gap-3 rounded-lg border border-line bg-white px-6 py-16 text-sm text-muted">
                        <Loader2 size={18} className="animate-spin" />
                        Loading occupancy…
                    </div>
                )}

                {occupancyError && (
                    <div className="flex flex-col items-center gap-4 rounded-lg border border-error/30 bg-error/5 px-6 py-16 text-center">
                        <TriangleAlert size={28} className="text-error" />
                        <div>
                            <p className="font-heading text-base font-bold text-ink">
                                Occupancy could not be loaded
                            </p>
                            <p className="mt-1 text-sm text-muted">
                                The request to the server did not succeed.
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={() => refetchOccupancy()}
                            disabled={occupancyFetching}
                            className="rounded-md bg-primary px-4 py-2 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:opacity-60"
                        >
                            {occupancyFetching ? "Retrying…" : "Retry"}
                        </button>
                    </div>
                )}

                {!occupancyPending && !occupancyError && (
                    <>
                        <OccupancyBars occupancy={occupancy ?? []} />
                        <p className="mt-6 text-[11px] text-muted">
                            Confirmations are linked to the scheduling module, so a booking can never
                            exceed the slot capacity. Cancelling a booking releases its seats and
                            refunds a recorded payment.
                        </p>
                    </>
                )}
            </section>
        </div>
    );
}