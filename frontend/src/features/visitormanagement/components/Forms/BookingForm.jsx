import { useState } from "react";
import { formatTimeRange, toIsoDate } from "../../utils/format";
import { formatEnumLabel } from "../../../../shared/utils/formatEnumLabel";
import { useAvailability } from "../../hooks/useRegistrations";

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

const PAYMENT_METHODS = ["CASH", "ORANGE_MONEY", "MOOV_MONEY"];
const EMAIL_PATTERN = /^\S+@\S+\.\S+$/;
const PHONE_PATTERN = /^\+?[0-9][0-9()\-. ]{5,28}$/;

/**
 * Booking form, used to create a booking and to edit an existing one. Both
 * send the same BookingRequest payload.
 *
 * Capacity filtering differs by mode: when creating, slots that cannot seat
 * the group are removed; when editing, they are kept, because the API excludes
 * the booking's own people from the occupancy before checking, so a full slot
 * can still accept the unchanged booking.
 */
export default function BookingForm({
    booking,
    activities,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const isEditing = Boolean(booking);

    const [activityId, setActivityId] = useState(booking ? String(booking.activityId) : "");
    const [date, setDate] = useState(booking?.slotDate ?? toIsoDate(new Date()));
    const [timeSlotId, setTimeSlotId] = useState(booking ? String(booking.timeSlotId) : "");
    const [visitorFullName, setVisitorFullName] = useState(booking?.visitorFullName ?? "");
    const [visitorEmail, setVisitorEmail] = useState(booking?.visitorEmail ?? "");
    const [visitorPhone, setVisitorPhone] = useState(booking?.visitorPhone ?? "");
    const [peopleCount, setPeopleCount] = useState(booking ? String(booking.peopleCount) : "1");
    const [paymentMethod, setPaymentMethod] = useState(booking?.paymentMethod ?? "CASH");
    const [fieldErrors, setFieldErrors] = useState({});

    const {
        data: availability,
        isPending: availabilityLoading,
        isError: availabilityError,
        refetch: refetchAvailability,
    } = useAvailability(date);

    const peopleNumber = Number(peopleCount);
    const validPeople = Number.isInteger(peopleNumber) && peopleNumber >= 1;

    // The first active activity is proposed by default on a new booking.
    const firstActiveActivity = activities.find((activity) => activity.active);
    const effectiveActivityId =
        activityId || (firstActiveActivity ? String(firstActiveActivity.id) : "");

    const allSlots = availability ?? [];
    const slots = isEditing
        ? allSlots
        : allSlots.filter((slot) => slot.remaining >= (validPeople ? peopleNumber : 1));
    const selectionAvailable = slots.some((slot) => String(slot.id) === timeSlotId);
    const effectiveTimeSlotId = selectionAvailable
        ? timeSlotId
        : slots.length > 0
          ? String(slots[0].id)
          : "";

    function validate() {
        const errors = {};
        if (!effectiveActivityId) errors.activityId = "Pick an activity.";
        if (!date) errors.date = "Pick a visit date.";
        if (!effectiveTimeSlotId) errors.timeSlotId = "Pick a time slot.";
        if (!visitorFullName.trim()) {
            errors.visitorFullName = "Visitor name is required.";
        } else if (visitorFullName.trim().length > 150) {
            errors.visitorFullName = "Name must be at most 150 characters.";
        }
        if (!visitorEmail.trim() || !EMAIL_PATTERN.test(visitorEmail.trim())) {
            errors.visitorEmail = "Enter a valid email address.";
        } else if (visitorEmail.trim().length > 200) {
            errors.visitorEmail = "Email must be at most 200 characters.";
        }
        if (visitorPhone.trim() && !PHONE_PATTERN.test(visitorPhone.trim())) {
            errors.visitorPhone = "Enter a valid phone number.";
        }
        if (!validPeople) {
            errors.peopleCount = "People count must be a whole number of at least 1.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            activityId: Number(effectiveActivityId),
            timeSlotId: Number(effectiveTimeSlotId),
            visitorFullName: visitorFullName.trim(),
            visitorEmail: visitorEmail.trim(),
            visitorPhone: visitorPhone.trim() || null,
            peopleCount: peopleNumber,
            paymentMethod,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form
            onSubmit={handleSubmit}
           
        >
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                <div>
                    <label htmlFor="bk-activity" className={LABEL_CLASS}>
                        Activity
                    </label>
                    <select
                        id="bk-activity"
                        value={effectiveActivityId}
                        onChange={(event) => setActivityId(event.target.value)}
                        className={INPUT_CLASS}
                    >
                        {activities.map((activity) => (
                            <option
                                key={activity.id}
                                value={activity.id}
                                disabled={
                                    !activity.active &&
                                    String(activity.id) !== effectiveActivityId
                                }
                            >
                                {activity.name}
                                {!activity.active ? " (inactive)" : ""}
                            </option>
                        ))}
                    </select>
                    {fieldError("activityId") && (
                        <p className={ERROR_CLASS}>{fieldError("activityId")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="bk-date" className={LABEL_CLASS}>
                        Visit date
                    </label>
                    <input
                        id="bk-date"
                        type="date"
                        value={date}
                        onChange={(event) => setDate(event.target.value)}
                        className={INPUT_CLASS}
                    />
                    {fieldError("date") && <p className={ERROR_CLASS}>{fieldError("date")}</p>}
                </div>

                <div>
                    <label htmlFor="bk-slot" className={LABEL_CLASS}>
                        Time slot
                    </label>
                    <select
                        id="bk-slot"
                        value={effectiveTimeSlotId}
                        onChange={(event) => setTimeSlotId(event.target.value)}
                        disabled={availabilityLoading || Boolean(availabilityError)}
                        className={`${INPUT_CLASS} disabled:opacity-60`}
                    >
                        {availabilityLoading && <option value="">Loading slots…</option>}
                        {!availabilityLoading && slots.length === 0 && (
                            <option value="">No slots on this date</option>
                        )}
                        {slots.map((slot) => (
                            <option key={slot.id} value={slot.id}>
                                {formatTimeRange(slot.startTime, slot.endTime)} · {slot.remaining} left
                            </option>
                        ))}
                    </select>
                    {fieldError("timeSlotId") && (
                        <p className={ERROR_CLASS}>{fieldError("timeSlotId")}</p>
                    )}
                    {availabilityError && (
                        <p className={ERROR_CLASS}>
                            Slots could not be loaded.{" "}
                            <button type="button" onClick={() => refetchAvailability()} className="underline">
                                Retry
                            </button>
                        </p>
                    )}
                </div>

                <div>
                    <label htmlFor="bk-name" className={LABEL_CLASS}>
                        Visitor full name
                    </label>
                    <input
                        id="bk-name"
                        type="text"
                        value={visitorFullName}
                        onChange={(event) => setVisitorFullName(event.target.value)}
                        className={INPUT_CLASS}
                    />
                    {fieldError("visitorFullName") && (
                        <p className={ERROR_CLASS}>{fieldError("visitorFullName")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="bk-email" className={LABEL_CLASS}>
                        Visitor email
                    </label>
                    <input
                        id="bk-email"
                        type="email"
                        value={visitorEmail}
                        onChange={(event) => setVisitorEmail(event.target.value)}
                        className={INPUT_CLASS}
                    />
                    {fieldError("visitorEmail") && (
                        <p className={ERROR_CLASS}>{fieldError("visitorEmail")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="bk-phone" className={LABEL_CLASS}>
                        Visitor phone
                    </label>
                    <input
                        id="bk-phone"
                        type="tel"
                        value={visitorPhone}
                        onChange={(event) => setVisitorPhone(event.target.value)}
                        className={INPUT_CLASS}
                    />
                    {fieldError("visitorPhone") && (
                        <p className={ERROR_CLASS}>{fieldError("visitorPhone")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="bk-people" className={LABEL_CLASS}>
                        People
                    </label>
                    <input
                        id="bk-people"
                        type="number"
                        min="1"
                        value={peopleCount}
                        onChange={(event) => setPeopleCount(event.target.value)}
                        className={INPUT_CLASS}
                    />
                    {fieldError("peopleCount") && (
                        <p className={ERROR_CLASS}>{fieldError("peopleCount")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="bk-payment" className={LABEL_CLASS}>
                        Payment method
                    </label>
                    <select
                        id="bk-payment"
                        value={paymentMethod}
                        onChange={(event) => setPaymentMethod(event.target.value)}
                        className={INPUT_CLASS}
                    >
                        {PAYMENT_METHODS.map((method) => (
                            <option key={method} value={method}>
                                {formatEnumLabel(method)}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {submitError && (
                <p className="mt-4 rounded-md border border-error/30 bg-error/5 px-3 py-2 text-sm text-error">
                    {submitError}
                </p>
            )}

            <div className="mt-4 flex gap-2.5">
                <button
                    type="submit"
                    disabled={isSubmitting}
                    className="rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {isSubmitting
                        ? "Saving…"
                        : isEditing
                          ? "Save booking"
                          : "Create booking"}
                </button>
                <button
                    type="button"
                    onClick={onCancel}
                    disabled={isSubmitting}
                    className="rounded-md border border-line bg-white px-4 py-2.5 text-xs text-primary hover:bg-[#F2FBF9] disabled:opacity-60"
                >
                    Cancel
                </button>
            </div>
        </form>
    );
}