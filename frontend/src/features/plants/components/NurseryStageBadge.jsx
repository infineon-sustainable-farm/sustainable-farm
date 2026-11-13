import { formatCode, NO_VALUE } from "../utils/format";
import PlantsBadge from "./PlantsBadge";
import Value from "./Value";

/*
 * The tones of PlantsBadge, which holds the colours and their contrast. As in
 * the mock-up, a batch ready to transplant is green, the stages it grows
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
 * changes ("ROOTSTOCK_GROWTH"), shown in sentence case ("Rootstock growth").
 */
export default function NurseryStageBadge({ stage }) {
    const label = formatCode(stage);
    if (label === NO_VALUE) {
        return <Value>{NO_VALUE}</Value>;
    }

    // An unknown stage is still shown, in the neutral tone.
    return <PlantsBadge tone={STAGE_TONES[stage]}>{label}</PlantsBadge>;
}
