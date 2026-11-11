import { formatCode, NO_VALUE } from "../utils/format";
import PlantsBadge from "./PlantsBadge";
import Value from "./Value";

/* The tone of each rating; the colours and their contrast are in PlantsBadge. */
const VIGOR_TONES = {
    good: "success",
    strong: "success",
    high: "success",
    average: "warning",
    medium: "warning",
    moderate: "warning",
    poor: "error",
    weak: "error",
    low: "error",
};

/**
 * Vigor of a variety row. The API codes read in sentence case ("GOOD" as
 * "Good"); a rating written otherwise is still shown, never dropped.
 */
export default function VigorBadge({ value }) {
    const label = formatCode(value);
    if (label === NO_VALUE) {
        // Same dash, same colour as every other missing value in the module.
        return <Value>{NO_VALUE}</Value>;
    }

    // An unrecognised rating is still shown, in neutral styling — the value is
    // never dropped or reinterpreted.
    return <PlantsBadge tone={VIGOR_TONES[label.toLowerCase()]}>{label}</PlantsBadge>;
}
