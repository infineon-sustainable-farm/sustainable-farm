import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input } from "../../../../shared/ui";
import FormActions from "../FormActions";
import FormAlert from "../FormAlert";
import { EMAIL_PATTERN, PHONE_PATTERN } from "./visitorRules";
import FetchedVisitorCard from "./registration/FetchedVisitorCard";
import ModeSwitch from "./registration/ModeSwitch";
import TargetFields from "./registration/TargetFields";
import VisitorDetailsFields from "./registration/VisitorDetailsFields";
import { resolvePurposeFromType, resolveTypeAndPurpose, typeChoiceOf } from "./registration/rules";

const NOTE_CLASS = "mt-3 rounded-control bg-surface-muted px-3 py-2 text-xs text-text-muted";

/**
 * Registration form: the visitor is picked by their full name from the directory, their details
 * are fetched and reused, no re-typing. If the typed name is not in the directory, an error is
 * shown and nothing is created; the visitor must be placed in the directory first. Then the
 * target is chosen: a farm tour slot, an event, or an activity booking. In edit mode (opened from
 * the registrations table) it edits the visitor's directory record instead; the API has no
 * endpoint to move a registration to another slot or event.
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
    const { t } = useTranslation("visitormanagement");
    const isEditing = Boolean(editingVisitor);
    const needsSlot = mode === "TOUR" || mode === "ACTIVITY";

    const [fullName, setFullName] = useState(editingVisitor?.fullName ?? "");
    const [details, setDetails] = useState(() => ({
        groupSize: String(editingVisitor?.groupSize ?? 1),
        email: editingVisitor?.email ?? "",
        phone: editingVisitor?.phone ?? "",
        language: editingVisitor?.language ?? "French",
        visitorType: typeChoiceOf(editingVisitor?.type),
        specialNeeds: editingVisitor?.specialNeeds ?? "",
    }));
    const [timeSlotId, setTimeSlotId] = useState("");
    const [eventId, setEventId] = useState(preselectedEventId ? String(preselectedEventId) : "");
    const [activityId, setActivityId] = useState("");
    const [paymentMethod, setPaymentMethod] = useState("CASH");
    const [fieldErrors, setFieldErrors] = useState({});
    const [nameChecked, setNameChecked] = useState(false);
    const [fetchedVisitorId, setFetchedVisitorId] = useState(null);

    const fetchedVisitor =
        !isEditing && fetchedVisitorId
            ? (visitors.find((visitor) => visitor.id === fetchedVisitorId) ?? null)
            : null;

    // In create mode the visitor's own group size drives the slot capacity check; in edit mode
    // the field is still editable.
    const groupSizeNumber = isEditing ? Number(details.groupSize) : (fetchedVisitor?.groupSize ?? 1);
    const validGroupSize = Number.isInteger(groupSizeNumber) && groupSizeNumber >= 1;

    /*
     * Slot selection, derived at render time: a selected slot that can no longer seat the group
     * is ignored and the first bookable slot is used instead, so the form stays usable in one
     * click.
     */
    const bookableSlots = (availability ?? []).filter(
        (slot) => slot.remaining >= (validGroupSize ? groupSizeNumber : 1),
    );
    const selectionStillBookable = bookableSlots.some((slot) => String(slot.id) === timeSlotId);
    const effectiveTimeSlotId = selectionStillBookable
        ? timeSlotId
        : bookableSlots.length > 0
          ? String(bookableSlots[0].id)
          : "";

    const effectiveEventId = eventId || (events.length > 0 ? String(events[0].id) : "");
    const firstActiveActivity = activities.find((activity) => activity.active);
    const effectiveActivityId = activityId || (firstActiveActivity ? String(firstActiveActivity.id) : "");

    function clearNameError() {
        setFieldErrors((errors) => {
            const rest = { ...errors };
            delete rest.fullName;
            return rest;
        });
    }

    /*
     * Typing a full name that exists in the directory fetches that visitor: their details come
     * from the directory record and the registration is created against that visitor. A name that
     * does not match anything is an error, no visitor is created.
     */
    function handleNameChange(value) {
        setFullName(value);
        setNameChecked(false);
        if (isEditing) return;
        const query = value.trim().toLowerCase();
        if (!query) {
            setFetchedVisitorId(null);
            clearNameError();
            return;
        }
        const match = visitors.find((visitor) => visitor.fullName.trim().toLowerCase() === query);
        setFetchedVisitorId(match?.id ?? null);
        clearNameError();
    }

    function handleNameBlur() {
        if (!isEditing && fullName.trim()) setNameChecked(true);
    }

    function validate() {
        const errors = {};
        const { email, phone, specialNeeds } = details;
        if (isEditing) {
            if (!fullName.trim()) {
                errors.fullName = t("visitors.validation.fullNameRequired");
            } else if (fullName.trim().length > 150) {
                errors.fullName = t("visitors.validation.fullNameTooLong");
            }
        } else if (!fullName.trim()) {
            errors.fullName = t("registration.validation.typeName");
        } else if (!fetchedVisitor) {
            errors.fullName = t("registration.validation.notInDirectory");
        }
        if (isEditing && !validGroupSize) {
            errors.groupSize = t("visitors.validation.groupSizeInvalid");
        }
        if (isEditing) {
            if (email.trim() && !EMAIL_PATTERN.test(email.trim())) {
                errors.email = t("visitors.validation.emailInvalid");
            } else if (email.trim().length > 200) {
                errors.email = t("visitors.validation.emailTooLong");
            }
            if (phone.trim() && !PHONE_PATTERN.test(phone.trim())) {
                errors.phone = t("visitors.validation.phoneInvalid");
            }
            if (specialNeeds.trim().length > 500) {
                errors.specialNeeds = t("visitors.validation.specialNeedsTooLong");
            }
        } else if (mode === "ACTIVITY" && !fetchedVisitor?.email?.trim()) {
            errors.email = t("registration.validation.noEmail");
        }
        if (needsSlot && !effectiveTimeSlotId) {
            errors.timeSlotId = nextAvailableDate
                ? t("registration.validation.nextDate", { date: nextAvailableDate })
                : t("registration.validation.pickDate");
        }
        if (mode === "EVENT" && !effectiveEventId) {
            errors.eventId = t("registration.validation.pickEvent");
        }
        if (mode === "ACTIVITY" && !effectiveActivityId) {
            errors.activityId = t("registration.validation.pickActivity");
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        setNameChecked(true);
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        if (isEditing) {
            const { type } = resolveTypeAndPurpose(details.visitorType, groupSizeNumber);
            onSubmit({
                visitor: {
                    fullName: fullName.trim(),
                    groupSize: groupSizeNumber,
                    email: details.email.trim() || null,
                    phone: details.phone.trim() || null,
                    language: details.language || null,
                    type,
                    specialNeeds: details.specialNeeds.trim() || null,
                },
            });
            return;
        }

        if (mode === "ACTIVITY") {
            onSubmit({
                booking: {
                    activityId: Number(effectiveActivityId),
                    timeSlotId: Number(effectiveTimeSlotId),
                    visitorFullName: fetchedVisitor.fullName,
                    visitorEmail: fetchedVisitor.email.trim(),
                    visitorPhone: fetchedVisitor.phone?.trim() || null,
                    peopleCount: groupSizeNumber,
                    paymentMethod,
                },
            });
            return;
        }

        if (mode === "EVENT") {
            onSubmit({
                visitorId: fetchedVisitor.id,
                eventId: Number(effectiveEventId),
                visitPurpose: resolvePurposeFromType(fetchedVisitor.type),
            });
        } else {
            onSubmit({
                visitorId: fetchedVisitor.id,
                timeSlotId: Number(effectiveTimeSlotId),
                visitPurpose: resolvePurposeFromType(fetchedVisitor.type),
            });
        }
    }

    const fieldError = (name) => fieldErrors[name] ?? serverFieldErrors?.[name];
    const nameNotFound = !isEditing && nameChecked && fullName.trim() && !fetchedVisitor;

    function submitLabel() {
        if (isEditing) return t("registration.submit.saveVisitor");
        if (mode === "EVENT") return t("registration.submit.event");
        if (mode === "ACTIVITY") return t("registration.submit.activity");
        return t("registration.submit.tour");
    }

    return (
        <form onSubmit={handleSubmit}>
            {!isEditing && <ModeSwitch mode={mode} onModeChange={onModeChange} />}

            <Field id="reg-name" className="mb-4" label={t("registration.fields.fullName")} error={fieldError("fullName")}>
                <Input
                    type="text"
                    value={fullName}
                    onChange={(event) => handleNameChange(event.target.value)}
                    onBlur={handleNameBlur}
                    placeholder={t("registration.placeholders.fullName")}
                />
                {fetchedVisitor && <FetchedVisitorCard visitor={fetchedVisitor} />}
                {nameNotFound && (
                    <p className="mt-1 text-xs text-error-fg">{t("registration.notFoundHint")}</p>
                )}
            </Field>

            {isEditing ? (
                <VisitorDetailsFields
                    values={details}
                    onChange={(name, value) => setDetails((current) => ({ ...current, [name]: value }))}
                    fieldError={fieldError}
                />
            ) : (
                <TargetFields
                    mode={mode}
                    needsSlot={needsSlot}
                    events={events}
                    activities={activities}
                    eventId={effectiveEventId}
                    onEventChange={setEventId}
                    activityId={effectiveActivityId}
                    onActivityChange={setActivityId}
                    date={date}
                    onDateChange={onDateChange}
                    availability={availability}
                    availabilityLoading={availabilityLoading}
                    availabilityError={availabilityError}
                    onRetryAvailability={onRetryAvailability}
                    nextAvailableDate={nextAvailableDate}
                    onUseNextDate={onUseNextDate}
                    bookableSlots={bookableSlots}
                    timeSlotId={effectiveTimeSlotId}
                    onTimeSlotChange={setTimeSlotId}
                    groupSize={groupSizeNumber}
                    validGroupSize={validGroupSize}
                    paymentMethod={paymentMethod}
                    onPaymentMethodChange={setPaymentMethod}
                    fieldError={fieldError}
                />
            )}

            {!isEditing && mode === "ACTIVITY" && <p className={NOTE_CLASS}>{t("registration.activityNote")}</p>}
            {isEditing && <p className={NOTE_CLASS}>{t("registration.editNote")}</p>}

            {submitError && <FormAlert className="mt-4">{submitError}</FormAlert>}

            <FormActions onCancel={onCancel} isSubmitting={isSubmitting} submitLabel={submitLabel()} />
        </form>
    );
}
