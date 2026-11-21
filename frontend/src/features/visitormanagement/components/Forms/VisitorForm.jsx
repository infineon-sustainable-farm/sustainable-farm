import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Select } from "../../../../shared/ui";
import { useLabels } from "../../utils/labels";
import FormActions from "../FormActions";
import FormAlert from "../FormAlert";
import { EMAIL_PATTERN, PHONE_PATTERN, VISITOR_LANGUAGES } from "./visitorRules";

const TYPES = ["INDIVIDUAL", "GROUP", "SCHOOL", "PARTNER"];

/**
 * Visitor create/edit form of the directory. It sends a VisitorRequest, the same payload the
 * registration screen builds.
 */
export default function VisitorForm({
    visitor,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const { t } = useTranslation("visitormanagement");
    const labels = useLabels();
    const isEditing = Boolean(visitor);

    const [fullName, setFullName] = useState(visitor?.fullName ?? "");
    const [groupSize, setGroupSize] = useState(String(visitor?.groupSize ?? 1));
    const [email, setEmail] = useState(visitor?.email ?? "");
    const [phone, setPhone] = useState(visitor?.phone ?? "");
    const [language, setLanguage] = useState(visitor?.language ?? "French");
    const [type, setType] = useState(visitor?.type ?? "INDIVIDUAL");
    const [specialNeeds, setSpecialNeeds] = useState(visitor?.specialNeeds ?? "");
    const [fieldErrors, setFieldErrors] = useState({});

    function validate() {
        const errors = {};
        if (!fullName.trim()) {
            errors.fullName = t("visitors.validation.fullNameRequired");
        } else if (fullName.trim().length > 150) {
            errors.fullName = t("visitors.validation.fullNameTooLong");
        }
        const size = Number(groupSize);
        if (!Number.isInteger(size) || size < 1) {
            errors.groupSize = t("visitors.validation.groupSizeInvalid");
        }
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
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            fullName: fullName.trim(),
            groupSize: Number(groupSize),
            email: email.trim() || null,
            phone: phone.trim() || null,
            language: language || null,
            type,
            specialNeeds: specialNeeds.trim() || null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Field id="vi-name" label={t("visitors.fields.fullName")} error={fieldError("fullName")}>
                    <Input
                        type="text"
                        value={fullName}
                        onChange={(event) => setFullName(event.target.value)}
                    />
                </Field>

                <Field id="vi-group" label={t("visitors.fields.groupSize")} error={fieldError("groupSize")}>
                    <Input
                        type="number"
                        min="1"
                        value={groupSize}
                        onChange={(event) => setGroupSize(event.target.value)}
                    />
                </Field>

                <Field id="vi-type" label={t("visitors.fields.type")}>
                    <Select
                        value={type}
                        onChange={(event) => setType(event.target.value)}
                        options={TYPES.map((option) => ({ value: option, label: labels.enum(option) }))}
                    />
                </Field>

                <Field id="vi-email" label={t("visitors.fields.email")} error={fieldError("email")}>
                    <Input
                        type="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        placeholder={t("visitors.placeholders.email")}
                    />
                </Field>

                <Field id="vi-phone" label={t("visitors.fields.phone")} error={fieldError("phone")}>
                    <Input
                        type="tel"
                        value={phone}
                        onChange={(event) => setPhone(event.target.value)}
                        placeholder={t("visitors.placeholders.phone")}
                    />
                </Field>

                <Field id="vi-language" label={t("visitors.fields.language")}>
                    <Select
                        value={language}
                        onChange={(event) => setLanguage(event.target.value)}
                        options={VISITOR_LANGUAGES.map((option) => ({
                            value: option,
                            label: labels.language(option),
                        }))}
                    />
                </Field>
            </div>

            <Field
                id="vi-needs"
                className="mt-4"
                label={t("visitors.fields.specialNeeds")}
                error={fieldError("specialNeeds")}
            >
                <Input
                    type="text"
                    value={specialNeeds}
                    onChange={(event) => setSpecialNeeds(event.target.value)}
                    placeholder={t("visitors.placeholders.specialNeeds")}
                />
            </Field>

            {submitError && <FormAlert className="mt-4">{submitError}</FormAlert>}

            <FormActions
                onCancel={onCancel}
                isSubmitting={isSubmitting}
                submitLabel={isEditing ? t("visitors.form.save") : t("visitors.form.create")}
            />
        </form>
    );
}
