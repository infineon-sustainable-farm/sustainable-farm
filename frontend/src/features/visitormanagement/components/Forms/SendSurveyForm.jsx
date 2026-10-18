import { useState } from "react";

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

/*
 * Surveys are sent by email only. The backend resolves the recipient from the
 * visitor, so the channel is fixed and the email shown in the form is read-only
 * information pulled from the selected visitor. The message field holds the
 * survey link rather than a prose template.
 */
export default function SendSurveyForm({
    visitors,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const [visitorId, setVisitorId] = useState("");
    const [surveyLink, setSurveyLink] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});

    const selectedVisitor = visitors.find((visitor) => visitor.id === Number(visitorId));
    const recipientEmail = selectedVisitor?.email ?? "";

    function validate() {
        const errors = {};
        if (!visitorId) errors.visitorId = "Pick the visitor.";
        if (!surveyLink.trim()) errors.surveyLink = "Enter the survey link.";
        else if (surveyLink.trim().length > 500) {
            errors.surveyLink = "Survey link must be at most 500 characters.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            visitorId: Number(visitorId),
            channel: "EMAIL",
            messageTemplate: surveyLink.trim(),
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form
            onSubmit={handleSubmit}
           
        >
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                    <label htmlFor="sv-visitor" className={LABEL_CLASS}>
                        Visitor
                    </label>
                    <select
                        id="sv-visitor"
                        value={visitorId}
                        onChange={(event) => setVisitorId(event.target.value)}
                        className={INPUT_CLASS}
                    >
                        <option value="">Select a visitor…</option>
                        {visitors.map((visitor) => (
                            <option key={visitor.id} value={visitor.id}>
                                {visitor.fullName}
                            </option>
                        ))}
                    </select>
                    {fieldError("visitorId") && (
                        <p className={ERROR_CLASS}>{fieldError("visitorId")}</p>
                    )}
                </div>
                <div>
                    <label htmlFor="sv-email" className={LABEL_CLASS}>
                        Email
                    </label>
                    <input
                        id="sv-email"
                        type="email"
                        value={recipientEmail}
                        readOnly
                        placeholder="No email on file"
                        className={`${INPUT_CLASS} bg-[#EFF5F3]`}
                    />
                    {selectedVisitor && !recipientEmail && (
                        <p className={ERROR_CLASS}>This visitor has no email address.</p>
                    )}
                </div>
            </div>

            <div className="mt-4">
                <label htmlFor="sv-message" className={LABEL_CLASS}>
                    Survey Link
                </label>
                <input
                    id="sv-message"
                    type="url"
                    value={surveyLink}
                    onChange={(event) => setSurveyLink(event.target.value)}
                    placeholder="https://forms.example.com/visit-survey"
                    className={INPUT_CLASS}
                />
                {fieldError("surveyLink") && (
                    <p className={ERROR_CLASS}>{fieldError("surveyLink")}</p>
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
                    {isSubmitting ? "Sending…" : "Send survey"}
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