import { formatCode, NO_VALUE } from "../utils/format";
import PlantsBadge from "./PlantsBadge";
import Value from "./Value";

/*
 * The tones of PlantsBadge, which holds the colours and their contrast, in
 * the words of the data requirements (PL-05-01) rather than the mock-up's
 * "Unripe" and "Ready to harvest". Ready is green (4.75:1), ripe amber
 * (4.74:1) and immature neutral (gray-700 on gray-100 -> 9.36:1).
 */
const STATUS_TONES = {
    IMMATURE: "neutral",
    RIPE: "warning",
    READY_FOR_HARVEST: "success",
};

/**
 * Maturity status of a planted variety, as estimated by the API from its
 * harvest window and the pre-harvest intervals of its block, or as seen in
 * the field ("READY_FOR_HARVEST"), shown in sentence case ("Ready for
 * harvest"). Both take the same tones, so the two columns read alike.
 */
export default function MaturityStatusBadge({ status }) {
    const label = formatCode(status);
    if (label === NO_VALUE) {
        return <Value>{NO_VALUE}</Value>;
    }

    // An unknown status is still shown, in the neutral tone.
    return <PlantsBadge tone={STATUS_TONES[status]}>{label}</PlantsBadge>;
}
