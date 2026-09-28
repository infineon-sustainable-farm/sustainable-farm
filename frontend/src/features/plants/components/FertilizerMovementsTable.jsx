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

// The mock-up's application history, widened to purchases and losses, with
// Source last as in the other lists of the module.
const COLUMNS = [
    "Date",
    "Fertilizer",
    "Type",
    "Quantity",
    "Block",
    "Details",
    "Cost (FCFA)",
    "Cost (EUR)",
    "Source",
];

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
 * movement without a cost shows a dash in each.
 */
export default function FertilizerMovementsTable({ movements }) {
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
                    {movements.map((movement) => (
                        <tr key={movement.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-3 font-medium text-gray-800">
                                {formatDate(movement.movementDate)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(movement.fertilizerName)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatCode(movement.movementType)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatQuantity(movement.quantity, movement.unit)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatBlock(movement.blockCode)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {movementDetails(movement)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatXof(movement.totalCostXof)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatEur(movement.totalCostEur)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(movement.source)}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
