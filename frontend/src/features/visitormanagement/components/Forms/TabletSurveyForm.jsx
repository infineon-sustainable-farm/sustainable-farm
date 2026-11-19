import { useState } from "react";
import { CircleAlert } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Field, Input } from "../../../../shared/ui";
import FormActions from "../FormActions";
import FormAlert from "../FormAlert";
import RatingStars from "../RatingStars";

// Values sent to the API; the buttons show their translation.
const YES = "Yes";
const NO = "No";
const MAX_NAME_LENGTH = 150;
const MAX_COMMENT_LENGTH = 1000;

function GroupError({ id, children }) {
    if (!children) return null;
    return (
        <p id={id} className="mt-1 flex items-center gap-1 text-xs text-error-fg">
            <CircleAlert size={16} aria-hidden="true" className="shrink-0" />
            <span>{children}</span>
        </p>
    );
}

/** A question answered with Yes or No, as a group of two toggle buttons. */
function ChoiceQuestion({ name, legend, value, onChange, error }) {
    const { t } = useTranslation("visitormanagement");
    const errorId = `${name}-error`;
    return (
        <fieldset aria-describedby={error ? errorId : undefined}>
            <legend className="text-sm font-medium">{legend}</legend>
            <div className="mt-1 flex gap-2">
                {[YES, NO].map((option) => (
                    <Button
                        key={option}
                        name={name}
                        variant={value === option ? "primary" : "secondary"}
                        aria-pressed={value === option}
                        onClick={() => onChange(option === value ? "" : option)}
                    >
                        {option === YES ? t("feedback.tablet.yes") : t("feedback.tablet.no")}
                    </Button>
                ))}
            </div>
            <GroupError id={errorId}>{error}</GroupError>
        </fieldset>
    );
}

/*
 * The on-site tablet survey, submitted at the wrap-up stop. The visitor types their own name
 * rather than being picked from a list, and the three questions are answered with Yes or No. The
 * rating is a 1 to 5 star pick and a comment is optional. The backend records the response as
 * ON_SITE with the typed name when no visitor is linked.
 */
export default function TabletSurveyForm({
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const { t } = useTranslation("visitormanagement");
    const [visitorName, setVisitorName] = useState("");
    const [rating, setRating] = useState(0);
    const [briefingClear, setBriefingClear] = useState("");
    const [educationalValue, setEducationalValue] = useState("");
    const [recommend, setRecommend] = useState("");
    const [comment, setComment] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});

    function validate() {
        const errors = {};
        if (!visitorName.trim()) errors.visitorName = t("feedback.tablet.nameRequired");
        else if (visitorName.trim().length > MAX_NAME_LENGTH) {
            errors.visitorName = t("feedback.tablet.nameTooLong", { max: MAX_NAME_LENGTH });
        }
        if (rating < 1 || rating > 5) errors.rating = t("feedback.tablet.ratingRequired");
        if (!briefingClear) errors.briefingClear = t("feedback.tablet.answerRequired");
        if (!educationalValue) errors.educationalValue = t("feedback.tablet.answerRequired");
        if (!recommend) errors.recommend = t("feedback.tablet.answerRequired");
        if (comment.trim().length > MAX_COMMENT_LENGTH) {
            errors.comment = t("feedback.tablet.commentTooLong", { max: MAX_COMMENT_LENGTH });
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
    const ratingError = fieldError("rating");

    return (
        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <Field label={t("feedback.tablet.name")} error={fieldError("visitorName")}>
                    <Input value={visitorName} onChange={(event) => setVisitorName(event.target.value)} />
                </Field>
                <fieldset aria-describedby={ratingError ? "rating-error" : undefined}>
                    <legend className="text-sm font-medium">{t("feedback.tablet.overall")}</legend>
                    <div className="mt-1">
                        <RatingStars value={rating} onChange={setRating} />
                    </div>
                    <GroupError id="rating-error">{ratingError}</GroupError>
                </fieldset>
            </div>

            <ChoiceQuestion
                name="briefingClear"
                legend={t("feedback.tablet.briefingClear")}
                value={briefingClear}
                onChange={setBriefingClear}
                error={fieldError("briefingClear")}
            />
            <ChoiceQuestion
                name="educationalValue"
                legend={t("feedback.tablet.educationalValue")}
                value={educationalValue}
                onChange={setEducationalValue}
                error={fieldError("educationalValue")}
            />
            <ChoiceQuestion
                name="recommend"
                legend={t("feedback.tablet.recommend")}
                value={recommend}
                onChange={setRecommend}
                error={fieldError("recommend")}
            />

            <Field
                label={
                    <>
                        {t("feedback.tablet.comment")}{" "}
                        <span className="font-normal text-text-muted">{t("feedback.tablet.optional")}</span>
                    </>
                }
                error={fieldError("comment")}
            >
                <Input
                    value={comment}
                    onChange={(event) => setComment(event.target.value)}
                    placeholder={t("feedback.tablet.commentPlaceholder")}
                />
            </Field>

            {submitError && <FormAlert>{submitError}</FormAlert>}

            <FormActions
                submitLabel={t("feedback.tablet.submit")}
                onCancel={onCancel}
                isSubmitting={isSubmitting}
            />
        </form>
    );
}
