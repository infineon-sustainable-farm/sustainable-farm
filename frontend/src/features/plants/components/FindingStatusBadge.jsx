import { formatCode, NO_VALUE } from "../utils/format";
import PlantsBadge from "./PlantsBadge";
import Value from "./Value";

/*
 * The tones of PlantsBadge, which holds the colours and their contrast.
 * Untreated is red and in progress amber, as in the mock-up. A resolved
 * finding needs nothing more, treated or not: both are green, and the label
 * tells them apart.
 */
const STATUS_TONES = {
    UNTREATED: "error",
    IN_PROGRESS: "warning",
    TREATED: "success",
    CLOSED_WITHOUT_TREATMENT: "success",
};

/**
 * Status of a finding, as computed by the API from its treatments and its
 * resolution ("IN_PROGRESS"), shown in sentence case ("In progress").
 */
export default function FindingStatusBadge({ status }) {
    const label = formatCode(status);
    if (label === NO_VALUE) {
        return <Value>{NO_VALUE}</Value>;
    }

    // An unknown status is still shown, in the neutral tone.
    return <PlantsBadge tone={STATUS_TONES[status]}>{label}</PlantsBadge>;
}
