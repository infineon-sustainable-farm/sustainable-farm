import { formatBlock, formatDate, formatQuantity, formatText, NO_VALUE } from "../utils/format";
import Value from "./Value";
import { CELL_NOTE, TABLE_CARD, TABLE_HEAD, TABLE_HEAD_NUM } from "./plantsStyles";

/*
 * What a GLOBALG.A.P. plant protection record keeps for each treatment, on
 * eight columns: what decides the next harvest stays in view, and what only
 * documents the treatment sits in small gray under the value it belongs to
 * (the type under the problem, the active ingredient under the product, the
 * equipment and the source under the applicator).
 */
const COLUMNS = [
    "Date",
    "Block",
    "Problem",
    "Product",
    "Quantity",
    "Interval",
    "Harvest allowed from",
    "Applied by",
];

// Quantities and durations line up on the right, header included.
const NUMERIC = new Set(["Quantity", "Interval"]);

/** A whole number of days, as the API stores the pre-harvest interval: "14 days", "1 day". */
function formatDays(days) {
    if (days === null || days === undefined) return NO_VALUE;
    return `${days} ${Number(days) === 1 ? "day" : "days"}`;
}

/** The lines under the applicator: the equipment when one was entered, then the source. */
function appliedWith(treatment) {
    const equipment = formatText(treatment.equipment);
    const source = `Source: ${formatText(treatment.source)}`;
    return equipment === NO_VALUE ? [source] : [equipment, source];
}

/** The treatment record, one row per treatment, in the order the API sends it. */
export default function TreatmentsTable({ treatments }) {
    return (
        <div className={TABLE_CARD}>
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th key={column} scope="col" className={NUMERIC.has(column) ? TABLE_HEAD_NUM : TABLE_HEAD}>
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {treatments.map((treatment) => {
                        const activeIngredient = formatText(treatment.activeIngredient);
                        return (
                            <tr key={treatment.id} className="border-b border-gray-100 align-top last:border-b-0">
                                <td className="px-4 py-3 text-gray-700">{formatDate(treatment.treatedOn)}</td>
                                <td className="px-4 py-3 font-semibold text-gray-800">{formatBlock(treatment.blockCode)}</td>
                                {/* "Other" is named by the label typed with it, as in the findings. */}
                                <td className="px-4 py-3 text-gray-700">
                                    {formatText(treatment.targetOtherLabel ?? treatment.targetIssueName)}
                                    <span className={CELL_NOTE}>
                                        {treatment.findingId === null ? "Preventive" : "Finding"}
                                    </span>
                                </td>
                                <td className="px-4 py-3 text-gray-700">
                                    {formatText(treatment.productName)}
                                    {activeIngredient !== NO_VALUE && <span className={CELL_NOTE}>{activeIngredient}</span>}
                                </td>
                                <td className="px-4 py-3 text-right text-gray-700">
                                    {formatQuantity(treatment.quantity, treatment.unit)}
                                </td>
                                <td className="px-4 py-3 text-right text-gray-700">
                                    {formatDays(treatment.preHarvestIntervalDays)}
                                </td>
                                <td className="px-4 py-3 text-gray-700">
                                    <Value>{formatDate(treatment.harvestAllowedFrom)}</Value>
                                </td>
                                <td className="px-4 py-3 text-gray-700">
                                    {formatText(treatment.applicator)}
                                    {appliedWith(treatment).map((line) => (
                                        <span key={line} className={CELL_NOTE}>
                                            {line}
                                        </span>
                                    ))}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
