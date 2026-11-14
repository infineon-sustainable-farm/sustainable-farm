import {
    formatAge,
    formatBlock,
    formatDate,
    formatPhaseBand,
    formatList,
    formatMillimeters,
    formatText,
    NO_VALUE,
} from "../utils/format";
import { useBlockRainfall } from "../hooks/useBlockRainfall";
import { RAINFALL_WINDOW_DAYS } from "../utils/rainfall";
import Value from "./Value";
import { TABLE_CARD, TABLE_HEAD, TABLE_HEAD_NUM } from "./plantsStyles";

// The rain is the one measured over the last 30 days; the header says so.
const RAINFALL_COLUMN = `Rainfall (${RAINFALL_WINDOW_DAYS} days)`;

// Mockup order, with Precision after Planting Date and Source last. The mockup's
// Stage is left out: no screen records it, so it could only show a dash.
const COLUMNS = [
    "Block",
    "Variety",
    "Planting Date",
    "Precision",
    "Age",
    "Phase",
    RAINFALL_COLUMN,
    "Source",
];

// Numbers, quantities and durations line up on the right, header included.
const NUMERIC = new Set(["Age", RAINFALL_COLUMN]);

/**
 * The rain of a block over the last 30 days. It is read apart from the rows,
 * so each cell says where its own read stands: a figure, the module's dash
 * when no rain gauge covers the block (no value, not an error), or that the
 * rain could not be read. gray-600 #4a5565 on white -> 7.56:1.
 */
function RainfallCell({ state }) {
    switch (state?.kind) {
        case "value":
            return formatMillimeters(state.rainfallMm);
        case "loading":
            return <span className="text-gray-600">Loading…</span>;
        case "unavailable":
            return <span className="text-gray-600">Rainfall unavailable</span>;
        default:
            return <Value>{NO_VALUE}</Value>;
    }
}

export default function GrowthCalendarTable({ entries }) {
    const rainfall = useBlockRainfall(entries.map((entry) => entry.blockCode));

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
                            <td className="px-4 py-3 text-right text-gray-700">
                                {formatAge(entry.ageYears, entry.ageMonths)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatPhaseBand(entry.growthPhase, entry.phaseYearsBand)}
                            </td>
                            <td className="px-4 py-3 text-right text-gray-700">
                                <RainfallCell state={rainfall[entry.blockCode]} />
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
