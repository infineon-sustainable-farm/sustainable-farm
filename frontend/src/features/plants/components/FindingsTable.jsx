import { SprayCan } from "lucide-react";
import { formatBlock, formatDate, formatText } from "../utils/format";
import FindingStatusBadge from "./FindingStatusBadge";
import Value from "./Value";

// The mock-up's columns that the API records ("Tree ID" is the tree label,
// "Treatment" the status), then the harvest and resolution dates. The mock-up's
// Source is left out: the API gives a finding no source.
const COLUMNS = [
    "Block",
    "Tree",
    "Disease / Pest",
    "Observed on",
    "Status",
    "Harvest allowed from",
    "Resolved on",
];

// Long headers and problem names may wrap, the cells are a little narrower than
// in the other tables, and the row buttons are compact: the actions then fit
// beside the seven columns on a 1280 px screen instead of scrolling away.
const TH_CLASS =
    "border-b border-gray-200 bg-gray-50 px-3 py-3 text-left text-xs font-semibold tracking-wide whitespace-normal text-gray-500 uppercase";
const ACTION_BUTTON_CLASS =
    "inline-flex items-center gap-1 rounded-lg border border-gray-200 bg-white px-2 py-1 text-xs font-semibold text-gray-700 hover:bg-gray-50";

/** What a finding is and where, for the name of its buttons: "Anthracnose on Block A, tree 12". */
function describeFinding(finding) {
    const tree = finding.treeLabel ? `, tree ${finding.treeLabel}` : "";
    return `${formatText(finding.otherLabel ?? finding.issueName)} on ${formatBlock(finding.blockCode)}${tree}`;
}

/**
 * The findings, one row per problem seen, in the order the API sends them. A
 * finding not resolved yet can be treated; a resolved one cannot, as the API
 * refuses it.
 */
export default function FindingsTable({ findings, onTreat }) {
    return (
        // relative: the hidden "Actions" header is absolutely positioned. Without a positioned box
        // here it would sit outside the scrolling area and widen the whole page on a phone.
        <div className="relative overflow-x-auto rounded-xl border border-gray-200 bg-white">
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th key={column} scope="col" className={TH_CLASS}>
                                {column}
                            </th>
                        ))}
                        <th scope="col" className={TH_CLASS}>
                            <span className="sr-only">Actions</span>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {findings.map((finding) => (
                        <tr key={finding.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-3 py-3 font-medium text-gray-800">
                                {formatBlock(finding.blockCode)}
                            </td>
                            <td className="px-3 py-3 text-gray-700">
                                <Value>{formatText(finding.treeLabel)}</Value>
                            </td>
                            {/* "Other" is named by the label typed with it, as in the API's alerts. */}
                            <td className="min-w-36 px-3 py-3 whitespace-normal text-gray-700">
                                {formatText(finding.otherLabel ?? finding.issueName)}
                            </td>
                            <td className="px-3 py-3 text-gray-700">
                                {formatDate(finding.inspectedOn)}
                            </td>
                            <td className="px-3 py-3">
                                <FindingStatusBadge status={finding.status} />
                            </td>
                            <td className="px-3 py-3 text-gray-700">
                                <Value>{formatDate(finding.harvestAllowedFrom)}</Value>
                            </td>
                            <td className="px-3 py-3 text-gray-700">
                                <Value>{formatDate(finding.resolvedOn)}</Value>
                            </td>
                            <td className="px-3 py-3 text-right">
                                {finding.resolvedOn === null && (
                                    <div className="flex justify-end gap-2">
                                        <button
                                            type="button"
                                            onClick={() => onTreat(finding)}
                                            aria-label={`Treat ${describeFinding(finding)}`}
                                            className={ACTION_BUTTON_CLASS}
                                        >
                                            <SprayCan size={14} />
                                            Treat
                                        </button>
                                    </div>
                                )}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
