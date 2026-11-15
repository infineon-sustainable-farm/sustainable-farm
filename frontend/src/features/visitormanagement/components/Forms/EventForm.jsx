import { useState } from "react";
import { formatEnumLabel } from "../../../../shared/utils/formatEnumLabel";

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

const EVENT_TYPES = ["OPEN_DAY", "PARTNER_BUYER", "SCHOOL"];

function splitDateTime(value) {
    if (!value) return { date: "", time: "" };
    return { date: value.slice(0, 10), time: value.slice(11, 16) };
}

/**
 * Inline event form, used to create and to edit an event. The mockup's
 * "linked visitor groups" and "status" fields are not part of EventRequest:
 * attendees are registrations linked to the event through the API, and the
 * status is driven by publish/cancel and the visit lifecycle job.
 */
export default function EventForm({
    event,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const isEditing = Boolean(event);
    const start = splitDateTime(event?.startDateTime);
    const end = splitDateTime(event?.endDateTime);

    const [title, setTitle] = useState(event?.title ?? "");
    const [type, setType] = useState(event?.type ?? "OPEN_DAY");
    const [date, setDate] = useState(start.date);
    const [startTime, setStartTime] = useState(start.time);
    const [endTime, setEndTime] = useState(end.time);
    const [location, setLocation] = useState(event?.location ?? "");
    const [maxCapacity, setMaxCapacity] = useState(
        event ? String(event.maxCapacity) : "10",
    );
    const [description, setDescription] = useState(event?.description ?? "");
    const [fieldErrors, setFieldErrors] = useState({});

    function validate() {
        const errors = {};
        if (!title.trim()) {
            errors.title = "Event name is required.";
        } else if (title.trim().length > 150) {
            errors.title = "Name must be at most 150 characters.";
        }
        if (!type) errors.type = "Pick an event type.";
        if (!date) errors.date = "Pick a date.";
        if (!startTime) errors.startTime = "Pick a start time.";
        if (!endTime) errors.endTime = "Pick an end time.";
        if (startTime && endTime && endTime <= startTime) {
            errors.endTime = "End time must be after the start time.";
        }
        const capacity = Number(maxCapacity);
        if (!Number.isInteger(capacity) || capacity < 1) {
            errors.maxCapacity = "Capacity must be a whole number of at least 1.";
        }
        if (location.trim().length > 150) {
            errors.location = "Location must be at most 150 characters.";
        }
        if (description.trim().length > 1000) {
            errors.description = "Description must be at most 1000 characters.";
        }
        return errors;
    }

    function handleSubmit(submitEvent) {
        submitEvent.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            title: title.trim(),
            type,
            startDateTime: `${date}T${startTime}:00`,
            endDateTime: `${date}T${endTime}:00`,
            maxCapacity: Number(maxCapacity),
            location: location.trim() || null,
            description: description.trim() || null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form
            onSubmit={handleSubmit}
           
        >
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                <div>
                    <label htmlFor="ev-title" className={LABEL_CLASS}>
                        Event name
                    </label>
                    <input
                        id="ev-title"
                        type="text"
                        value={title}
                        onChange={(changeEvent) => setTitle(changeEvent.target.value)}
                        placeholder="e.g. Open Farm Day"
                        className={INPUT_CLASS}
                    />
                    {fieldError("title") && <p className={ERROR_CLASS}>{fieldError("title")}</p>}
                </div>

                <div>
                    <label htmlFor="ev-type" className={LABEL_CLASS}>
                        Event type
                    </label>
                    <select
                        id="ev-type"
                        value={type}
                        onChange={(changeEvent) => setType(changeEvent.target.value)}
                        className={INPUT_CLASS}
                    >
                        {EVENT_TYPES.map((option) => (
                            <option key={option} value={option}>
                                {formatEnumLabel(option)}
                            </option>
                        ))}
                    </select>
                </div>

                <div>
                    <label htmlFor="ev-capacity" className={LABEL_CLASS}>
                        Max capacity
                    </label>
                    <input
                        id="ev-capacity"
                        type="number"
                        min="1"
                        value={maxCapacity}
                        onChange={(changeEvent) => setMaxCapacity(changeEvent.target.value)}
                        className={INPUT_CLASS}
                    />
                    {fieldError("maxCapacity") && (
                        <p className={ERROR_CLASS}>{fieldError("maxCapacity")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="ev-date" className={LABEL_CLASS}>
                        Date
                    </label>
                    <input
                        id="ev-date"
                        type="date"
                        value={date}
                        onChange={(changeEvent) => setDate(changeEvent.target.value)}
                        className={INPUT_CLASS}
                    />
                    {fieldError("date") && <p className={ERROR_CLASS}>{fieldError("date")}</p>}
                </div>

                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label htmlFor="ev-start" className={LABEL_CLASS}>
                            Start time
                        </label>
                        <input
                            id="ev-start"
                            type="time"
                            value={startTime}
                            onChange={(changeEvent) => setStartTime(changeEvent.target.value)}
                            className={INPUT_CLASS}
                        />
                        {fieldError("startTime") && (
                            <p className={ERROR_CLASS}>{fieldError("startTime")}</p>
                        )}
                    </div>
                    <div>
                        <label htmlFor="ev-end" className={LABEL_CLASS}>
                            End time
                        </label>
                        <input
                            id="ev-end"
                            type="time"
                            value={endTime}
                            onChange={(changeEvent) => setEndTime(changeEvent.target.value)}
                            className={INPUT_CLASS}
                        />
                        {fieldError("endTime") && (
                            <p className={ERROR_CLASS}>{fieldError("endTime")}</p>
                        )}
                    </div>
                </div>

                <div>
                    <label htmlFor="ev-location" className={LABEL_CLASS}>
                        Location on farm
                    </label>
                    <input
                        id="ev-location"
                        type="text"
                        value={location}
                        onChange={(changeEvent) => setLocation(changeEvent.target.value)}
                        placeholder="e.g. Main courtyard"
                        className={INPUT_CLASS}
                    />
                    {fieldError("location") && (
                        <p className={ERROR_CLASS}>{fieldError("location")}</p>
                    )}
                </div>
            </div>

            <div className="mt-4">
                <label htmlFor="ev-description" className={LABEL_CLASS}>
                    Description
                </label>
                <input
                    id="ev-description"
                    type="text"
                    value={description}
                    onChange={(changeEvent) => setDescription(changeEvent.target.value)}
                    placeholder="Short description"
                    className={INPUT_CLASS}
                />
                {fieldError("description") && (
                    <p className={ERROR_CLASS}>{fieldError("description")}</p>
                )}
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
                          ? "Save event"
                          : "Create event"}
                </button>
                {isEditing && (
                    <button
                        type="button"
                        onClick={onCancel}
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