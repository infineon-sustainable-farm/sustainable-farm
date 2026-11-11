import {
    formatAge,
    formatBlock,
    formatDate,
    formatPhaseBand,
    formatList,
    formatMillimeters,
    formatText,
} from "../utils/format";
import { TABLE_CARD, TABLE_HEAD } from "./plantsStyles";

// Mockup order, with Precision after Planting Date and Source last. The mockup's
// Stage is left out: no screen records it, so it could only show a dash.
const COLUMNS = [
    "Block",
    "Variety",
    "Planting Date",
    "Precision",
    "Age",
    "Phase",
    "Rainfall",
    "Source",
];

export default function GrowthCalendarTable({ entries }) {
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
                        <tr key={entry.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-3 font-semibold text-gray-800">
                                {formatBlock(entry.blockCode)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatList(entry.varieties)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatDate(entry.plantingDate)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(entry.datePrecision)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatAge(entry.ageYears, entry.ageMonths)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatPhaseBand(entry.growthPhase, entry.phaseYearsBand)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatMillimeters(entry.localRainfallMm)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(entry.source)}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
