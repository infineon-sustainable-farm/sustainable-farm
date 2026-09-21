import { useState } from "react";
import { formatTimeRange } from "../../utils/format";
import { formatEnumLabel } from "../../../../shared/utils/formatEnumLabel";

/*
 * Three targets, one entry point. The farm tour and the event are both
 * registrations in the API; the activity is a booking, so its visitor details
 * feed the booking's own contact fields rather than creating a visitor record
 * (the API has no visitor link on a booking). That difference is spelled out
 * in the form when the activity target is chosen.
 */
const MODES = [
    { value: "TOUR", label: "Farm tour" },
    { value: "EVENT", label: "Event" },
    { value: "ACTIVITY", label: "Activity booking" },
];

/*
 * The mockup's three visitor choices. The backend separates the kind of group
 * (VisitorType) from the reason for the visit (VisitPurpose), while the mockup
 * folds both into one select, so each choice resolves to a (type, purpose)
 * pair:
 *   - General public        → individual, or group when several people → tourism
 *   - Export buyer/partner  → partner (individual or group)             → purchase
 *   - School group          → school                                    → education
 * A commercial purpose (purchase) makes the backend flag the registration as a
 * prospect, which is exactly what the buyer choice should do.
 */
const VISITOR_TYPE_CHOICES = [
    { value: "GENERAL", label: "General public" },
    { value: "BUYER", label: "Export buyer / partner" },
    { value: "SCHOOL", label: "School group" },
];

const LANGUAGES = ["French", "Moore", "German", "English"];
const PAYMENT_METHODS = ["CASH", "ORANGE_MONEY", "MOOV_MONEY"];

const PHONE_PATTERN = /^\+?[0-9][0-9()\-. ]{5,28}$/;
const EMAIL_PATTERN = /^\S+@\S+\.\S+$/;

function resolveTypeAndPurpose(choice, groupSize) {
    if (choice === "SCHOOL") return { type: "SCHOOL", purpose: "EDUCATION" };
    const type = groupSize > 1 ? "GROUP" : "INDIVIDUAL";
    if (choice === "BUYER") return { type: "PARTNER", purpose: "PURCHASE" };
    return { type, purpose: "TOURISM" };
}

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

/**
 * Registration form: visitor details plus the target — a farm tour slot, an
 * event, or an activity booking. In edit mode it only edits the visitor fields;
 * the API has no endpoint to move a registration to another slot or event.
 */
export default function RegistrationForm({
    mode,
    onModeChange,
    date,
    onDateChange,
    availability,
    availabilityLoading,
    availabilityError,
    onRetryAvailability,
    events,
    activities,
    editingVisitor,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancelEdit,
}) {
    const isEditing = Boolean(editingVisitor);
    const needsSlot = mode === "TOUR" || mode === "ACTIVITY";

    const [fullName, setFullName] = useState(editingVisitor?.fullName ?? "");
    const [groupSize, setGroupSize] = useState(String(editingVisitor?.groupSize ?? 1));
    const [email, setEmail] = useState(editingVisitor?.email ?? "");
    const [phone, setPhone] = useState(editingVisitor?.phone ?? "");
    const [language, setLanguage] = useState(editingVisitor?.language ?? "French");
    const [visitorType, setVisitorType] = useState(
        editingVisitor?.type === "SCHOOL"
            ? "SCHOOL"
            : editingVisitor?.type === "PARTNER"
              ? "BUYER"
              : "GENERAL",
    );
    const [specialNeeds, setSpecialNeeds] = useState(editingVisitor?.specialNeeds ?? "");
    const [timeSlotId, setTimeSlotId] = useState("");
    const [eventId, setEventId] = useState("");
    const [activityId, setActivityId] = useState("");
    const [paymentMethod, setPaymentMethod] = useState("CASH");
    const [fieldErrors, setFieldErrors] = useState({});

    const groupSizeNumber = Number(groupSize);
    const validGroupSize = Number.isInteger(groupSizeNumber) && groupSizeNumber >= 1;

    /*
     * Slot selection, derived at render time: a selected slot that can no
     * longer seat the group is ignored and the first bookable slot is used
     * instead, so the form stays usable in one click.
     */
    const bookableSlots = (availability ?? []).filter(
        (slot) => slot.remaining >= (validGroupSize ? groupSizeNumber : 1),
    );
    const selectionStillBookable = bookableSlots.some(
        (slot) => String(slot.id) === timeSlotId,
    );
    const effectiveTimeSlotId = selectionStillBookable
        ? timeSlotId
        : bookableSlots.length > 0
          ? String(bookableSlots[0].id)
          : "";

    const effectiveEventId =
        eventId || (events.length > 0 ? String(events[0].id) : "");
    const firstActiveActivity = activities.find((activity) => activity.active);
    const effectiveActivityId =
        activityId || (firstActiveActivity ? String(firstActiveActivity.id) : "");

    function validate() {
        const errors = {};
        if (!fullName.trim()) {
            errors.fullName = "Full name is required.";
        } else if (fullName.trim().length > 150) {
            errors.fullName = "Full name must be at most 150 characters.";
        }
        if (!validGroupSize) {
            errors.groupSize = "Group size must be a whole number of at least 1.";
        }
        if (mode === "ACTIVITY" && !email.trim()) {
            errors.email = "An email address is required for an activity booking.";
        } else if (email.trim() && !EMAIL_PATTERN.test(email.trim())) {
            errors.email = "Enter a valid email address.";
        } else if (email.trim().length > 200) {
            errors.email = "Email must be at most 200 characters.";
        }
        if (phone.trim() && !PHONE_PATTERN.test(phone.trim())) {
            errors.phone = "Enter a valid phone number.";
        }
        if (specialNeeds.trim().length > 500) {
            errors.specialNeeds = "Special requirements must be at most 500 characters.";
        }
        if (needsSlot && !effectiveTimeSlotId) {
            errors.timeSlotId = "Pick a visit date with an available slot.";
        }
        if (mode === "EVENT" && !effectiveEventId) {
            errors.eventId = "Pick a published event.";
        }
        if (mode === "ACTIVITY" && !effectiveActivityId) {
            errors.activityId = "Pick an activity.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        const { type, purpose } = resolveTypeAndPurpose(visitorType, groupSizeNumber);

        if (isEditing) {
            onSubmit({
                visitor: {
                    fullName: fullName.trim(),
                    groupSize: groupSizeNumber,
                    email: email.trim() || null,
                    phone: phone.trim() || null,
                    language: language || null,
                    type,
                    specialNeeds: specialNeeds.trim() || null,
                },
            });
            return;
        }

        if (mode === "ACTIVITY") {
            onSubmit({
                booking: {
                    activityId: Number(effectiveActivityId),
                    timeSlotId: Number(effectiveTimeSlotId),
                    visitorFullName: fullName.trim(),
                    visitorEmail: email.trim(),
                    visitorPhone: phone.trim() || null,
                    peopleCount: groupSizeNumber,
                    paymentMethod,
                },
            });
            return;
        }

        const visitor = {
            fullName: fullName.trim(),
            groupSize: groupSizeNumber,
            email: email.trim() || null,
            phone: phone.trim() || null,
            language: language || null,
            type,
            specialNeeds: specialNeeds.trim() || null,
        };

        if (mode === "EVENT") {
            onSubmit({
                visitor,
                eventId: Number(effectiveEventId),
                visitPurpose: purpose,
            });
        } else {
            onSubmit({
                visitor,
                timeSlotId: Number(effectiveTimeSlotId),
                visitPurpose: purpose,
            });
        }
    }

    const fieldError = (name) => fieldErrors[name] ?? serverFieldErrors?.[name];

    return (
        <form onSubmit={handleSubmit}>
            {!isEditing && (
                <div className="mb-4 flex flex-wrap gap-2">
                    {MODES.map((choice) => (
                        <button
                            key={choice.value}
                            type="button"
                            onClick={() => onModeChange(choice.value)}
                            aria-pressed={mode === choice.value}
                            className={`rounded-md border px-4 py-2 text-xs font-semibold tracking-wide uppercase ${
                                mode === choice.value
                                    ? "border-primary bg-primary text-white"
                                    : "border-line bg-white text-primary hover:bg-[#F2FBF9]"
                            }`}
                        >
                            {choice.label}
                        </button>
                    ))}
                </div>
            )}

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                    <label htmlFor="reg-name" className={LABEL_CLASS}>
                        Full name
                    </label>
                    <input
                        id="reg-name"
                        type="text"
                        value={fullName}
                        onChange={(event) => setFullName(event.target.value)}
                        placeholder="e.g. Marie Dubois"
                        className={INPUT_CLASS}
                    />
                    {fieldError("fullName") && (
                        <p className={ERROR_CLASS}>{fieldError("fullName")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="reg-group" className={LABEL_CLASS}>
                        {mode === "ACTIVITY" ? "People" : "Group size"}
                    </label>
                    <input
                        id="reg-group"
                        type="number"
                        min="1"
                        value={groupSize}
                        onChange={(event) => setGroupSize(event.target.value)}
                        placeholder="e.g. 4"
                        className={INPUT_CLASS}
                    />
                    {fieldError("groupSize") && (
                        <p className={ERROR_CLASS}>{fieldError("groupSize")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="reg-email" className={LABEL_CLASS}>
                        Email
                    </label>
                    <input
                        id="reg-email"
                        type="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        placeholder="email@example.com"
                        className={INPUT_CLASS}
                    />
                    {fieldError("email") && <p className={ERROR_CLASS}>{fieldError("email")}</p>}
                </div>

                <div>
                    <label htmlFor="reg-phone" className={LABEL_CLASS}>
                        Phone
                    </label>
                    <input
                        id="reg-phone"
                        type="tel"
                        value={phone}
                        onChange={(event) => setPhone(event.target.value)}
                        placeholder="+226 ..."
                        className={INPUT_CLASS}
                    />
                    {fieldError("phone") && <p className={ERROR_CLASS}>{fieldError("phone")}</p>}
                </div>

                {!isEditing && mode === "EVENT" && (
                    <div>
                        <label htmlFor="reg-event" className={LABEL_CLASS}>
                            Event
                        </label>
                        <select
                            id="reg-event"
                            value={effectiveEventId}
                            onChange={(event) => setEventId(event.target.value)}
                            disabled={events.length === 0}
                            className={`${INPUT_CLASS} disabled:opacity-60`}
                        >
                            {events.length === 0 && (
                                <option value="">No published event</option>
                            )}
                            {events.map((event) => (
                                <option key={event.id} value={event.id}>
                                    {event.title} · {event.booked}/{event.maxCapacity}
                                </option>
                            ))}
                        </select>
                        {fieldError("eventId") && (
                            <p className={ERROR_CLASS}>{fieldError("eventId")}</p>
                        )}
                    </div>
                )}

                {!isEditing && mode === "ACTIVITY" && (
                    <div>
                        <label htmlFor="reg-activity" className={LABEL_CLASS}>
                            Activity
                        </label>
                        <select
                            id="reg-activity"
                            value={effectiveActivityId}
                            onChange={(event) => setActivityId(event.target.value)}
                            disabled={activities.length === 0}
                            className={`${INPUT_CLASS} disabled:opacity-60`}
                        >
                            {activities.length === 0 && <option value="">No active activity</option>}
                            {activities.map((activity) => (
                                <option key={activity.id} value={activity.id}>
                                    {activity.name} · {activity.price} FCFA
                                </option>
                            ))}
                        </select>
                        {fieldError("activityId") && (
                            <p className={ERROR_CLASS}>{fieldError("activityId")}</p>
                        )}
                    </div>
                )}

                {!isEditing && needsSlot && (
                    <>
                        <div>
                            <label htmlFor="reg-date" className={LABEL_CLASS}>
                                Visit date
                            </label>
                            <input
                                id="reg-date"
                                type="date"
                                value={date}
                                onChange={(event) => onDateChange(event.target.value)}
                                className={INPUT_CLASS}
                            />
                        </div>

                        <div>
                            <label htmlFor="reg-slot" className={LABEL_CLASS}>
                                Time slot
                            </label>
                            <select
                                id="reg-slot"
                                value={effectiveTimeSlotId}
                                onChange={(event) => setTimeSlotId(event.target.value)}
                                disabled={availabilityLoading || Boolean(availabilityError)}
                                className={`${INPUT_CLASS} disabled:opacity-60`}
                            >
                                {availabilityLoading && <option value="">Loading slots…</option>}
                                {!availabilityLoading && (availability ?? []).length === 0 && (
                                    <option value="">No slots on this date</option>
                                )}
                                {(availability ?? []).map((slot) => (
                                    <option
                                        key={slot.id}
                                        value={slot.id}
                                        disabled={
                                            slot.remaining <
                                            (validGroupSize ? groupSizeNumber : 1)
                                        }
                                    >
                                        {formatTimeRange(slot.startTime, slot.endTime)} ·{" "}
                                        {slot.remaining} left
                                    </option>
                                ))}
                            </select>
                            {fieldError("timeSlotId") && (
                                <p className={ERROR_CLASS}>{fieldError("timeSlotId")}</p>
                            )}
                            {availabilityError && (
                                <p className={ERROR_CLASS}>
                                    Slots could not be loaded.{" "}
                                    <button
                                        type="button"
                                        onClick={onRetryAvailability}
                                        className="underline"
                                    >
                                        Retry
                                    </button>
                                </p>
                            )}
                        </div>
                    </>
                )}

                {!isEditing && mode !== "ACTIVITY" && (
                    <>
                        <div>
                            <label htmlFor="reg-language" className={LABEL_CLASS}>
                                Preferred language
                            </label>
                            <select
                                id="reg-language"
                                value={language}
                                onChange={(event) => setLanguage(event.target.value)}
                                className={INPUT_CLASS}
                            >
                                {LANGUAGES.map((option) => (
                                    <option key={option} value={option}>
                                        {option}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label htmlFor="reg-type" className={LABEL_CLASS}>
                                Visitor type
                            </label>
                            <select
                                id="reg-type"
                                value={visitorType}
                                onChange={(event) => setVisitorType(event.target.value)}
                                className={INPUT_CLASS}
                            >
                                {VISITOR_TYPE_CHOICES.map((choice) => (
                                    <option key={choice.value} value={choice.value}>
                                        {choice.label}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </>
                )}

                {!isEditing && mode === "ACTIVITY" && (
                    <div>
                        <label htmlFor="reg-payment" className={LABEL_CLASS}>
                            Payment method
                        </label>
                        <select
                            id="reg-payment"
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
                )}
            </div>

            {!isEditing && mode !== "ACTIVITY" && (
                <div className="mt-4">
                    <label htmlFor="reg-needs" className={LABEL_CLASS}>
                        Special requirements
                    </label>
                    <input
                        id="reg-needs"
                        type="text"
                        value={specialNeeds}
                        onChange={(event) => setSpecialNeeds(event.target.value)}
                        placeholder="Allergies, mobility needs, etc."
                        className={INPUT_CLASS}
                    />
                    {fieldError("specialNeeds") && (
                        <p className={ERROR_CLASS}>{fieldError("specialNeeds")}</p>
                    )}
                </div>
            )}

            {!isEditing && mode === "ACTIVITY" && (
                <p className="mt-3 rounded-md border border-line bg-[#F2FBF9] px-3 py-2 text-xs text-muted">
                    An activity booking keeps the contact details on the booking itself; it does not
                    create a visitor record in the directory.
                </p>
            )}

            {isEditing && (
                <p className="mt-3 rounded-md border border-line bg-[#F2FBF9] px-3 py-2 text-xs text-muted">
                    Visit date, slot and purpose cannot be changed after registration.
                </p>
            )}

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
                          ? "Save visitor"
                          : mode === "EVENT"
                            ? "Register for event"
                            : mode === "ACTIVITY"
                              ? "Book activity"
                              : "Register visitor"}
                </button>
                {isEditing && (
                    <button
                        type="button"
                        onClick={onCancelEdit}
                        disabled={isSubmitting}
                        className="rounded-md border border-line bg-white px-4 py-2.5 text-xs text-primary hover:bg-[#F2FBF9] disabled:opacity-60"
                    >
                        Cancel
                    </button>
                )}
            </div>
        </form>
    );
}