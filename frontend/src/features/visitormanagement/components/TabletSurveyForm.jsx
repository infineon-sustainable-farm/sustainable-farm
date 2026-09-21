import { useState } from "react";
import RatingStars from "./RatingStars";

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

/*
 * The mockup's on-site tablet survey, submitted at the wrap-up stop. The API
 * requires a visitor, so the form starts with a visitor select; the origin is
 * set to ON_SITE by the backend when no survey is linked.
 */
export default function TabletSurveyForm({
    visitors,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
}) {
    const [visitorId, setVisitorId] = useState("");
    const [rating, setRating] = useState(0);
    const [briefingClear, setBriefingClear] = useState("");
    const [educationalValue, setEducationalValue] = useState("");
    const [recommend, setRecommend] = useState("");
    const [comment, setComment] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});

    function validate() {
        const errors = {};
        if (!visitorId) errors.visitorId = "Pick the visitor.";
        if (rating < 1 || rating > 5) errors.rating = "Pick a rating from 1 to 5.";
        for (const [field, value] of Object.entries({
            briefingClear,
            educationalValue,
            recommend,
            comment,
        })) {
            if (value.trim().length > 1000) {
                errors[field] = "Must be at most 1000 characters.";
            }
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
            rating,
            briefingClear: briefingClear.trim() || null,
            educationalValue: educationalValue.trim() || null,
            recommend: recommend.trim() || null,
            comment: comment.trim() || null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                    <label htmlFor="fb-visitor" className={LABEL_CLASS}>
                        Visitor
                    </label>
                    <select
                        id="fb-visitor"
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
                    <span className={LABEL_CLASS}>How would you rate your overall visit?</span>
                    <RatingStars value={rating} onChange={setRating} />
                    {fieldError("rating") && <p className={ERROR_CLASS}>{fieldError("rating")}</p>}
                </div>
            </div>

            <div>
                <label htmlFor="fb-briefing" className={LABEL_CLASS}>
                    Was the safety briefing clear?
                </label>
                <input
                    id="fb-briefing"
                    type="text"
                    value={briefingClear}
                    onChange={(event) => setBriefingClear(event.target.value)}
                    placeholder="e.g. Yes, very clear"
                    className={INPUT_CLASS}
                />
                {fieldError("briefingClear") && (
                    <p className={ERROR_CLASS}>{fieldError("briefingClear")}</p>
                )}
            </div>

            <div>
                <label htmlFor="fb-education" className={LABEL_CLASS}>
                    Did the educational program teach you something new?
                </label>
                <input
                    id="fb-education"
                    type="text"
                    value={educationalValue}
                    onChange={(event) => setEducationalValue(event.target.value)}
                    placeholder="e.g. Yes, especially the solar tracking"
                    className={INPUT_CLASS}
                />
                {fieldError("educationalValue") && (
                    <p className={ERROR_CLASS}>{fieldError("educationalValue")}</p>
                )}
            </div>

            <div>
                <label htmlFor="fb-recommend" className={LABEL_CLASS}>
                    Would you recommend this visit?
                </label>
                <input
                    id="fb-recommend"
                    type="text"
                    value={recommend}
                    onChange={(event) => setRecommend(event.target.value)}
                    placeholder="e.g. Definitely"
                    className={INPUT_CLASS}
                />
                {fieldError("recommend") && (
                    <p className={ERROR_CLASS}>{fieldError("recommend")}</p>
                )}
            </div>

            <div>
                <label htmlFor="fb-comment" className={LABEL_CLASS}>
                    Any comment?
                </label>
                <input
                    id="fb-comment"
                    type="text"
                    value={comment}
                    onChange={(event) => setComment(event.target.value)}
                    placeholder="Free comment"
                    className={INPUT_CLASS}
                />
                {fieldError("comment") && <p className={ERROR_CLASS}>{fieldError("comment")}</p>}
            </div>

            {submitError && (
                <p className="rounded-md border border-error/30 bg-error/5 px-3 py-2 text-sm text-error">
                    {submitError}
                </p>
            )}

            <div>
                <button
                    type="submit"
                    disabled={isSubmitting}
                    className="rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {isSubmitting ? "Submitting…" : "Submit feedback (tablet)"}
                </button>
            </div>
        </form>
    );
}