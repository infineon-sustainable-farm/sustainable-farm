/**
 * A 1–5 star selector, as in the mockup's tablet survey. Implemented as a
 * radio group so keyboard and screen-reader users get the same control.
 */
export default function RatingStars({ value, onChange }) {
    return (
        <div className="flex items-center gap-1" role="radiogroup" aria-label="Rating out of 5">
            {[1, 2, 3, 4, 5].map((star) => (
                <button
                    key={star}
                    type="button"
                    role="radio"
                    aria-checked={value === star}
                    aria-label={`${star} out of 5`}
                    onClick={() => onChange(star)}
                    className={`text-2xl leading-none ${
                        star <= value ? "text-accent" : "text-gray-300"
                    }`}
                >
                    ★
                </button>
            ))}
            <span className="ml-2 text-xs text-muted">{value > 0 ? `${value}/5` : "Not rated"}</span>
        </div>
    );
}