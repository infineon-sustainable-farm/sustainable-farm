import { formatBlock, formatDate, formatQuantity, formatText, NO_VALUE } from "../utils/format";
import Value from "./Value";
import { TABLE_CARD, TABLE_HEAD } from "./plantsStyles";

/*
 * What a GLOBALG.A.P. plant protection record keeps for each treatment. What
 * decides the next harvest comes first, so that the pre-harvest interval and
 * the date it ends stay in view on a desktop screen; the other details follow,
 * with Source last as in the other tables.
 */
const COLUMNS = [
    "Date",
    "Block",
    "Problem targeted",
    "Product",
    "Pre-harvest interval",
    "Harvest allowed from",
    "Type",
    "Active ingredient",
    "Quantity",
    "Applicator",
    "Equipment",
    "Source",
];

// Headers may wrap, as in the cash flow: long titles no longer widen their column.
const TH_CLASS =
    `${TABLE_HEAD} align-bottom whitespace-normal`;

/** A whole number of days, as the API stores the pre-harvest interval: "14 days", "1 day". */
function formatDays(days) {
    if (days === null || days === undefined) return NO_VALUE;
    return `${days} ${Number(days) === 1 ? "day" : "days"}`;
}

/** The treatment record, one row per treatment, in the order the API sends it. */
export default function TreatmentsTable({ treatments }) {
    return (
        <div className={TABLE_CARD}>
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th key={column} scope="col" className={TH_CLASS}>
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {treatments.map((treatment) => (
                        <tr key={treatment.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-3 text-gray-700">{formatDate(treatment.treatedOn)}</td>
                            <td className="px-4 py-3 font-semibold text-gray-800">{formatBlock(treatment.blockCode)}</td>
                            {/* "Other" is named by the label typed with it, as in the findings. */}
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(treatment.targetOtherLabel ?? treatment.targetIssueName)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">{formatText(treatment.productName)}</td>
                            <td className="px-4 py-3 text-gray-700">{formatDays(treatment.preHarvestIntervalDays)}</td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatDate(treatment.harvestAllowedFrom)}</Value>
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {treatment.findingId === null ? "Preventive" : "Finding"}
                            </td>
                            <td className="px-4 py-3 text-gray-700">{formatText(treatment.activeIngredient)}</td>
                            <td className="px-4 py-3 text-gray-700">{formatQuantity(treatment.quantity, treatment.unit)}</td>
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
