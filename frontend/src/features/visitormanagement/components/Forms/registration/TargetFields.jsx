import { useTranslation } from "react-i18next";
import { Field, Input, Select } from "../../../../../shared/ui";
import { useVisitorFormat } from "../../../utils/format";
import { useLabels } from "../../../utils/labels";
import { PAYMENT_METHODS } from "./rules";

const LINK_BUTTON = "font-semibold text-text-accent underline hover:text-primary-pressed";

/**
 * Target of a new registration: the published event, or the activity, the visit date and the
 * time slot, and the payment method of an activity booking.
 */
export default function TargetFields({
    mode,
    needsSlot,
    events,
    activities,
    eventId,
    onEventChange,
    activityId,
    onActivityChange,
    date,
    onDateChange,
    availability,
    availabilityLoading,
    availabilityError,
    onRetryAvailability,
    nextAvailableDate,
    onUseNextDate,
    bookableSlots,
    timeSlotId,
    onTimeSlotChange,
    groupSize,
    validGroupSize,
    paymentMethod,
    onPaymentMethodChange,
    fieldError,
}) {
    const { t } = useTranslation("visitormanagement");
    const format = useVisitorFormat();
    const labels = useLabels();
    const slots = availability ?? [];
    const emailError = fieldError("email");
    const noSlotFits = !availabilityLoading && !availabilityError && slots.length > 0 && bookableSlots.length === 0;

    return (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {mode === "EVENT" && (
                <Field id="reg-event" label={t("registration.fields.event")} error={fieldError("eventId")}>
                    <Select value={eventId} onChange={(event) => onEventChange(event.target.value)} disabled={events.length === 0}>
                        {events.length === 0 && <option value="">{t("registration.options.noEvent")}</option>}
                        {events.map((event) => (
                            <option key={event.id} value={event.id}>
                                {t("registration.options.event", {
                                    title: event.title,
                                    booked: event.booked,
                                    capacity: event.maxCapacity,
                                })}
                            </option>
                        ))}
                    </Select>
                </Field>
            )}

            {mode === "ACTIVITY" && (
                <Field
                    id="reg-activity"
                    label={t("registration.fields.activity")}
                    error={fieldError("activityId")}
                >
                    <Select
                        value={activityId}
                        onChange={(event) => onActivityChange(event.target.value)}
                        disabled={activities.length === 0}
                    >
                        {activities.length === 0 && <option value="">{t("registration.options.noActivity")}</option>}
                        {activities.map((activity) => (
                            <option key={activity.id} value={activity.id}>
                                {t("registration.options.activity", { name: activity.name, price: activity.price })}
                            </option>
                        ))}
                    </Select>
                    {emailError && (
                        <p role="alert" className="mt-1 text-xs text-error-fg">
                            {emailError}
                        </p>
                    )}
                </Field>
            )}

            {needsSlot && (
                <>
                    <Field id="reg-date" label={t("registration.fields.date")}>
                        <Input type="date" value={date} onChange={(event) => onDateChange(event.target.value)} />
                    </Field>
                    <Field id="reg-slot" label={t("registration.fields.slot")} error={fieldError("timeSlotId")}>
                        <Select
                            value={timeSlotId}
                            onChange={(event) => onTimeSlotChange(event.target.value)}
                            disabled={availabilityLoading || Boolean(availabilityError)}
                        >
                            {availabilityLoading && <option value="">{t("registration.options.loadingSlots")}</option>}
                            {availabilityError && <option value="">{t("registration.options.slotsUnavailable")}</option>}
                            {!availabilityLoading && slots.length === 0 && (
                                <option value="">{t("registration.options.noSlots")}</option>
                            )}
                            {noSlotFits && (
                                <option value="">{t("registration.options.noSlotFits", { size: groupSize })}</option>
                            )}
                            {slots.map((slot) => (
                                <option
                                    key={slot.id}
                                    value={slot.id}
                                    disabled={slot.remaining < (validGroupSize ? groupSize : 1)}
                                >
                                    {t("registration.options.slot", {
                                        range: format.timeRange(slot.startTime, slot.endTime),
                                        remaining: slot.remaining,
                                    })}
                                </option>
                            ))}
                        </Select>
                    </Field>

                    {!availabilityLoading &&
                        !availabilityError &&
                        slots.length === 0 &&
                        nextAvailableDate &&
                        nextAvailableDate !== date && (
                            <p className="text-xs text-text-muted md:col-span-2">
                                {t("registration.noSlotsOnDate")}{" "}
                                <button
                                    type="button"
                                    onClick={() => onUseNextDate(nextAvailableDate)}
                                    className={LINK_BUTTON}
                                >
                                    {t("registration.useDate", { date: nextAvailableDate })}
                                </button>
                            </p>
                        )}
                    {noSlotFits && (
                        <p className="text-xs text-text-muted md:col-span-2">
                            {t("registration.slotsTooSmall", { size: groupSize })}
                        </p>
                    )}
                    {availabilityError && (
                        <p role="alert" className="text-xs text-error-fg md:col-span-2">
                            {t("registration.slotsLoadError")}{" "}
                            <button type="button" onClick={onRetryAvailability} className="underline">
                                {t("registration.retry")}
                            </button>
                        </p>
                    )}
                </>
            )}

            {mode === "ACTIVITY" && (
                <Field id="reg-payment" label={t("registration.fields.paymentMethod")}>
                    <Select
                        value={paymentMethod}
                        onChange={(event) => onPaymentMethodChange(event.target.value)}
                        options={PAYMENT_METHODS.map((method) => ({ value: method, label: labels.enum(method) }))}
                    />
                </Field>
            )}
        </div>
    );
}
