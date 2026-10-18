import { useState } from "react";

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

/**
 * Briefing delivery form, opened from a tracker row. The staff name is a free
 * input backed by a datalist of the active staff, so a guest can still be
 * recorded if they are not in the list. The group leader's signature is a
 * plain checkbox: ticking it records the proof of delivery.
 */
export default function BriefingForm({
    registrationId,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const [staffMember, setStaffMember] = useState("");
    const [signed, setSigned] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});

    function handleSubmit(event) {
        event.preventDefault();
        const errors = {};
        if (!staffMember.trim()) {
            errors.staffMember = "Who delivered the briefing is required.";
        } else if (staffMember.trim().length > 150) {
            errors.staffMember = "Staff member must be at most 150 characters.";
        }
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            staffMember: staffMember.trim(),
            signature: signed ? "signed" : null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                    <label
                        htmlFor={`briefing-staff-${registrationId}`}
                        className={LABEL_CLASS}
                    >
                        Delivered by
                    </label>
                    <input
                        id={`briefing-staff-${registrationId}`}
                        type="text"
                        list="briefing-staff-names"
                        value={staffMember}
                        onChange={(event) => setStaffMember(event.target.value)}
                        placeholder="e.g. S. Traoré"
                        className={INPUT_CLASS}
                    />
                    {fieldError("staffMember") && (
                        <p className={ERROR_CLASS}>{fieldError("staffMember")}</p>
                    )}
                </div>
                <label
                    htmlFor={`briefing-signature-${registrationId}`}
                    className="flex items-end gap-2 pb-2 text-sm text-ink"
                >
                    <input
                        id={`briefing-signature-${registrationId}`}
                        type="checkbox"
                        checked={signed}
                        onChange={(event) => setSigned(event.target.checked)}
                        className="mb-0.5 h-4 w-4 accent-[#0a8276]"
                    />
                    <span>
                        Signature (group leader) —{" "}
                        <span className="text-muted">tick to sign</span>
                    </span>
                    {fieldError("signature") && (
                        <span className={ERROR_CLASS}>{fieldError("signature")}</span>
                    )}
                </label>
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
                    {isSubmitting ? "Recording…" : "Record briefing"}
                </button>
                <button
                    type="button"
                    onClick={onCancel}
                    disabled={isSubmitting}
                    className="rounded-md border border-line bg-white px-4 py-2.5 text-xs text-primary hover:bg-[#F2FBF9] disabled:opacity-60"
                >
                    Cancel
                </button>
            </div>
        </form>
    );
}