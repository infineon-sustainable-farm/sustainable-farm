import { Star } from "lucide-react";
import { useTranslation } from "react-i18next";

/** A received rating, "4" followed by a filled star; screen readers hear "4 out of 5". */
export default function RatingValue({ value }) {
    const { t } = useTranslation("visitormanagement");
    return (
        <span className="inline-flex items-center gap-1">
            <span aria-hidden="true">{value}</span>
            <Star size={16} aria-hidden="true" fill="currentColor" className="text-warning-fg" />
            <span className="sr-only">{t("rating.starLabel", { star: value })}</span>
        </span>
    );
}
