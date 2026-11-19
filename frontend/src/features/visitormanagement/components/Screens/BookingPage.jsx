import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Card, Modal, PageHeader } from "../../../../shared/ui";
import { useActivities, useBookingAction, useBookings, useUpdateBooking } from "../../hooks/useBooking";
import ActivitiesSection from "../ActivitiesSection";
import BookingFilters from "../BookingFilters";
import BookingForm from "../Forms/BookingForm";
import BookingsTable from "../BookingsTable";
import { LoadError, LoadingBlock } from "../LoadState";
import OccupancySection from "../OccupancySection";

const NO_FILTERS = { status: "", activityId: "", date: "" };

export default function BookingPage() {
    const { t } = useTranslation("visitormanagement");
    const [editing, setEditing] = useState(null);
    const [formKey, setFormKey] = useState(0);
    const [filters, setFilters] = useState(NO_FILTERS);
    const [showCancelledBookings, setShowCancelledBookings] = useState(false);

    /*
     * The API applies only the first non-null filter, so the controls clear one another:
     * choosing a status drops the activity and the date, and so on.
     */
    function updateFilter(key, value) {
        setFilters({
            status: key === "status" ? value : "",
            activityId: key === "activityId" ? value : "",
            date: key === "date" ? value : "",
        });
    }

    useEffect(() => {
        document.title = t("booking.documentTitle");
    }, [t]);

    const {
        data: bookings,
        isPending: bookingsPending,
        isError: bookingsError,
        refetch: refetchBookings,
        isFetching: bookingsFetching,
    } = useBookings(filters);
    const { data: activities, isError: activitiesError } = useActivities();

    const updateBookingMutation = useUpdateBooking();
    const actionMutation = useBookingAction();

    function resetEdit() {
        setEditing(null);
        setFormKey((current) => current + 1);
        updateBookingMutation.reset();
    }

    function startEdit(booking) {
        updateBookingMutation.reset();
        setEditing(booking);
        setFormKey((current) => current + 1);
    }

    const pendingAction = actionMutation.isPending ? actionMutation.variables : null;
    const actionError = actionMutation.isError
        ? { id: actionMutation.variables?.id, message: actionMutation.error.message }
        : null;

    // Cancelled bookings stay out of view unless asked for.
    const visibleBookings = (bookings ?? []).filter(
        (booking) => showCancelledBookings || booking.status !== "CANCELLED",
    );

    return (
        <div className="space-y-6 p-6">
            <PageHeader title={t("booking.title")} />

            <Card title={t("booking.bookingsTitle")} subtitle={t("booking.bookingsIntro")}>
                <BookingFilters
                    filters={filters}
                    activities={activities ?? []}
                    onFilterChange={updateFilter}
                    onClear={() => setFilters(NO_FILTERS)}
                    showCancelled={showCancelledBookings}
                    onShowCancelledChange={setShowCancelledBookings}
                />

                {activitiesError && <p className="mb-3 text-xs text-text-muted">{t("booking.activitiesUnavailable")}</p>}

                {bookingsPending && <LoadingBlock label={t("booking.loading")} />}
                {bookingsError && (
                    <LoadError
                        title={t("booking.loadError")}
                        onRetry={() => refetchBookings()}
                        retrying={bookingsFetching}
                    />
                )}
                {!bookingsPending && !bookingsError && (
                    <BookingsTable
                        bookings={visibleBookings}
                        pendingAction={pendingAction}
                        actionError={actionError}
                        onAction={(booking, action) => actionMutation.mutate({ id: booking.id, action })}
                        onEdit={startEdit}
                    />
                )}
            </Card>

            <ActivitiesSection activities={activities} activitiesError={activitiesError} />

            <OccupancySection />

            <Modal
                open={Boolean(editing)}
                onClose={resetEdit}
                size="xl"
                dismissible={!updateBookingMutation.isPending}
                title={editing ? t("booking.form.editTitle", { reference: editing.reference }) : ""}
            >
                {editing && (
                    <BookingForm
                        key={formKey}
                        booking={editing}
                        activities={activities ?? []}
                        isSubmitting={updateBookingMutation.isPending}
                        submitError={updateBookingMutation.error?.message}
                        serverFieldErrors={updateBookingMutation.error?.data?.fieldErrors}
                        onSubmit={(values) =>
                            updateBookingMutation.mutate({ id: editing.id, data: values }, { onSuccess: resetEdit })
                        }
                        onCancel={resetEdit}
                    />
                )}
            </Modal>
        </div>
    );
}
