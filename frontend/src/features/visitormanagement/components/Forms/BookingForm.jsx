import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Select } from "../../../../shared/ui";
import { toIsoDate, useVisitorFormat } from "../../utils/format";
import { useLabels } from "../../utils/labels";
import { useAvailability } from "../../hooks/useRegistrations";
import FormActions from "../FormActions";
import FormAlert from "../FormAlert";

const PAYMENT_METHODS = ["CASH", "ORANGE_MONEY", "MOOV_MONEY"];
const EMAIL_PATTERN = /^\S+@\S+\.\S+$/;
const PHONE_PATTERN = /^\+?[0-9][0-9()\-. ]{5,28}$/;

/**
 * Booking form, used to create a booking and to edit an existing one. Both send the same
 * BookingRequest payload.
 *
 * Capacity filtering differs by mode: when creating, slots that cannot seat the group are
 * removed; when editing, they are kept, because the API excludes the booking's own people from
 * the occupancy before checking, so a full slot can still accept the unchanged booking.
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
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const labels = useLabels();
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
    const effectiveActivityId = activityId || (firstActiveActivity ? String(firstActiveActivity.id) : "");

    const allSlots = availability ?? [];
    const slots = isEditing
        ? allSlots
        : allSlots.filter((slot) => slot.remaining >= (validPeople ? peopleNumber : 1));
    const selectionAvailable = slots.some((slot) => String(slot.id) === timeSlotId);
    const effectiveTimeSlotId = selectionAvailable ? timeSlotId : slots.length > 0 ? String(slots[0].id) : "";

    function validate() {
        const errors = {};
        if (!effectiveActivityId) errors.activityId = t("booking.form.errors.activity");
        if (!date) errors.date = t("booking.form.errors.date");
        if (!effectiveTimeSlotId) errors.timeSlotId = t("booking.form.errors.slot");
        if (!visitorFullName.trim()) {
            errors.visitorFullName = t("booking.form.errors.nameRequired");
        } else if (visitorFullName.trim().length > 150) {
            errors.visitorFullName = t("booking.form.errors.nameTooLong");
        }
        if (!visitorEmail.trim() || !EMAIL_PATTERN.test(visitorEmail.trim())) {
            errors.visitorEmail = t("booking.form.errors.emailInvalid");
        } else if (visitorEmail.trim().length > 200) {
            errors.visitorEmail = t("booking.form.errors.emailTooLong");
        }
        if (visitorPhone.trim() && !PHONE_PATTERN.test(visitorPhone.trim())) {
            errors.visitorPhone = t("booking.form.errors.phoneInvalid");
        }
        if (!validPeople) {
            errors.peopleCount = t("booking.form.errors.people");
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
        <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Field id="bk-activity" label={t("booking.form.activity")} error={fieldError("activityId")}>
                    <Select value={effectiveActivityId} onChange={(event) => setActivityId(event.target.value)}>
                        {activities.map((activity) => (
                            <option
                                key={activity.id}
                                value={activity.id}
                                disabled={!activity.active && String(activity.id) !== effectiveActivityId}
                            >
                                {activity.active
                                    ? activity.name
                                    : t("booking.form.inactiveActivity", { name: activity.name })}
                            </option>
                        ))}
                    </Select>
                </Field>

                <Field id="bk-date" label={t("booking.form.date")} error={fieldError("date")}>
                    <Input type="date" value={date} onChange={(event) => setDate(event.target.value)} />
                </Field>

                <Field
                    id="bk-slot"
                    label={t("booking.form.slot")}
                    error={
                        fieldError("timeSlotId") ??
                        (availabilityError ? (
                            <>
                                {t("booking.form.slotsError")}{" "}
                                <button type="button" onClick={() => refetchAvailability()} className="underline">
                                    {t("booking.form.retry")}
                                </button>
                            </>
                        ) : undefined)
                    }
                >
                    <Select
                        value={effectiveTimeSlotId}
                        onChange={(event) => setTimeSlotId(event.target.value)}
                        disabled={availabilityLoading || Boolean(availabilityError)}
                    >
                        {availabilityLoading && <option value="">{t("booking.form.slotsLoading")}</option>}
                        {!availabilityLoading && slots.length === 0 && (
                            <option value="">{t("booking.form.noSlots")}</option>
                        )}
                        {slots.map((slot) => (
                            <option key={slot.id} value={slot.id}>
                                {t("booking.form.slotOption", {
                                    time: format.timeRange(slot.startTime, slot.endTime),
                                    remaining: slot.remaining,
                                })}
                            </option>
                        ))}
                    </Select>
                </Field>

                <Field id="bk-name" label={t("booking.form.name")} error={fieldError("visitorFullName")}>
                    <Input type="text" value={visitorFullName} onChange={(event) => setVisitorFullName(event.target.value)} />
                </Field>

                <Field id="bk-email" label={t("booking.form.email")} error={fieldError("visitorEmail")}>
                    <Input type="email" value={visitorEmail} onChange={(event) => setVisitorEmail(event.target.value)} />
                </Field>

                <Field id="bk-phone" label={t("booking.form.phone")} error={fieldError("visitorPhone")}>
                    <Input type="tel" value={visitorPhone} onChange={(event) => setVisitorPhone(event.target.value)} />
                </Field>

                <Field id="bk-people" label={t("booking.form.people")} error={fieldError("peopleCount")}>
                    <Input type="number" min="1" value={peopleCount} onChange={(event) => setPeopleCount(event.target.value)} />
                </Field>

                <Field id="bk-payment" label={t("booking.form.paymentMethod")}>
                    <Select
                        value={paymentMethod}
                        onChange={(event) => setPaymentMethod(event.target.value)}
                        options={PAYMENT_METHODS.map((method) => ({ value: method, label: labels.enum(method) }))}
                    />
                </Field>
            </div>

            {submitError && <FormAlert className="mt-4">{submitError}</FormAlert>}

            <FormActions
                onCancel={onCancel}
                isSubmitting={isSubmitting}
                submitLabel={isEditing ? t("booking.form.save") : t("booking.form.create")}
            />
        </form>
    );
}
