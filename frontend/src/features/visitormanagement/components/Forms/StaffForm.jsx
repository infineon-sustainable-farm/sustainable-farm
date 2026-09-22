import { useState } from "react";
import { formatEnumLabel } from "../../../../shared/utils/formatEnumLabel";

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

const ROLES = ["GUIDE", "RECEPTION", "MANAGER"];
const EMAIL_PATTERN = /^\S+@\S+\.\S+$/;

/**
 * Staff create/edit form. A new member is always created active; deactivation
 * and reactivation are handled by the table actions, not by this form.
 */
export default function StaffForm({
    staff,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const isEditing = Boolean(staff);

    const [fullName, setFullName] = useState(staff?.fullName ?? "");
    const [role, setRole] = useState(staff?.role ?? "GUIDE");
    const [email, setEmail] = useState(staff?.email ?? "");
    const [phone, setPhone] = useState(staff?.phone ?? "");
    const [fieldErrors, setFieldErrors] = useState({});

    // A new member is always created active; editing keeps the current value,
    // since deactivation/reactivation live in the table actions.
    const active = staff ? staff.active : true;

    function validate() {
        const errors = {};
        if (!fullName.trim()) {
            errors.fullName = "Full name is required.";
        } else if (fullName.trim().length > 150) {
            errors.fullName = "Full name must be at most 150 characters.";
        }
        if (!role) errors.role = "Pick a role.";
        if (email.trim() && !EMAIL_PATTERN.test(email.trim())) {
            errors.email = "Enter a valid email address.";
        } else if (email.trim().length > 200) {
            errors.email = "Email must be at most 200 characters.";
        }
        if (phone.trim().length > 30) {
            errors.phone = "Phone must be at most 30 characters.";
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
        <form
            onSubmit={handleSubmit}
           
        >
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                    <label htmlFor="st-name" className={LABEL_CLASS}>
                        Full name
                    </label>
                    <input
                        id="st-name"
                        type="text"
                        value={fullName}
                        onChange={(event) => setFullName(event.target.value)}
                        placeholder="e.g. Alix VEBAMBA"
                        className={INPUT_CLASS}
                    />
                    {fieldError("fullName") && (
                        <p className={ERROR_CLASS}>{fieldError("fullName")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="st-role" className={LABEL_CLASS}>
                        Role
                    </label>
                    <select
                        id="st-role"
                        value={role}
                        onChange={(event) => setRole(event.target.value)}
                        className={INPUT_CLASS}
                    >
                        {ROLES.map((option) => (
                            <option key={option} value={option}>
                                {formatEnumLabel(option)}
                            </option>
                        ))}
                    </select>
                    {fieldError("role") && <p className={ERROR_CLASS}>{fieldError("role")}</p>}
                </div>

                <div>
                    <label htmlFor="st-email" className={LABEL_CLASS}>
                        Email
                    </label>
                    <input
                        id="st-email"
                        type="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        placeholder="name@example.com"
                        className={INPUT_CLASS}
                    />
                    {fieldError("email") && <p className={ERROR_CLASS}>{fieldError("email")}</p>}
                </div>

                <div>
                    <label htmlFor="st-phone" className={LABEL_CLASS}>
                        Phone
                    </label>
                    <input
                        id="st-phone"
                        type="tel"
                        value={phone}
                        onChange={(event) => setPhone(event.target.value)}
                        placeholder="+226 ..."
                        className={INPUT_CLASS}
                    />
                    {fieldError("phone") && <p className={ERROR_CLASS}>{fieldError("phone")}</p>}
                </div>
            </div>

            <label className="mt-4 flex items-center gap-2 text-sm text-ink"><span className="text-xs tracking-wide text-primary uppercase">Status</span> {staff ? (staff.active ? "Active" : "Inactive") : "Active"}</label>

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
                          ? "Save staff member"
                          : "Create staff member"}
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