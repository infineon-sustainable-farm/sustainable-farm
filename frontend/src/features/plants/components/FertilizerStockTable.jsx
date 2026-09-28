import { formatCode, formatQuantity, formatText } from "../utils/format";
import StockStatusBadge from "./StockStatusBadge";

// The unit sits in each cell rather than in the header: a catalogue can mix
// kilograms and litres.
const COLUMNS = [
    "Fertilizer",
    "Type",
    "Composition",
    "Stock",
    "Alert threshold",
    "Status",
];

/** The catalogue with the stock of each fertilizer, in the order the API sends it. */
export default function FertilizerStockTable({ fertilizers }) {
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
                    {fertilizers.map((fertilizer) => (
                        <tr key={fertilizer.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-3 font-medium text-gray-800">
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
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
