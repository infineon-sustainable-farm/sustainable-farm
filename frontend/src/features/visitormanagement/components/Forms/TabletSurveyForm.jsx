import { useState } from "react";
import RatingStars from "../RatingStars";

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";
const YES = "Yes";
const NO = "No";

/*
 * The on-site tablet survey, submitted at the wrap-up stop. The visitor types
 * their own name rather than being picked from a list, and the three questions
 * are answered with simple Yes/No chips instead of free text. The rating stays
 * a 1–5 star pick and a comment is optional. The backend records the response
 * as ON_SITE with the typed name when no visitor is linked.
 */
function ChoiceChips({ name, value, onChange, error }) {
    return (
        <div className="flex gap-2">
            {[YES, NO].map((option) => (
                <button
                    key={option}
                    type="button"
                    name={name}
                    aria-pressed={value === option}
                    onClick={() => onChange(option === value ? "" : option)}
                    className={`rounded-md border px-5 py-2 text-sm font-semibold text-primary transition ${
                        value === option
                            ? "border-primary bg-primary text-white"
                            : "border-line bg-white hover:bg-[#F2FBF9]"
                    }`}
                >
                    {option}
                </button>
            ))}
            {error && <span className={ERROR_CLASS}>{error}</span>}
        </div>
    );
}

export default function TabletSurveyForm({
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const [visitorName, setVisitorName] = useState("");
    const [rating, setRating] = useState(0);
    const [briefingClear, setBriefingClear] = useState("");
    const [educationalValue, setEducationalValue] = useState("");
    const [recommend, setRecommend] = useState("");
    const [comment, setComment] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});

    function validate() {
        const errors = {};
        if (!visitorName.trim()) errors.visitorName = "Enter the visitor's name.";
        else if (visitorName.trim().length > 150) {
            errors.visitorName = "Name must be at most 150 characters.";
        }
        if (rating < 1 || rating > 5) errors.rating = "Pick a rating from 1 to 5.";
        if (!briefingClear) errors.briefingClear = "Pick an answer.";
        if (!educationalValue) errors.educationalValue = "Pick an answer.";
        if (!recommend) errors.recommend = "Pick an answer.";
        if (comment.trim().length > 1000) {
            errors.comment = "Comment must be at most 1000 characters.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            visitorName: visitorName.trim(),
            rating,
            briefingClear,
            educationalValue,
            recommend,
            comment: comment.trim() || null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                    <label htmlFor="fb-name" className={LABEL_CLASS}>
                        Your name
                    </label>
                    <input
                        id="fb-name"
                        type="text"
                        value={visitorName}
                        onChange={(event) => setVisitorName(event.target.value)}
                        placeholder="e.g. Alix VEBAMBA"
                        className={INPUT_CLASS}
                    />
                    {fieldError("visitorName") && (
                        <p className={ERROR_CLASS}>{fieldError("visitorName")}</p>
                    )}
                </div>
                <div>
                    <span className={LABEL_CLASS}>How would you rate your overall visit?</span>
                    <RatingStars value={rating} onChange={setRating} />
                    {fieldError("rating") && <p className={ERROR_CLASS}>{fieldError("rating")}</p>}
                </div>
            </div>

            <div>
                <span className={LABEL_CLASS}>Was the safety briefing clear?</span>
                <ChoiceChips
                    name="briefingClear"
                    value={briefingClear}
                    onChange={setBriefingClear}
                    error={fieldError("briefingClear")}
                />
            </div>

            <div>
                <span className={LABEL_CLASS}>Did the educational program teach you something new?</span>
                <ChoiceChips
                    name="educationalValue"
                    value={educationalValue}
                    onChange={setEducationalValue}
                    error={fieldError("educationalValue")}
                />
            </div>

            <div>
                <span className={LABEL_CLASS}>Would you recommend this visit?</span>
                <ChoiceChips
                    name="recommend"
                    value={recommend}
                    onChange={setRecommend}
                    error={fieldError("recommend")}
                />
            </div>

            <div>
                <label htmlFor="fb-comment" className={LABEL_CLASS}>
                    Any comment? <span className="text-muted">(optional)</span>
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

            <div className="mt-4 flex gap-2.5">
                <button
                    type="submit"
                    disabled={isSubmitting}
                    className="rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {isSubmitting ? "Submitting…" : "Submit feedback (tablet)"}
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