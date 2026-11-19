import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input } from "../../../../shared/ui";
import FormActions from "../FormActions";
import FormAlert from "../FormAlert";

/**
 * Tour stop create/edit form. The position is unique among active stops (the API answers 409
 * otherwise), and there is no reactivation call, so a deactivated stop stays out of the timeline.
 */
export default function TourStopForm({
    stop,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const { t } = useTranslation("visitormanagement");
    const isEditing = Boolean(stop);

    const [name, setName] = useState(stop?.name ?? "");
    const [position, setPosition] = useState(stop ? String(stop.position) : "");
    const [durationMinutes, setDurationMinutes] = useState(
        stop ? String(stop.durationMinutes) : "",
    );
    const [maxCapacity, setMaxCapacity] = useState(stop ? String(stop.maxCapacity) : "");
    const [location, setLocation] = useState(stop?.location ?? "");
    const [demo, setDemo] = useState(stop?.demo ?? "");
    const [safetyNotes, setSafetyNotes] = useState(stop?.safetyNotes ?? "");
    const [description, setDescription] = useState(stop?.description ?? "");
    const [fieldErrors, setFieldErrors] = useState({});

    function validate() {
        const errors = {};
        if (!name.trim()) {
            errors.name = t("education.validation.nameRequired");
        } else if (name.trim().length > 150) {
            errors.name = t("education.validation.nameTooLong", { max: 150 });
        }
        const positionNumber = Number(position);
        if (!Number.isInteger(positionNumber) || positionNumber < 1) {
            errors.position = t("education.validation.positionInvalid");
        }
        const durationNumber = Number(durationMinutes);
        if (!Number.isInteger(durationNumber) || durationNumber < 1) {
            errors.durationMinutes = t("education.validation.durationInvalid");
        }
        const capacityNumber = Number(maxCapacity);
        if (!Number.isInteger(capacityNumber) || capacityNumber < 1) {
            errors.maxCapacity = t("education.validation.capacityInvalid");
        }
        if (location.trim().length > 150) {
            errors.location = t("education.validation.locationTooLong", { max: 150 });
        }
        if (demo.trim().length > 1000) {
            errors.demo = t("education.validation.demoTooLong", { max: 1000 });
        }
        if (safetyNotes.trim().length > 1000) {
            errors.safetyNotes = t("education.validation.safetyNotesTooLong", { max: 1000 });
        }
        if (description.trim().length > 2000) {
            errors.description = t("education.validation.descriptionTooLong", { max: 2000 });
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            name: name.trim(),
            position: Number(position),
            durationMinutes: Number(durationMinutes),
            maxCapacity: Number(maxCapacity),
            location: location.trim() || null,
            demo: demo.trim() || null,
            safetyNotes: safetyNotes.trim() || null,
            description: description.trim() || null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Field id="ts-name" label={t("education.stopForm.name")} error={fieldError("name")}>
                    <Input
                        type="text"
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                        placeholder={t("education.stopForm.namePlaceholder")}
                    />
                </Field>

                <Field id="ts-position" label={t("education.stopForm.position")} error={fieldError("position")}>
                    <Input
                        type="number"
                        min="1"
                        value={position}
                        onChange={(event) => setPosition(event.target.value)}
                        placeholder={t("education.stopForm.positionPlaceholder")}
                    />
                </Field>

                <Field
                    id="ts-duration"
                    label={t("education.form.duration")}
                    error={fieldError("durationMinutes")}
                >
                    <Input
                        type="number"
                        min="1"
                        value={durationMinutes}
                        onChange={(event) => setDurationMinutes(event.target.value)}
                    />
                </Field>

                <Field
                    id="ts-capacity"
                    label={t("education.stopForm.maxCapacity")}
                    error={fieldError("maxCapacity")}
                >
                    <Input
                        type="number"
                        min="1"
                        value={maxCapacity}
                        onChange={(event) => setMaxCapacity(event.target.value)}
                    />
                </Field>

                <Field id="ts-location" label={t("education.stopForm.location")} error={fieldError("location")}>
                    <Input
                        type="text"
                        value={location}
                        onChange={(event) => setLocation(event.target.value)}
                        placeholder={t("education.stopForm.locationPlaceholder")}
                    />
                </Field>

                <Field id="ts-demo" label={t("education.stopForm.demo")} error={fieldError("demo")}>
                    <Input
                        type="text"
                        value={demo}
                        onChange={(event) => setDemo(event.target.value)}
                        placeholder={t("education.stopForm.demoPlaceholder")}
                    />
                </Field>
            </div>

            <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
                <Field
                    id="ts-safety"
                    label={t("education.stopForm.safetyNotes")}
                    error={fieldError("safetyNotes")}
                >
                    <Input
                        type="text"
                        value={safetyNotes}
                        onChange={(event) => setSafetyNotes(event.target.value)}
                        placeholder={t("education.stopForm.safetyNotesPlaceholder")}
                    />
                </Field>
                <Field
                    id="ts-description"
                    label={t("education.form.description")}
                    error={fieldError("description")}
                >
                    <Input
                        type="text"
                        value={description}
                        onChange={(event) => setDescription(event.target.value)}
                        placeholder={t("education.form.descriptionPlaceholder")}
                    />
                </Field>
            </div>

            {submitError && <FormAlert className="mt-4">{submitError}</FormAlert>}

            <FormActions
                onCancel={onCancel}
                isSubmitting={isSubmitting}
                submitLabel={isEditing ? t("education.stopForm.save") : t("education.stopForm.create")}
            />
        </form>
    );
}
