import { formatCode, NO_VALUE } from "../utils/format";
import Value from "./Value";

/*
 * Same colours as VigorBadge, which stays untouched: the border is the shared
 * token, the label its darkened hue on its lightened hue.
 *   #257e55 on #e5f7ef -> 4.50:1
 *   #8e661c on #f8efde -> 4.52:1
 *   #9a3f32 on #f7e9e7 -> 5.67:1
 * The mock-up shows no block in severe stress; it takes the red of "Weakened".
 */
const GOOD = "border-success bg-[#e5f7ef] text-[#257e55]";
const FAIR = "border-warning bg-[#f8efde] text-[#8e661c]";
const POOR = "border-error bg-[#f7e9e7] text-[#9a3f32]";

const CATEGORY_STYLES = {
    VERY_HEALTHY: GOOD,
    HEALTHY: GOOD,
    MODERATE: FAIR,
    WEAKENED: POOR,
    SEVERE_STRESS: POOR,
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

    // An unknown category is still shown, in neutral styling.
    const style = CATEGORY_STYLES[category] ?? "border-gray-300 bg-gray-100 text-gray-700";
    return (
        <span className={`inline-block rounded-full border px-2.5 py-0.5 text-xs font-bold ${style}`}>
            {label}
        </span>
    );
}
