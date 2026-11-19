import { Badge } from "../../../shared/ui";
import { formatEnum, NO_VALUE } from "../utils/format";
import Value from "./Value";

// Ready for harvest reads green, ripe amber, immature and any unknown status neutral.
const STATUS_TONES = {
    IMMATURE: "neutral",
    RIPE: "warning",
    READY_FOR_HARVEST: "success",
};

/**
 * Maturity status of a planted variety, as estimated by the API from its
 * harvest window and the pre-harvest intervals of its block, or as seen in
 * the field ("READY_FOR_HARVEST"), shown in words ("Ready for harvest"). Both
 * take the same tones, so the two columns read alike. The label always says
 * what the colour says.
 */
export default function MaturityStatusBadge({ status }) {
    const label = formatEnum("maturityStatus", status);
    if (label === NO_VALUE) {
        return <Value>{NO_VALUE}</Value>;
    }
    return <Badge tone={STATUS_TONES[status] ?? "neutral"}>{label}</Badge>;
}
