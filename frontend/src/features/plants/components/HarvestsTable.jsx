import { formatBlock, formatDate, formatNumber, formatText } from "../utils/format";

// The mock-up's columns that the API records, in its order ("Actual Harvest" is
// the harvest date), with Source last.
const COLUMNS = [
    "Block",
    "Variety",
    "Harvest Date",
    "Quantity (kg)",
    "Source",
];

/** Recorded harvests, one row per picking, in the order the API sends them. */
export default function HarvestsTable({ harvests }) {
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
                    {harvests.map((harvest) => (
                        <tr key={harvest.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-3 font-medium text-gray-800">
                                {formatBlock(harvest.blockCode)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(harvest.varietyName)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatDate(harvest.harvestDate)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatNumber(harvest.quantityKg)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(harvest.source)}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
