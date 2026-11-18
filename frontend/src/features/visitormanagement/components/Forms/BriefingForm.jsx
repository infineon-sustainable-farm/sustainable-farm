import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input } from "../../../../shared/ui";
import FormActions from "../FormActions";
import FormAlert from "../FormAlert";

const MAX_STAFF_LENGTH = 150;

/**
 * Briefing delivery form, opened from a tracker row. The staff name is a free input backed by a
 * datalist of the active staff (rendered by the page), so a guest can still be recorded if they
 * are not in the list. The group leader's signature is a plain checkbox: ticking it records the
 * proof of delivery.
 */
export default function BriefingForm({
    registrationId,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const { t } = useTranslation("visitormanagement");
    const [staffMember, setStaffMember] = useState("");
    const [signed, setSigned] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});

    function handleSubmit(event) {
        event.preventDefault();
        const errors = {};
        if (!staffMember.trim()) {
            errors.staffMember = t("safety.form.staffRequired");
        } else if (staffMember.trim().length > MAX_STAFF_LENGTH) {
            errors.staffMember = t("safety.form.staffTooLong", { max: MAX_STAFF_LENGTH });
        }
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            staffMember: staffMember.trim(),
            signature: signed ? "signed" : null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];
    const signatureId = `briefing-signature-${registrationId}`;
    const signatureError = fieldError("signature");

    return (
        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <Field
                    id={`briefing-staff-${registrationId}`}
                    label={t("safety.form.deliveredBy")}
                    error={fieldError("staffMember")}
                >
                    <Input
                        list="briefing-staff-names"
                        value={staffMember}
                        onChange={(event) => setStaffMember(event.target.value)}
                    />
                </Field>
                <div className="flex flex-col justify-end pb-2">
                    <label htmlFor={signatureId} className="flex items-center gap-2 text-sm">
                        <input
                            id={signatureId}
                            type="checkbox"
                            checked={signed}
                            onChange={(event) => setSigned(event.target.checked)}
                            className="h-4 w-4 accent-primary"
                        />
                        <span>
                            {t("safety.form.signature")}{" "}
                            <span className="text-text-muted">{t("safety.form.tickToSign")}</span>
                        </span>
                    </label>
                    {signatureError && <p className="mt-1 text-xs text-error-fg">{signatureError}</p>}
                </div>
            </div>

            {submitError && <FormAlert>{submitError}</FormAlert>}

            <FormActions
                submitLabel={t("safety.form.submit")}
                onCancel={onCancel}
                isSubmitting={isSubmitting}
            />
        </form>
    );
}
