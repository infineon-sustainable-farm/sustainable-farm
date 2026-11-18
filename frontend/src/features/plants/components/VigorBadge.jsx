import { Badge } from "../../../shared/ui";
import { formatEnum, formatCode, NO_VALUE } from "../utils/format";
import Value from "./Value";

/* The tone of each rating, keyed by the code in lower case. */
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
 * Vigor of a variety row, in the active language ("GOOD" as "Good"); a rating
 * the module does not know is still shown in sentence case, never dropped. The
 * label always says what the colour says.
 */
export default function VigorBadge({ value }) {
    if (formatCode(value) === NO_VALUE) {
        // Same dash, same colour as every other missing value in the module.
        return <Value>{NO_VALUE}</Value>;
    }
    const code = String(value).trim();
    const label = formatEnum("vigor", code.toUpperCase());
    return <Badge tone={VIGOR_TONES[code.toLowerCase()] ?? "neutral"}>{label}</Badge>;
}
