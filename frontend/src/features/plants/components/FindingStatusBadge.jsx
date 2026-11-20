import { Badge } from "../../../shared/ui";
import { formatEnum, NO_VALUE } from "../utils/format";
import Value from "./Value";

/*
 * Untreated is red and in progress amber. A resolved finding needs nothing
 * more, treated or not: both are green, and the label tells them apart.
 */
const STATUS_TONES = {
    UNTREATED: "error",
    IN_PROGRESS: "warning",
    TREATED: "success",
    CLOSED_WITHOUT_TREATMENT: "success",
};

/**
 * Status of a finding, as computed by the API from its treatments and its
 * resolution ("IN_PROGRESS"), shown with its translated label ("In progress").
 */
export default function FindingStatusBadge({ status }) {
    const label = formatEnum("findingStatus", status);
    if (label === NO_VALUE) {
        return <Value>{NO_VALUE}</Value>;
    }
    // An unknown status is still shown, in neutral styling.
    return <Badge tone={STATUS_TONES[status] ?? "neutral"}>{label}</Badge>;
}
