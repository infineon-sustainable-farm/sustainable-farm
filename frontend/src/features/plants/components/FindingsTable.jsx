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

/** The findings, one row per problem seen, in the order the API sends them. */
export default function FindingsTable({ findings }) {
    return (
        <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th
                                key={column}
                                scope="col"
                                className="border-b border-gray-200 bg-gray-50 px-4 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase"
                            >
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {findings.map((finding) => (
                        <tr key={finding.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-3 font-medium text-gray-800">
                                {formatBlock(finding.blockCode)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatText(finding.treeLabel)}</Value>
                            </td>
                            {/* "Other" is named by the label typed with it, as in the API's alerts. */}
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(finding.otherLabel ?? finding.issueName)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatDate(finding.inspectedOn)}
                            </td>
                            <td className="px-4 py-3">
                                <FindingStatusBadge status={finding.status} />
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatDate(finding.harvestAllowedFrom)}</Value>
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatDate(finding.resolvedOn)}</Value>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
