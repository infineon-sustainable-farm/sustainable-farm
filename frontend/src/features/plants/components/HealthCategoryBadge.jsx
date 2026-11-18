import { Badge } from "../../../shared/ui";
import { formatEnum, NO_VALUE } from "../utils/format";
import Value from "./Value";

// A severe stress takes the red of "Weakened".
const CATEGORY_TONES = {
    VERY_HEALTHY: "success",
    HEALTHY: "success",
    MODERATE: "warning",
    WEAKENED: "error",
    SEVERE_STRESS: "error",
};

/**
 * Health category of a score, as computed by the API ("VERY_HEALTHY"), shown
 * with its translated label ("Very healthy"). No score is compared here.
 */
export default function HealthCategoryBadge({ category }) {
    const label = formatEnum("healthCategory", category);
    if (label === NO_VALUE) {
        return <Value>{NO_VALUE}</Value>;
    }
    // An unknown category is still shown, in neutral styling.
    return <Badge tone={CATEGORY_TONES[category] ?? "neutral"}>{label}</Badge>;
}
