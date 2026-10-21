import { formatBlock, formatCode, formatDate, formatNumber, formatText } from "../utils/format";
import Value from "./Value";

// Each column holds what one event type records; the others show the dash.
const COLUMNS = [
    "Date",
    "Batch",
    "Event",
    "Stage",
    "Quantity",
    "Block",
    "Reason",
    "Source",
];

/** Nursery events, one row per event, in the order the API sends them. */
export default function NurseryEventsTable({ events }) {
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
                    {events.map((event) => (
                        <tr key={event.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-3 text-gray-700">
                                {formatDate(event.eventDate)}
                            </td>
                            <td className="px-4 py-3 font-medium text-gray-800">
                                {formatText(event.batchCode)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatCode(event.eventType)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatCode(event.stage)}</Value>
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatNumber(event.quantity)}</Value>
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatBlock(event.blockCode)}</Value>
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatText(event.reason)}</Value>
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(event.source)}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
