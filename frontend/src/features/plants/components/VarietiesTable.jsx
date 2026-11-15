import VigorBadge from "./VigorBadge";
import {
    formatBlock,
    formatNumber,
    formatSpacing,
    formatText,
} from "../utils/format";
import { TABLE_CARD, TABLE_HEAD } from "./plantsStyles";

// The expected and actual yields are compared over a period, under this table.
const COLUMNS = [
    "Variety",
    "Block",
    "Trees",
    "Spacing",
    "Vigor",
];

export default function VarietiesTable({ varieties, onSelect }) {
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
                    {varieties.map((variety) => (
                        <tr
                            key={variety.id}
                            tabIndex={0}
                            onClick={() => onSelect(variety)}
                            onKeyDown={(event) => {
                                if (event.key === "Enter" || event.key === " ") {
                                    event.preventDefault();
                                    onSelect(variety);
                                }
                            }}
                            className="cursor-pointer border-b border-gray-100 last:border-b-0 hover:bg-gray-50 focus:bg-gray-50 focus:outline-none"
                        >
                            <td className="px-4 py-3 font-semibold text-gray-800">
                                {formatText(variety.name)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatBlock(variety.blockCode)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatNumber(variety.currentTreeCount)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatSpacing(
                                    variety.rowSpacingM,
                                    variety.treeSpacingM,
                                )}
                            </td>
                            <td className="px-4 py-3">
                                <VigorBadge value={variety.vigor} />
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
