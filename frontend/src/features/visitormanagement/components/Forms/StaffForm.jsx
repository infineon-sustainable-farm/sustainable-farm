import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Select } from "../../../../shared/ui";
import { useLabels } from "../../utils/labels";
import FormActions from "../FormActions";
import FormAlert from "../FormAlert";
import StatusTag from "../StatusTag";

const ROLES = ["GUIDE", "RECEPTION", "MANAGER"];
const EMAIL_PATTERN = /^\S+@\S+\.\S+$/;

/**
 * Staff create/edit form. A new member is always created active; deactivation and reactivation
 * are handled by the table actions, not by this form.
 */
export default function StaffForm({ staff, isSubmitting, submitError, serverFieldErrors, onSubmit, onCancel }) {
    const { t } = useTranslation("visitormanagement");
    const labels = useLabels();
    const isEditing = Boolean(staff);

    const [fullName, setFullName] = useState(staff?.fullName ?? "");
    const [role, setRole] = useState(staff?.role ?? "GUIDE");
    const [email, setEmail] = useState(staff?.email ?? "");
    const [phone, setPhone] = useState(staff?.phone ?? "");
    const [fieldErrors, setFieldErrors] = useState({});

    // A new member is always created active; editing keeps the current value, since
    // deactivation and reactivation live in the table actions.
    const active = staff ? staff.active : true;

    function validate() {
        const errors = {};
        if (!fullName.trim()) {
            errors.fullName = t("staff.form.fullNameRequired");
        } else if (fullName.trim().length > 150) {
            errors.fullName = t("staff.form.fullNameTooLong", { max: 150 });
        }
        if (!role) errors.role = t("staff.form.roleRequired");
        if (email.trim() && !EMAIL_PATTERN.test(email.trim())) {
            errors.email = t("staff.form.emailInvalid");
        } else if (email.trim().length > 200) {
            errors.email = t("staff.form.emailTooLong", { max: 200 });
        }
        if (phone.trim().length > 30) {
            errors.phone = t("staff.form.phoneTooLong", { max: 30 });
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
            role,
            email: email.trim() || null,
            phone: phone.trim() || null,
            active,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form onSubmit={handleSubmit}>
            {submitError && <FormAlert className="mb-4">{submitError}</FormAlert>}

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <Field label={t("staff.form.fullName")} error={fieldError("fullName")}>
                    <Input type="text" value={fullName} onChange={(event) => setFullName(event.target.value)} />
                </Field>

                <Field label={t("staff.form.role")} error={fieldError("role")}>
                    <Select value={role} onChange={(event) => setRole(event.target.value)}>
                        {ROLES.map((option) => (
                            <option key={option} value={option}>
                                {labels.enum(option)}
                            </option>
                        ))}
                    </Select>
                </Field>

                <Field label={t("staff.form.email")} error={fieldError("email")}>
                    <Input
                        type="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        placeholder={t("staff.form.emailPlaceholder")}
                    />
                </Field>

                <Field label={t("staff.form.phone")} error={fieldError("phone")}>
                    <Input
                        type="tel"
                        value={phone}
                        onChange={(event) => setPhone(event.target.value)}
                        placeholder={t("staff.form.phonePlaceholder")}
                    />
                </Field>
            </div>

            <p className="mt-4 flex items-center gap-2 text-sm">
                <span className="font-medium">{t("staff.form.status")}</span>
                <StatusTag value={active ? "ACTIVE" : "INACTIVE"} />
            </p>

            <FormActions
                onCancel={onCancel}
                isSubmitting={isSubmitting}
                submitLabel={isEditing ? t("staff.form.save") : t("staff.form.create")}
            />
        </form>
    );
}
