import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input } from "../../../../shared/ui";
import FormActions from "../FormActions";
import FormAlert from "../FormAlert";

/**
 * Activity form, used to create an activity and to edit one. The API has no reactivation call,
 * so deactivation is handled by the table, not here.
 */
export default function ActivityForm({ activity, isSubmitting, submitError, serverFieldErrors, onSubmit, onCancel }) {
    const { t } = useTranslation("visitormanagement");
    const isEditing = Boolean(activity);

    const [name, setName] = useState(activity?.name ?? "");
    const [price, setPrice] = useState(activity ? String(activity.price) : "");
    const [capacity, setCapacity] = useState(activity ? String(activity.capacity) : "");
    const [durationMinutes, setDurationMinutes] = useState(activity ? String(activity.durationMinutes) : "");
    const [description, setDescription] = useState(activity?.description ?? "");
    const [fieldErrors, setFieldErrors] = useState({});

    function validate() {
        const errors = {};
        if (!name.trim()) {
            errors.name = t("booking.activityForm.errors.nameRequired");
        } else if (name.trim().length > 150) {
            errors.name = t("booking.activityForm.errors.nameTooLong");
        }
        const priceNumber = Number(price);
        if (price === "" || Number.isNaN(priceNumber) || priceNumber < 0) {
            errors.price = t("booking.activityForm.errors.price");
        }
        const capacityNumber = Number(capacity);
        if (!Number.isInteger(capacityNumber) || capacityNumber < 1) {
            errors.capacity = t("booking.activityForm.errors.capacity");
        }
        const durationNumber = Number(durationMinutes);
        if (!Number.isInteger(durationNumber) || durationNumber < 1) {
            errors.durationMinutes = t("booking.activityForm.errors.duration");
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
            price: Number(price),
            capacity: Number(capacity),
            durationMinutes: Number(durationMinutes),
            description: description.trim() || null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <Field id="act-name" label={t("booking.activityForm.name")} error={fieldError("name")}>
                    <Input
                        type="text"
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                        placeholder={t("booking.activityForm.namePlaceholder")}
                    />
                </Field>
                <Field id="act-price" label={t("booking.activityForm.price")} error={fieldError("price")}>
                    <Input
                        type="number"
                        min="0"
                        step="1"
                        value={price}
                        onChange={(event) => setPrice(event.target.value)}
                        placeholder={t("booking.activityForm.pricePlaceholder")}
                    />
                </Field>
                <Field id="act-capacity" label={t("booking.activityForm.capacity")} error={fieldError("capacity")}>
                    <Input
                        type="number"
                        min="1"
                        value={capacity}
                        onChange={(event) => setCapacity(event.target.value)}
                        placeholder={t("booking.activityForm.capacityPlaceholder")}
                    />
                </Field>
                <Field id="act-duration" label={t("booking.activityForm.duration")} error={fieldError("durationMinutes")}>
                    <Input
                        type="number"
                        min="1"
                        value={durationMinutes}
                        onChange={(event) => setDurationMinutes(event.target.value)}
                        placeholder={t("booking.activityForm.durationPlaceholder")}
                    />
                </Field>
            </div>

            <Field id="act-description" className="mt-4" label={t("booking.activityForm.description")}>
                <Input
                    type="text"
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    placeholder={t("booking.activityForm.descriptionPlaceholder")}
                />
            </Field>

            {submitError && <FormAlert className="mt-4">{submitError}</FormAlert>}

            <FormActions
                onCancel={onCancel}
                isSubmitting={isSubmitting}
                submitLabel={isEditing ? t("booking.activityForm.save") : t("booking.activityForm.create")}
            />
        </form>
    );
}
