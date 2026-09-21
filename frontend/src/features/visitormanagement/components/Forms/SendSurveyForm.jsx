import { useState } from "react";
import { formatEnumLabel } from "../../../../shared/utils/formatEnumLabel";

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

/*
 * The API sends a survey to one visitor at a time and immediately: ON_SITE is
 * rejected (it is the tablet channel), and there is no bulk-recipient or
 * send-timing field. The mockup's recipient groups and timing are therefore not
 * reproduced.
 */
const SENDABLE_CHANNELS = ["EMAIL", "SMS", "WHATSAPP"];

export default function SendSurveyForm({
    visitors,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const [visitorId, setVisitorId] = useState("");
    const [channel, setChannel] = useState("EMAIL");
    const [messageTemplate, setMessageTemplate] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});

    function validate() {
        const errors = {};
        if (!visitorId) errors.visitorId = "Pick the visitor.";
        if (!channel) errors.channel = "Pick a channel.";
        if (messageTemplate.trim().length > 500) {
            errors.messageTemplate = "Message must be at most 500 characters.";
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
            channel,
            messageTemplate: messageTemplate.trim() || null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form
            onSubmit={handleSubmit}
            className="mb-4.5 rounded-lg border border-dashed border-primary bg-[#F2FBF9] p-4.5"
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
                    <label htmlFor="sv-channel" className={LABEL_CLASS}>
                        Channel
                    </label>
                    <select
                        id="sv-channel"
                        value={channel}
                        onChange={(event) => setChannel(event.target.value)}
                        className={INPUT_CLASS}
                    >
                        {SENDABLE_CHANNELS.map((option) => (
                            <option key={option} value={option}>
                                {formatEnumLabel(option)}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            <div className="mt-4">
                <label htmlFor="sv-message" className={LABEL_CLASS}>
                    Message template
                </label>
                <input
                    id="sv-message"
                    type="text"
                    value={messageTemplate}
                    onChange={(event) => setMessageTemplate(event.target.value)}
                    placeholder="Thank you for visiting Sustainable Farm! Share your feedback: [link]"
                    className={INPUT_CLASS}
                />
                {fieldError("messageTemplate") && (
                    <p className={ERROR_CLASS}>{fieldError("messageTemplate")}</p>
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