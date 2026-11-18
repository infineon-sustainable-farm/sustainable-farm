import { Star } from "lucide-react";
import { useTranslation } from "react-i18next";

/**
 * A 1 to 5 star selector, as in the mockup's tablet survey. Implemented as a radio group so
 * keyboard and screen-reader users get the same control.
 */
export default function RatingStars({ value, onChange }) {
    const { t } = useTranslation("visitormanagement");
    return (
        <div className="flex items-center gap-1" role="radiogroup" aria-label={t("rating.groupLabel")}>
            {[1, 2, 3, 4, 5].map((star) => (
                <button
                    key={star}
                    type="button"
                    role="radio"
                    aria-checked={value === star}
                    aria-label={t("rating.starLabel", { star })}
                    onClick={() => onChange(star)}
                    className={`rounded-control p-0.5 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus-ring ${
                        star <= value ? "text-warning-fg" : "text-border-input"
                    }`}
                >
                    <Star size={20} aria-hidden="true" fill={star <= value ? "currentColor" : "none"} />
                </button>
            ))}
            <span className="ml-2 text-xs text-text-muted">
                {value > 0 ? t("format.outOfFive", { value }) : t("rating.notRated")}
            </span>
        </div>
    );
}
