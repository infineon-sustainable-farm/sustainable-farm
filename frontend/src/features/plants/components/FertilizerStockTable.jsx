import { formatCode, formatDate, formatQuantity, formatText } from "../utils/format";
import StockStatusBadge from "./StockStatusBadge";
import Value from "./Value";
import { TABLE_CARD, TABLE_HEAD } from "./plantsStyles";

// The unit sits in each cell rather than in the header: a catalogue can mix
// kilograms and litres. "Last restock" is the date of the latest purchase, as
// the API picks it, last as in the mock-up.
const COLUMNS = [
    "Fertilizer",
    "Type",
    "Composition",
    "Stock",
    "Alert threshold",
    "Status",
    "Last restock",
];

/** The catalogue with the stock of each fertilizer, in the order the API sends it. */
export default function FertilizerStockTable({ fertilizers }) {
    return (
        <div className={TABLE_CARD}>
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th
                                key={column}
                                scope="col"
                                className={TABLE_HEAD}
                            >
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {fertilizers.map((fertilizer) => (
                        <tr key={fertilizer.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-3 font-semibold text-gray-800">
                                {formatText(fertilizer.name)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatCode(fertilizer.fertilizerType)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(fertilizer.composition)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatQuantity(fertilizer.currentStock, fertilizer.unit)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatQuantity(fertilizer.reorderThreshold, fertilizer.unit)}
                            </td>
                            <td className="px-4 py-3">
                                <StockStatusBadge belowThreshold={fertilizer.belowThreshold} />
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatDate(fertilizer.lastPurchaseDate)}</Value>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
