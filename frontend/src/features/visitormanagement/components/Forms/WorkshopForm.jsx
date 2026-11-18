import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Select } from "../../../../shared/ui";
import FormActions from "../FormActions";
import FormAlert from "../FormAlert";

/**
 * Workshop form, used both to create (status DRAFT) and to edit a workshop. Assigning a
 * facilitator is editing the facilitator field, which this form does. Facilitators are the
 * guides, so the field is a select built from the active guides; a previously stored facilitator
 * who is no longer in the list keeps an option so the record can be saved unchanged.
 */
export default function WorkshopForm({
    workshop,
    guides,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const { t } = useTranslation("visitormanagement");
    const isEditing = Boolean(workshop);

    const [name, setName] = useState(workshop?.name ?? "");
    const [durationMinutes, setDurationMinutes] = useState(
        workshop ? String(workshop.durationMinutes) : "",
    );
    const [targetGroup, setTargetGroup] = useState(workshop?.targetGroup ?? "");
    const [facilitator, setFacilitator] = useState(workshop?.facilitator ?? "");
    const [description, setDescription] = useState(workshop?.description ?? "");
    const [fieldErrors, setFieldErrors] = useState({});

    function validate() {
        const errors = {};
        if (!name.trim()) {
            errors.name = t("education.validation.nameRequired");
        } else if (name.trim().length > 150) {
            errors.name = t("education.validation.nameTooLong", { max: 150 });
        }
        const duration = Number(durationMinutes);
        if (!Number.isInteger(duration) || duration < 1) {
            errors.durationMinutes = t("education.validation.durationInvalid");
        }
        if (!targetGroup.trim()) {
            errors.targetGroup = t("education.validation.targetGroupRequired");
        } else if (targetGroup.trim().length > 60) {
            errors.targetGroup = t("education.validation.targetGroupTooLong", { max: 60 });
        }
        if (description.trim().length > 1000) {
            errors.description = t("education.validation.descriptionTooLong", { max: 1000 });
        }
        return errors;
    }

    const facilitatorInList = (guides ?? []).some(
        (guide) => guide.fullName === workshop?.facilitator,
    );
    const hasLegacyFacilitator = isEditing && workshop?.facilitator && !facilitatorInList;

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            name: name.trim(),
            durationMinutes: Number(durationMinutes),
            targetGroup: targetGroup.trim(),
            facilitator: facilitator || null,
            description: description.trim() || null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <Field id="ws-name" label={t("education.workshopForm.name")} error={fieldError("name")}>
                    <Input
                        type="text"
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                        placeholder={t("education.workshopForm.namePlaceholder")}
                    />
                </Field>

                <Field
                    id="ws-duration"
                    label={t("education.form.duration")}
                    error={fieldError("durationMinutes")}
                >
                    <Input
                        type="number"
                        min="1"
                        value={durationMinutes}
                        onChange={(event) => setDurationMinutes(event.target.value)}
                        placeholder={t("education.workshopForm.durationPlaceholder")}
                    />
                </Field>

                <Field
                    id="ws-target"
                    label={t("education.workshopForm.targetGroup")}
                    error={fieldError("targetGroup")}
                >
                    <Input
                        type="text"
                        value={targetGroup}
                        onChange={(event) => setTargetGroup(event.target.value)}
                        placeholder={t("education.workshopForm.targetGroupPlaceholder")}
                    />
                </Field>

                <Field
                    id="ws-facilitator"
                    label={t("education.workshopForm.facilitator")}
                    error={fieldError("facilitator")}
                >
                    <Select value={facilitator} onChange={(event) => setFacilitator(event.target.value)}>
                        <option value="">{t("education.workshopForm.selectGuide")}</option>
                        {hasLegacyFacilitator && (
                            <option value={workshop.facilitator}>{workshop.facilitator}</option>
                        )}
                        {(guides ?? []).map((guide) => (
                            <option key={guide.id} value={guide.fullName}>
                                {guide.fullName}
                            </option>
                        ))}
                    </Select>
                </Field>
            </div>

            <Field
                id="ws-description"
                className="mt-4"
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

            {submitError && <FormAlert className="mt-4">{submitError}</FormAlert>}

            <FormActions
                onCancel={onCancel}
                isSubmitting={isSubmitting}
                submitLabel={
                    isEditing ? t("education.workshopForm.save") : t("education.workshopForm.create")
                }
            />
        </form>
    );
}
