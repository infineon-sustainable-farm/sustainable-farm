import { formatBlock, formatQuantity, formatText } from "../utils/format";
import { formatSignedPercent, formatSignedQuantity } from "../utils/fertilizerPlans";
import PlanCost from "./PlanCost";
import { CELL_NOTE, TABLE_CARD, TABLE_HEAD, TABLE_HEAD_NUM } from "./plantsStyles";

// One row per block and fertilizer. What is still to do sits under the
// planned quantity, what was applied without a plan under the applied one,
// and the gap as a percentage under the gap.
const COLUMNS = [
    "Block",
    "Fertilizer",
    "Planned",
    "Applied",
    "Gap",
    "Planned cost",
    "Applied cost",
];

// Everything but the block and the fertilizer is a quantity or an amount.
const TEXT = new Set(["Block", "Fertilizer"]);

/**
 * Planned and applied quantities by block and fertilizer, in the order the
 * API sends them. Every figure is the API's: the totals, the gap and both
 * costs at the last purchase price. The gap is shown as it is, with its sign
 * and no judgement: the application recommends no rate.
 */
export default function FertilizerPlanComparisonTable({ rows }) {
    return (
        <div className={TABLE_CARD}>
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th key={column} scope="col" className={TEXT.has(column) ? TABLE_HEAD : TABLE_HEAD_NUM}>
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {rows.map((row) => {
                        const gapPercent = formatSignedPercent(row.gapPercent);
                        return (
                            <tr
                                key={`${row.blockCode}-${row.fertilizerId}`}
                                className="border-b border-gray-100 align-top last:border-b-0"
                            >
                                <td className="px-4 py-3 font-semibold text-gray-800">
                                    {formatBlock(row.blockCode)}
                                </td>
                                <td className="px-4 py-3 text-gray-700">
                                    {formatText(row.fertilizerName)}
                                </td>
                                <td className="px-4 py-3 text-right text-gray-700">
                                    {formatQuantity(row.plannedQuantity, row.unit)}
                                    {row.remainingQuantity > 0 && (
                                        <span className={CELL_NOTE}>
                                            {`${formatQuantity(row.remainingQuantity, row.unit)} still to do`}
                                        </span>
                                    )}
                                </td>
                                <td className="px-4 py-3 text-right text-gray-700">
                                    {formatQuantity(row.appliedQuantity, row.unit)}
                                    {row.unplannedQuantity > 0 && (
                                        <span className={CELL_NOTE}>
                                            {`${formatQuantity(row.unplannedQuantity, row.unit)} not planned`}
                                        </span>
                                    )}
                                </td>
                                <td className="px-4 py-3 text-right text-gray-700">
                                    {formatSignedQuantity(row.gapQuantity, row.unit)}
                                    {gapPercent && <span className={CELL_NOTE}>{gapPercent}</span>}
                                </td>
                                <td className="px-4 py-3 text-right text-gray-700">
                                    <PlanCost xof={row.plannedCostXof} eur={row.plannedCostEur} />
                                </td>
                                <td className="px-4 py-3 text-right text-gray-700">
                                    <PlanCost xof={row.appliedCostXof} eur={row.appliedCostEur} />
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
