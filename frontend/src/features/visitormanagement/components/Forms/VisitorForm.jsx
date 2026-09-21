import { useState } from "react";
import { formatEnumLabel } from "../../../../shared/utils/formatEnumLabel";

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

const TYPES = ["INDIVIDUAL", "GROUP", "SCHOOL", "PARTNER"];
const LANGUAGES = ["French", "Moore", "German", "English"];
const EMAIL_PATTERN = /^\S+@\S+\.\S+$/;
const PHONE_PATTERN = /^\+?[0-9][0-9()\-. ]{5,28}$/;

/**
 * Visitor create/edit form of the directory. It sends a VisitorRequest, the
 * same payload the registration screen builds.
 */
export default function VisitorForm({
    visitor,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
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
            errors.fullName = "Full name is required.";
        } else if (fullName.trim().length > 150) {
            errors.fullName = "Full name must be at most 150 characters.";
        }
        const size = Number(groupSize);
        if (!Number.isInteger(size) || size < 1) {
            errors.groupSize = "Group size must be a whole number of at least 1.";
        }
        if (email.trim() && !EMAIL_PATTERN.test(email.trim())) {
            errors.email = "Enter a valid email address.";
        } else if (email.trim().length > 200) {
            errors.email = "Email must be at most 200 characters.";
        }
        if (phone.trim() && !PHONE_PATTERN.test(phone.trim())) {
            errors.phone = "Enter a valid phone number.";
        }
        if (specialNeeds.trim().length > 500) {
            errors.specialNeeds = "Special requirements must be at most 500 characters.";
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
        <form
            onSubmit={handleSubmit}
            className="mb-4.5 rounded-lg border border-dashed border-primary bg-[#F2FBF9] p-4.5"
        >
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                <div>
                    <label htmlFor="vi-name" className={LABEL_CLASS}>
                        Full name
                    </label>
                    <input
                        id="vi-name"
                        type="text"
                        value={fullName}
                        onChange={(event) => setFullName(event.target.value)}
                        placeholder="e.g. Marie Dubois"
                        className={INPUT_CLASS}
                    />
                    {fieldError("fullName") && (
                        <p className={ERROR_CLASS}>{fieldError("fullName")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="vi-group" className={LABEL_CLASS}>
                        Group size
                    </label>
                    <input
                        id="vi-group"
                        type="number"
                        min="1"
                        value={groupSize}
                        onChange={(event) => setGroupSize(event.target.value)}
                        className={INPUT_CLASS}
                    />
                    {fieldError("groupSize") && (
                        <p className={ERROR_CLASS}>{fieldError("groupSize")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="vi-type" className={LABEL_CLASS}>
                        Visitor type
                    </label>
                    <select
                        id="vi-type"
                        value={type}
                        onChange={(event) => setType(event.target.value)}
                        className={INPUT_CLASS}
                    >
                        {TYPES.map((option) => (
                            <option key={option} value={option}>
                                {formatEnumLabel(option)}
                            </option>
                        ))}
                    </select>
                </div>

                <div>
                    <label htmlFor="vi-email" className={LABEL_CLASS}>
                        Email
                    </label>
                    <input
                        id="vi-email"
                        type="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        placeholder="email@example.com"
                        className={INPUT_CLASS}
                    />
                    {fieldError("email") && <p className={ERROR_CLASS}>{fieldError("email")}</p>}
                </div>

                <div>
                    <label htmlFor="vi-phone" className={LABEL_CLASS}>
                        Phone
                    </label>
                    <input
                        id="vi-phone"
                        type="tel"
                        value={phone}
                        onChange={(event) => setPhone(event.target.value)}
                        placeholder="+226 ..."
                        className={INPUT_CLASS}
                    />
                    {fieldError("phone") && <p className={ERROR_CLASS}>{fieldError("phone")}</p>}
                </div>

                <div>
                    <label htmlFor="vi-language" className={LABEL_CLASS}>
                        Preferred language
                    </label>
                    <select
                        id="vi-language"
                        value={language}
                        onChange={(event) => setLanguage(event.target.value)}
                        className={INPUT_CLASS}
                    >
                        {LANGUAGES.map((option) => (
                            <option key={option} value={option}>
                                {option}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            <div className="mt-4">
                <label htmlFor="vi-needs" className={LABEL_CLASS}>
                    Special requirements
                </label>
                <input
                    id="vi-needs"
                    type="text"
                    value={specialNeeds}
                    onChange={(event) => setSpecialNeeds(event.target.value)}
                    placeholder="Allergies, mobility needs, etc."
                    className={INPUT_CLASS}
                />
                {fieldError("specialNeeds") && (
                    <p className={ERROR_CLASS}>{fieldError("specialNeeds")}</p>
                )}
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
                    {isSubmitting ? "Saving…" : isEditing ? "Save visitor" : "Create visitor"}
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