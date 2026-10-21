import { formatBlock, formatDate, formatQuantity, formatText, NO_VALUE } from "../utils/format";
import Value from "./Value";

// What a GLOBALG.A.P. plant protection record keeps for each treatment, with
// Source last as in the other tables.
const COLUMNS = [
    "Date",
    "Block",
    "Problem targeted",
    "Type",
    "Product",
    "Active ingredient",
    "Quantity",
    "Pre-harvest interval",
    "Harvest allowed from",
    "Applicator",
    "Equipment",
    "Source",
];

/** A whole number of days, as the API stores the pre-harvest interval: "14 days", "1 day". */
function formatDays(days) {
    if (days === null || days === undefined) return NO_VALUE;
    return `${days} ${Number(days) === 1 ? "day" : "days"}`;
}

/** The treatment record, one row per treatment, in the order the API sends it. */
export default function TreatmentsTable({ treatments }) {
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
                    {treatments.map((treatment) => (
                        <tr key={treatment.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-3 text-gray-700">{formatDate(treatment.treatedOn)}</td>
                            <td className="px-4 py-3 font-medium text-gray-800">{formatBlock(treatment.blockCode)}</td>
                            {/* "Other" is named by the label typed with it, as in the findings. */}
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(treatment.targetOtherLabel ?? treatment.targetIssueName)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {treatment.findingId === null ? "Preventive" : "Finding"}
                            </td>
                            <td className="px-4 py-3 text-gray-700">{formatText(treatment.productName)}</td>
                            <td className="px-4 py-3 text-gray-700">{formatText(treatment.activeIngredient)}</td>
                            <td className="px-4 py-3 text-gray-700">{formatQuantity(treatment.quantity, treatment.unit)}</td>
                            <td className="px-4 py-3 text-gray-700">{formatDays(treatment.preHarvestIntervalDays)}</td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatDate(treatment.harvestAllowedFrom)}</Value>
                            </td>
                            <td className="px-4 py-3 text-gray-700">{formatText(treatment.applicator)}</td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatText(treatment.equipment)}</Value>
                            </td>
                            <td className="px-4 py-3 text-gray-700">{formatText(treatment.source)}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
