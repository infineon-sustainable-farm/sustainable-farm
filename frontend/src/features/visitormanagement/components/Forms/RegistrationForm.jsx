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
 * Registration form: pick an existing visitor by full name first (their
 * details are fetched from the directory and reused — no re-typing), or type
 * a name that is not in the directory to create the visitor, then choose the
 * target — a farm tour slot, an event, or an activity booking.
 *
 * In edit mode it only edits the visitor fields; the API has no endpoint to
 * move a registration to another slot or event.
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
    nextAvailableDate,
    onUseNextDate,
    events,
    activities,
    preselectedEventId,
    editingVisitor,
    visitors = [],
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
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
    const [eventId, setEventId] = useState(preselectedEventId ? String(preselectedEventId) : "");
    const [activityId, setActivityId] = useState("");
    const [paymentMethod, setPaymentMethod] = useState("CASH");
    const [fieldErrors, setFieldErrors] = useState({});

    /*
     * The visitor is either picked from the directory (id set, details shown
     * from the record and no visitor is created) or typed as a new visitor.
     * The picker is hidden in edit mode, which only edits the existing visitor.
     */
    const [pickedVisitorId, setPickedVisitorId] = useState(null);
    const [pickerFocused, setPickerFocused] = useState(false);

    const pickedVisitor =
        !isEditing && pickedVisitorId
            ? visitors.find((visitor) => visitor.id === pickedVisitorId) ?? null
            : null;

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

    const query = fullName.trim();
    const suggestions = !isEditing && !pickedVisitor && pickerFocused && query
        ? visitors
              .filter((visitor) => visitor.fullName.toLowerCase().includes(query.toLowerCase()))
              .slice(0, 6)
        : [];

    function pickVisitor(visitor) {
        setPickedVisitorId(visitor.id);
        setFullName(visitor.fullName ?? "");
        setGroupSize(String(visitor.groupSize ?? 1));
        setEmail(visitor.email ?? "");
        setPhone(visitor.phone ?? "");
        setLanguage(visitor.language ?? "French");
        setVisitorType(
            visitor.type === "SCHOOL"
                ? "SCHOOL"
                : visitor.type === "PARTNER"
                  ? "BUYER"
                  : "GENERAL",
        );
        setSpecialNeeds(visitor.specialNeeds ?? "");
        setFieldErrors({});
    }

    function handleNameChange(value) {
        setFullName(value);
        if (!isEditing) {
            setPickedVisitorId(null);
            setFieldErrors((errors) => {
                const rest = { ...errors };
                delete rest.fullName;
                return rest;
            });
            /*
             * Typing a name the directory already has picks that visitor, so a
             * known visitor needs no further data entry.
             */
            const exactMatch = visitors.find(
                (visitor) =>
                    visitor.fullName.trim().toLowerCase() === value.trim().toLowerCase(),
            );
            if (exactMatch) setPickedVisitorId(exactMatch.id);
        }
    }

    function clearPick() {
        setPickedVisitorId(null);
        setFieldErrors({});
    }

    function validate() {
        const errors = {};
        if (isEditing || !pickedVisitor) {
            if (!fullName.trim()) {
                errors.fullName = "Type the visitor's full name, or pick a match above.";
            } else if (fullName.trim().length > 150) {
                errors.fullName = "Full name must be at most 150 characters.";
            }
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
        if (!isEditing && !pickedVisitor && specialNeeds.trim().length > 500) {
            errors.specialNeeds = "Special requirements must be at most 500 characters.";
        }
        if (needsSlot && !effectiveTimeSlotId) {
            if (nextAvailableDate) {
                errors.timeSlotId = `No bookable slot on this date — the next date with slots is ${nextAvailableDate}.`;
            } else {
                errors.timeSlotId = "Pick a visit date with an available slot.";
            }
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
                    visitorFullName: (pickedVisitor?.fullName ?? fullName).trim(),
                    visitorEmail: email.trim(),
                    visitorPhone: phone.trim() || null,
                    peopleCount: groupSizeNumber,
                    paymentMethod,
                },
            });
            return;
        }

        if (pickedVisitor) {
            if (mode === "EVENT") {
                onSubmit({
                    visitorId: pickedVisitor.id,
                    eventId: Number(effectiveEventId),
                    visitPurpose: purpose,
                });
            } else {
                onSubmit({
                    visitorId: pickedVisitor.id,
                    timeSlotId: Number(effectiveTimeSlotId),
                    visitPurpose: purpose,
                });
            }
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

    /*
     * The visitor's directory fields are only edited for a brand-new visitor,
     * except the activity booking, whose email / phone / people count are the
     * booking's own contact fields even when the visitor is known.
     */
    const showContactFields = isEditing || !pickedVisitor || mode === "ACTIVITY";

    return (
        <form onSubmit={handleSubmit}>
            {/* Step 1 — the visitor, fetched by full name from the directory. */}
            {!isEditing && (
                <div className="mb-4 rounded-md border border-line bg-[#F7FDFB] px-4 py-3.5">
                    <label
                        htmlFor="reg-name"
                        className={`${LABEL_CLASS} !mb-0.5 text-[10px] tracking-widest text-muted`}
                    >
                        Step 1 · Visitor
                    </label>

                    {pickedVisitor ? (
                        <div className="mt-1 flex items-start justify-between gap-3">
                            <div className="min-w-0">
                                <p className="text-sm font-semibold text-ink">
                                    {pickedVisitor.fullName}
                                </p>
                                <p className="mt-0.5 text-xs text-muted">
                                    {formatEnumLabel(pickedVisitor.type)} ·{" "}
                                    {pickedVisitor.groupSize}{" "}
                                    {pickedVisitor.groupSize > 1 ? "people" : "person"}
                                    {pickedVisitor.email ? ` · ${pickedVisitor.email}` : ""}
                                    {pickedVisitor.phone ? ` · ${pickedVisitor.phone}` : ""}
                                    {pickedVisitor.language ? ` · ${pickedVisitor.language}` : ""}
                                </p>
                                {pickedVisitor.specialNeeds && (
                                    <p className="mt-0.5 text-xs text-muted">
                                        Needs: {pickedVisitor.specialNeeds}
                                    </p>
                                )}
                            </div>
                            <button
                                type="button"
                                onClick={clearPick}
                                className="shrink-0 rounded-md border border-line bg-white px-2.5 py-1.5 text-[11px] font-semibold text-primary hover:bg-[#F2FBF9]"
                            >
                                Use a different visitor
                            </button>
                        </div>
                    ) : (
                        <div className="relative">
                            <input
                                id="reg-name"
                                type="text"
                                value={fullName}
                                onChange={(event) => handleNameChange(event.target.value)}
                                onFocus={() => setPickerFocused(true)}
                                onBlur={() => setPickerFocused(false)}
                                placeholder="Type a full name — known visitors are found and reused"
                                className={INPUT_CLASS}
                            />
                            {suggestions.length > 0 && (
                                <ul className="absolute z-10 mt-1 max-h-56 w-full overflow-auto rounded-md border border-line bg-white shadow-lg">
                                    {suggestions.map((visitor) => (
                                        <li key={visitor.id}>
                                            <button
                                                type="button"
                                                onMouseDown={(event) => event.preventDefault()}
                                                onClick={() => pickVisitor(visitor)}
                                                className="flex w-full items-center justify-between gap-3 px-3 py-2 text-left text-sm hover:bg-[#F2FBF9]"
                                            >
                                                <span className="truncate font-medium text-ink">
                                                    {visitor.fullName}
                                                </span>
                                                <span className="shrink-0 text-[11px] text-muted">
                                                    {formatEnumLabel(visitor.type)} ·{" "}
                                                    {visitor.groupSize}
                                                </span>
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}
                            {pickerFocused && query && suggestions.length === 0 && (
                                <p className="mt-1 text-xs text-muted">
                                    No visitor matches “{query}” — the fields below create a new
                                    visitor.
                                </p>
                            )}
                        </div>
                    )}
                    {fieldError("fullName") && (
                        <p className={ERROR_CLASS}>{fieldError("fullName")}</p>
                    )}
                </div>
            )}

            {/* Step 2 — the target of the registration. */}
            {!isEditing && (
                <label className={`${LABEL_CLASS} text-[10px] tracking-widest text-muted`}>
                    Step 2 · Registration for
                </label>
            )}
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

            {/* The target fields always show; the visitor's contact fields show
                only for a brand-new visitor, or for an activity booking whose
                email / phone / people count are the booking's own contact
                fields (activity bookings have no visitor link). */}
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                {isEditing && (
                    <div>
                        <label htmlFor="reg-name" className={LABEL_CLASS}>
                            Full name
                        </label>
                        <input
                            id="reg-name"
                            type="text"
                            value={fullName}
                            onChange={(event) => handleNameChange(event.target.value)}
                            className={INPUT_CLASS}
                        />
                        {fieldError("fullName") && (
                            <p className={ERROR_CLASS}>{fieldError("fullName")}</p>
                        )}
                    </div>
                )}

                {showContactFields && (
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
                )}

                {showContactFields && (
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
                )}

                {showContactFields && (
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
                )}

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
                                {!availabilityLoading &&
                                    !availabilityError &&
                                    (availability ?? []).length === 0 &&
                                    nextAvailableDate &&
                                    nextAvailableDate !== date && (
                                        <p className="mt-1.5 text-xs text-muted">
                                            No slots on this date.{" "}
                                            <button
                                                type="button"
                                                onClick={() => onUseNextDate(nextAvailableDate)}
                                                className="font-semibold text-primary underline hover:text-primary-dark"
                                            >
                                                Use {nextAvailableDate} instead
                                            </button>
                                        </p>
                                    )}
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
                                    {availabilityError && <option value="">Slots unavailable</option>}
                                    {!availabilityLoading && (availability ?? []).length === 0 && (
                                        <option value="">No slots on this date</option>
                                    )}
                                    {!availabilityLoading &&
                                        !availabilityError &&
                                        (availability ?? []).length > 0 &&
                                        bookableSlots.length === 0 && (
                                            <option value="">No slot fits {groupSize} people</option>
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
                                {!availabilityLoading && !availabilityError && (availability ?? []).length > 0 &&
                                    bookableSlots.length === 0 && (
                                        <p className="mt-1 text-xs text-muted">
                                            Every slot on this date is too small for {groupSize} people.
                                        </p>
                                    )}
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

                    {!isEditing && mode !== "ACTIVITY" && !pickedVisitor && (
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

            {!isEditing &&
                mode !== "ACTIVITY" &&
                (pickedVisitor ? (
                    <p className="mt-3 rounded-md border border-line bg-[#F2FBF9] px-3 py-2 text-xs text-muted">
                        The visitor's details come from the directory — they are shared, not
                        re-entered here.
                    </p>
                ) : (
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
                ))}

            {!isEditing && mode === "ACTIVITY" && (
                <p className="mt-3 rounded-md border border-line bg-[#F2FBF9] px-3 py-2 text-xs text-muted">
                    An activity booking keeps the contact details on the booking itself; it does not
                    create a new visitor record — {pickedVisitor ? `“${pickedVisitor.fullName}” stays their directory entry.` : "the email and phone stay on the booking."}
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