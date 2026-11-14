import {
    formatBlock,
    formatKilograms,
    formatMonth,
    formatNumber,
    formatText,
} from "../utils/format";
import { TABLE_CARD, TABLE_HEAD } from "./plantsStyles";

const COLUMNS = [
    "Month",
    "Block",
    "Variety",
    "Trees",
    "Phase",
    "Expected yield",
    "Sources",
];

/**
 * The forecast line by line: one row per month and variety with a yield above
 * zero, as the API lists them. The sources are shown as the raw codes the API
 * sends, one per reference value.
 */
export default function YieldForecastTable({ entries }) {
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
                    {entries.map((entry) => (
                        <tr
                            key={`${entry.month}-${entry.varietyId}`}
                            className="border-b border-gray-100 last:border-b-0"
                        >
                            <td className="px-4 py-3 font-semibold text-gray-800">
                                {formatMonth(entry.month)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatBlock(entry.blockCode)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(entry.varietyName)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatNumber(entry.treeCount)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(entry.growthPhase)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatKilograms(entry.expectedKg)}
                            </td>
                            {/* The only cell allowed to wrap: three sources on one line would push
                                the table into a sideways scroll even on a wide screen. */}
                            <td className="min-w-72 px-4 py-3 whitespace-normal text-gray-700">
                                {`Yield: ${formatText(entry.yieldSource)} · `
                                    + `Season: ${formatText(entry.seasonSource)} · `
                                    + `Phase share: ${formatText(entry.phaseShareSource)}`}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
