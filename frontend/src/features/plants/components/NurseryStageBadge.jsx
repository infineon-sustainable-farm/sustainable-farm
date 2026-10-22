import { formatCode, NO_VALUE } from "../utils/format";
import Value from "./Value";

/*
 * Same colours as VigorBadge, which stays untouched (4.50 and 4.52:1), and its
 * neutral style. As in the mock-up, a batch ready to transplant is green, the
 * stages it grows through in the nursery are amber ("Nursery growth"), and
 * germination is neutral.
 */
const READY = "border-success bg-[#e5f7ef] text-[#257e55]";
const GROWING = "border-warning bg-[#f8efde] text-[#8e661c]";
const NEUTRAL = "border-gray-300 bg-gray-100 text-gray-700";

const STAGE_STYLES = {
    GERMINATION: NEUTRAL,
    ROOTSTOCK_GROWTH: GROWING,
    GRAFTED: GROWING,
    HARDENING: GROWING,
    READY_TO_TRANSPLANT: READY,
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

    // An unknown stage is still shown, in neutral styling.
    const style = STAGE_STYLES[stage] ?? NEUTRAL;
    return (
        <span className={`inline-block rounded-full border px-2.5 py-0.5 text-xs font-bold ${style}`}>
            {label}
        </span>
    );
}
