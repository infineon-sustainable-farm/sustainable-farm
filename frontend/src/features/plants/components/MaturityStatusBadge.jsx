import { formatCode, NO_VALUE } from "../utils/format";
import Value from "./Value";

/*
 * The mock-up's hues, in the words of the data requirements (PL-05-01) rather
 * than the mock-up's "Unripe" and "Ready to harvest". Ready and ripe take the
 * green and amber of the other badges (4.50 and 4.52:1); immature takes the
 * mock-up's grey, #5b6b66 on #eef2f1 -> 4.98:1.
 */
const STATUS_STYLES = {
    IMMATURE: "border-gray-300 bg-[#eef2f1] text-[#5b6b66]",
    RIPE: "border-warning bg-[#f8efde] text-[#8e661c]",
    READY_FOR_HARVEST: "border-success bg-[#e5f7ef] text-[#257e55]",
};

/**
 * Maturity status of a planted variety, as estimated by the API from its
 * harvest window and the pre-harvest intervals of its block, or as seen in
 * the field ("READY_FOR_HARVEST"), shown in sentence case ("Ready for
 * harvest"). Both take the same hues, so the two columns read alike.
 */
export default function MaturityStatusBadge({ status }) {
    const label = formatCode(status);
    if (label === NO_VALUE) {
        return <Value>{NO_VALUE}</Value>;
    }

    // An unknown status is still shown, in neutral styling.
    const style = STATUS_STYLES[status] ?? "border-gray-300 bg-gray-100 text-gray-700";
    return (
        <span className={`inline-block rounded-full border px-2.5 py-0.5 text-xs font-bold ${style}`}>
            {label}
        </span>
    );
}
