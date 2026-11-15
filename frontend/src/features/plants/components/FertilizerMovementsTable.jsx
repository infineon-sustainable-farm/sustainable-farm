import {
    formatBlock,
    formatCode,
    formatDate,
    formatEur,
    formatQuantity,
    formatText,
    formatXof,
    NO_VALUE,
} from "../utils/format";
import { CELL_NOTE, TABLE_CARD, TABLE_HEAD, TABLE_HEAD_NUM } from "./plantsStyles";

// The mock-up's application history, widened to purchases and losses, on
// seven columns: the source sits in small gray under the details, and the
// cost in euros under the cost in FCFA.
const COLUMNS = [
    "Date",
    "Fertilizer",
    "Type",
    "Quantity",
    "Block",
    "Details",
    "Cost",
];

// Quantities and amounts line up on the right, header included.
const NUMERIC = new Set(["Quantity", "Cost"]);

/**
 * What the movement records besides its quantity: the supplier of a purchase,
 * the applicator of an application followed by its method when one was
 * entered, the reason of a loss.
 */
function movementDetails(movement) {
    switch (movement.movementType) {
        case "PURCHASE":
            return formatText(movement.supplier);
        case "APPLICATION": {
            const applicator = formatText(movement.applicator);
            const method = formatText(movement.method);
            return method === NO_VALUE ? applicator : `${applicator} · ${method}`;
        }
        case "LOSS":
            return formatText(movement.reason);
        default:
            return NO_VALUE;
    }
}

/**
 * Fertilizer movements, one row per purchase, application or loss, in the
 * order the API sends them. Both costs are the API's own conversion; a
 * movement without a cost shows one dash.
 */
export default function FertilizerMovementsTable({ movements }) {
    return (
        <div className={TABLE_CARD}>
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th
                                key={column}
                                scope="col"
                                className={NUMERIC.has(column) ? TABLE_HEAD_NUM : TABLE_HEAD}
                            >
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {movements.map((movement) => {
                        const costEur = formatEur(movement.totalCostEur);
                        return (
                            <tr key={movement.id} className="border-b border-gray-100 align-top last:border-b-0">
                                <td className="px-4 py-3 font-semibold text-gray-800">
                                    {formatDate(movement.movementDate)}
                                </td>
                                <td className="px-4 py-3 text-gray-700">
                                    {formatText(movement.fertilizerName)}
                                </td>
                                <td className="px-4 py-3 text-gray-700">
                                    {formatCode(movement.movementType)}
                                </td>
                                <td className="px-4 py-3 text-right text-gray-700">
                                    {formatQuantity(movement.quantity, movement.unit)}
                                </td>
                                <td className="px-4 py-3 text-gray-700">
                                    {formatBlock(movement.blockCode)}
                                </td>
                                <td className="px-4 py-3 text-gray-700">
                                    {movementDetails(movement)}
                                    <span className={CELL_NOTE}>{`Source: ${formatText(movement.source)}`}</span>
                                </td>
                                <td className="px-4 py-3 text-right text-gray-700">
                                    {formatXof(movement.totalCostXof)}
                                    {costEur !== NO_VALUE && <span className={CELL_NOTE}>{costEur}</span>}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
