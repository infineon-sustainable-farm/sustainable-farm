import { useState } from "react";
import { formatEnumLabel } from "../../../../shared/utils/formatEnumLabel";

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

const PURPOSES = ["TOURISM", "PURCHASE", "PARTNERSHIP", "INVESTMENT", "EDUCATION", "OTHER"];

/**
 * Registers one visitor on an event. The API only accepts PUBLISHED events, so
 * the whole form is disabled (with an explanation) for any other status.
 */
export default function EventAttendeeForm({
    visitors,
    disabled,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
}) {
    const [visitorId, setVisitorId] = useState("");
    const [visitPurpose, setVisitPurpose] = useState("TOURISM");
    const [fieldErrors, setFieldErrors] = useState({});

    function handleSubmit(event) {
        event.preventDefault();
        const errors = {};
        if (!visitorId) errors.visitorId = "Pick the visitor.";
        if (!visitPurpose) errors.visitPurpose = "Pick a visit purpose.";
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({ visitorId: Number(visitorId), visitPurpose });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form onSubmit={handleSubmit} className="mt-4 border-t border-line pt-4">
            <div className="grid grid-cols-1 items-end gap-4 sm:grid-cols-3">
                <div>
                    <label htmlFor="ea-visitor" className={LABEL_CLASS}>
                        Visitor
                    </label>
                    <select
                        id="ea-visitor"
                        value={visitorId}
                        onChange={(event) => setVisitorId(event.target.value)}
                        disabled={disabled || isSubmitting}
                        className={`${INPUT_CLASS} disabled:opacity-60`}
                    >
                        <option value="">Select a visitor…</option>
                        {visitors.map((visitor) => (
                            <option key={visitor.id} value={visitor.id}>
                                {visitor.fullName} ({visitor.groupSize})
                            </option>
                        ))}
                    </select>
                    {fieldError("visitorId") && (
                        <p className={ERROR_CLASS}>{fieldError("visitorId")}</p>
                    )}
                </div>

                <div>
                    <label htmlFor="ea-purpose" className={LABEL_CLASS}>
                        Visit purpose
                    </label>
                    <select
                        id="ea-purpose"
                        value={visitPurpose}
                        onChange={(event) => setVisitPurpose(event.target.value)}
                        disabled={disabled || isSubmitting}
                        className={`${INPUT_CLASS} disabled:opacity-60`}
                    >
                        {PURPOSES.map((purpose) => (
                            <option key={purpose} value={purpose}>
                                {formatEnumLabel(purpose)}
                            </option>
                        ))}
                    </select>
                </div>

                <div>
                    <button
                        type="submit"
                        disabled={disabled || isSubmitting}
                        className="w-full rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        {isSubmitting ? "Registering…" : "Register attendee"}
                    </button>
                </div>
            </div>

            {submitError && (
                <p className="mt-3 rounded-md border border-error/30 bg-error/5 px-3 py-2 text-sm text-error">
                    {submitError}
                </p>
            )}
        </form>
    );
}