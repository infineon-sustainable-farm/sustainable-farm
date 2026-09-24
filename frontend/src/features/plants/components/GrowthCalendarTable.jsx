import {
    formatAge,
    formatBlock,
    formatDate,
    formatPhaseBand,
    formatList,
    formatMillimeters,
    formatText,
} from "../utils/format";

// Mockup order, with Precision after Planting Date and Source last.
const COLUMNS = [
    "Block",
    "Variety",
    "Planting Date",
    "Precision",
    "Age",
    "Stage",
    "Phase",
    "Rainfall",
    "Source",
];

export default function GrowthCalendarTable({ entries }) {
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
                    {entries.map((entry) => (
                        <tr key={entry.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-3 font-medium text-gray-800">
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
                                {formatText(entry.currentStage)}
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
