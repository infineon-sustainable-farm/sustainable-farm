import { formatCode, NO_VALUE } from "../utils/format";
import Value from "./Value";

/*
 * Same colours as VigorBadge, which stays untouched (4.50, 4.52 and 5.67:1).
 * Untreated is red and in progress amber, as in the mock-up. A resolved
 * finding needs nothing more, treated or not: both are green, and the label
 * tells them apart.
 */
const STATUS_STYLES = {
    UNTREATED: "border-error bg-[#f7e9e7] text-[#9a3f32]",
    IN_PROGRESS: "border-warning bg-[#f8efde] text-[#8e661c]",
    TREATED: "border-success bg-[#e5f7ef] text-[#257e55]",
    CLOSED_WITHOUT_TREATMENT: "border-success bg-[#e5f7ef] text-[#257e55]",
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

    // An unknown status is still shown, in neutral styling.
    const style = STATUS_STYLES[status] ?? "border-gray-300 bg-gray-100 text-gray-700";
    return (
        <span className={`inline-block rounded-full border px-2.5 py-0.5 text-xs font-bold ${style}`}>
            {label}
        </span>
    );
}
