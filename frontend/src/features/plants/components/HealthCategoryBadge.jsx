import { formatCode, NO_VALUE } from "../utils/format";
import PlantsBadge from "./PlantsBadge";
import Value from "./Value";

/*
 * The tones of PlantsBadge, which holds the colours and their contrast. The
 * mock-up shows no block in severe stress; it takes the red of "Weakened".
 */
const CATEGORY_TONES = {
    VERY_HEALTHY: "success",
    HEALTHY: "success",
    MODERATE: "warning",
    WEAKENED: "error",
    SEVERE_STRESS: "error",
};

/**
 * Health category of a score, as computed by the API ("VERY_HEALTHY"), shown
 * in sentence case ("Very healthy"). No score is compared here.
 */
export default function HealthCategoryBadge({ category }) {
    const label = formatCode(category);
    if (label === NO_VALUE) {
        return <Value>{NO_VALUE}</Value>;
    }

    // An unknown category is still shown, in the neutral tone.
    return <PlantsBadge tone={CATEGORY_TONES[category]}>{label}</PlantsBadge>;
}
