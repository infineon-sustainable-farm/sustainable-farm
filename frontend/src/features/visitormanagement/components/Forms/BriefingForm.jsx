import { useState } from "react";
import { useTranslation } from "react-i18next";
import PersonSelect from "../../../../app/PersonSelect";
import FormActions from "../FormActions";
import FormAlert from "../FormAlert";

/**
 * Briefing delivery form, opened from a tracker row. Who delivered it is picked in the farm people
 * directory (the Staff screen of this module adds a missing person). The group leader's signature is a plain checkbox: ticking it records the
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
    const [staffMemberPersonId, setStaffMemberPersonId] = useState("");
    const [signed, setSigned] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});

    function handleSubmit(event) {
        event.preventDefault();
        const errors = {};
        if (!staffMemberPersonId) {
            errors.staffMemberPersonId = t("safety.form.staffRequired");
        }
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            staffMemberPersonId: Number(staffMemberPersonId),
            signature: signed ? "signed" : null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];
    const signatureId = `briefing-signature-${registrationId}`;
    const signatureError = fieldError("signature");

    return (
        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <PersonSelect
                    id={`briefing-staff-${registrationId}`}
                    label={t("safety.form.deliveredBy")}
                    value={staffMemberPersonId}
                    onChange={(id) => setStaffMemberPersonId(id)}
                    error={fieldError("staffMemberPersonId") ?? fieldError("staffMember")}
                    required
                />
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
