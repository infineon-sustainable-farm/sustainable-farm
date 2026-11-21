import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Field, Input, Select } from "../../../../shared/ui";
import { useLabels } from "../../utils/labels";
import FormAlert from "../FormAlert";

const EVENT_TYPES = ["OPEN_DAY", "PARTNER_BUYER", "SCHOOL"];

function splitDateTime(value) {
    if (!value) return { date: "", time: "" };
    return { date: value.slice(0, 10), time: value.slice(11, 16) };
}

/**
 * Event form, used to create and to edit an event. Linked visitor groups and the status are not
 * part of EventRequest: attendees are registrations linked to the event through the API, and the
 * status is driven by publish/cancel and the visit lifecycle job.
 */
export default function EventForm({ event, isSubmitting, submitError, serverFieldErrors, onSubmit, onCancel }) {
    const { t } = useTranslation("visitormanagement");
    const labels = useLabels();
    const isEditing = Boolean(event);
    const start = splitDateTime(event?.startDateTime);
    const end = splitDateTime(event?.endDateTime);

    const [title, setTitle] = useState(event?.title ?? "");
    const [type, setType] = useState(event?.type ?? "OPEN_DAY");
    const [date, setDate] = useState(start.date);
    const [startTime, setStartTime] = useState(start.time);
    const [endTime, setEndTime] = useState(end.time);
    const [location, setLocation] = useState(event?.location ?? "");
    const [maxCapacity, setMaxCapacity] = useState(event ? String(event.maxCapacity) : "10");
    const [description, setDescription] = useState(event?.description ?? "");
    const [fieldErrors, setFieldErrors] = useState({});

    function validate() {
        const errors = {};
        if (!title.trim()) {
            errors.title = t("events.validation.titleRequired");
        } else if (title.trim().length > 150) {
            errors.title = t("events.validation.titleTooLong", { max: 150 });
        }
        if (!type) errors.type = t("events.validation.typeRequired");
        if (!date) errors.date = t("events.validation.dateRequired");
        if (!startTime) errors.startTime = t("events.validation.startRequired");
        if (!endTime) errors.endTime = t("events.validation.endRequired");
        if (startTime && endTime && endTime <= startTime) {
            errors.endTime = t("events.validation.endAfterStart");
        }
        const capacity = Number(maxCapacity);
        if (!Number.isInteger(capacity) || capacity < 1) {
            errors.maxCapacity = t("events.validation.capacityInvalid");
        }
        if (location.trim().length > 150) {
            errors.location = t("events.validation.locationTooLong", { max: 150 });
        }
        if (description.trim().length > 1000) {
            errors.description = t("events.validation.descriptionTooLong", { max: 1000 });
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
        <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Field id="ev-title" label={t("events.form.title")} error={fieldError("title")}>
                    <Input
                        type="text"
                        value={title}
                        onChange={(changeEvent) => setTitle(changeEvent.target.value)}
                        placeholder={t("events.form.titlePlaceholder")}
                    />
                </Field>

                <Field id="ev-type" label={t("events.form.type")} error={fieldError("type")}>
                    <Select value={type} onChange={(changeEvent) => setType(changeEvent.target.value)}>
                        {EVENT_TYPES.map((option) => (
                            <option key={option} value={option}>
                                {labels.enum(option)}
                            </option>
                        ))}
                    </Select>
                </Field>

                <Field id="ev-capacity" label={t("events.form.maxCapacity")} error={fieldError("maxCapacity")}>
                    <Input
                        type="number"
                        min="1"
                        value={maxCapacity}
                        onChange={(changeEvent) => setMaxCapacity(changeEvent.target.value)}
                    />
                </Field>

                <Field id="ev-date" label={t("events.form.date")} error={fieldError("date")}>
                    <Input type="date" value={date} onChange={(changeEvent) => setDate(changeEvent.target.value)} />
                </Field>

                <div className="grid grid-cols-2 gap-4">
                    <Field id="ev-start" label={t("events.form.startTime")} error={fieldError("startTime")}>
                        <Input
                            type="time"
                            value={startTime}
                            onChange={(changeEvent) => setStartTime(changeEvent.target.value)}
                        />
                    </Field>
                    <Field id="ev-end" label={t("events.form.endTime")} error={fieldError("endTime")}>
                        <Input
                            type="time"
                            value={endTime}
                            onChange={(changeEvent) => setEndTime(changeEvent.target.value)}
                        />
                    </Field>
                </div>

                <Field id="ev-location" label={t("events.form.location")} error={fieldError("location")}>
                    <Input
                        type="text"
                        value={location}
                        onChange={(changeEvent) => setLocation(changeEvent.target.value)}
                        placeholder={t("events.form.locationPlaceholder")}
                    />
                </Field>
            </div>

            <Field
                id="ev-description"
                className="mt-4"
                label={t("events.form.description")}
                error={fieldError("description")}
            >
                <Input
                    type="text"
                    value={description}
                    onChange={(changeEvent) => setDescription(changeEvent.target.value)}
                    placeholder={t("events.form.descriptionPlaceholder")}
                />
            </Field>

            {submitError && <FormAlert className="mt-4">{submitError}</FormAlert>}

            <div className="mt-4 flex justify-end gap-2">
                {isEditing && (
                    <Button variant="secondary" onClick={onCancel} disabled={isSubmitting}>
                        {t("common:actions.cancel")}
                    </Button>
                )}
                <Button type="submit" loading={isSubmitting}>
                    {isEditing ? t("events.form.save") : t("events.form.create")}
                </Button>
            </div>
        </form>
    );
}
