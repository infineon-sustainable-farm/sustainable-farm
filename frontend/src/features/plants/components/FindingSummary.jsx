import { formatBlock, formatDate, formatText } from "../utils/format";

/**
 * The finding a dialog is about, recalled at its top: where it was seen, what
 * it is, and when. "Other" is named by the label typed with it.
 */
export default function FindingSummary({ finding }) {
    const place = `${formatBlock(finding.blockCode)}${finding.treeLabel ? `, tree ${finding.treeLabel}` : ""}`;
    return (
        <p className="mb-5 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-800">
            <span className="font-semibold">{place}</span>
            {` · ${formatText(finding.otherLabel ?? finding.issueName)} · seen on ${formatDate(finding.inspectedOn)}`}
        </p>
    );
}
