import { Badge } from "../../../shared/ui";
import { formatEnum, NO_VALUE } from "../utils/format";
import Value from "./Value";

/*
 * As in the mock-up, a batch ready to transplant is green, the stages it grows
 * through in the nursery are amber ("Nursery growth"), and germination is
 * neutral.
 */
const STAGE_TONES = {
    GERMINATION: "neutral",
    ROOTSTOCK_GROWTH: "warning",
    GRAFTED: "warning",
    HARDENING: "warning",
    READY_TO_TRANSPLANT: "success",
};

/**
 * Current stage of a nursery batch, as computed by the API from its stage
 * changes ("ROOTSTOCK_GROWTH"), shown with its translated label ("Rootstock growth").
 */
export default function NurseryStageBadge({ stage }) {
    const label = formatEnum("nurseryStage", stage);
    if (label === NO_VALUE) {
        return <Value>{NO_VALUE}</Value>;
    }

    // An unknown stage is still shown, in neutral styling.
    return <Badge tone={STAGE_TONES[stage] ?? "neutral"}>{label}</Badge>;
}
