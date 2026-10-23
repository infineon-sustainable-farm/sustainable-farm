import { formatBlock, formatDate, formatDaysAhead, formatText, NO_VALUE } from "../utils/format";
import MaturityStatusBadge from "./MaturityStatusBadge";
import Value from "./Value";

// The mock-up's columns that the API gives: "Est. Harvest" is the harvest
// window, "Actual Harvest" the first harvest in it. The running pre-harvest
// interval and the sources are added; Variance, Quantity and Lot No. are not
// computed by the API.
const COLUMNS = [
    "Block",
    "Variety",
    "Maturity",
    "Harvest window",
    "Harvest allowed from",
    "First harvest",
    "Sources",
];

/**
 * Harvest window, from its first day to the end of the season, with the days
 * left before it opens as the API counts them. Both dates are null without an
 * agronomic reference, and the start is always the first day of a month.
 */
function HarvestWindow({ status }) {
    if (status.estimatedHarvestDate === null) {
        return <Value>{NO_VALUE}</Value>;
    }
    const daysAhead = formatDaysAhead(status.daysFromToday);
    return (
        <>
            {`${formatDate(status.estimatedHarvestDate)} – ${formatDate(status.seasonEnd)}`}
            {/* gray-500: #6a7282 on white -> 4.84:1. */}
            {daysAhead && <span className="block text-xs text-gray-500">{daysAhead}</span>}
        </>
    );
}

/** Source of the harvest season, and of the yield share that opens the window. */
function Sources({ status }) {
    if (status.seasonSource === null && status.phaseShareSource === null) {
        return <Value>{NO_VALUE}</Value>;
    }
    return (
        <>
            <span className="block">{`Season: ${formatText(status.seasonSource)}`}</span>
            <span className="block">{`Share: ${formatText(status.phaseShareSource)}`}</span>
        </>
    );
}

/** The maturity status of each planted variety, in the order the API sends it. */
export default function HarvestStatusTable({ statuses }) {
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
                    {statuses.map((status) => (
                        <tr key={status.varietyId} className="border-b border-gray-100 align-top last:border-b-0">
                            <td className="px-4 py-3 font-medium text-gray-800">{formatBlock(status.blockCode)}</td>
                            <td className="px-4 py-3 text-gray-700">{formatText(status.varietyName)}</td>
                            <td className="px-4 py-3">
                                {/* Without a reference there is no season, hence no status: said, not dashed. */}
                                {status.maturityStatus === null
                                    ? <span className="text-gray-600">No reference value</span>
                                    : <MaturityStatusBadge status={status.maturityStatus} />}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <HarvestWindow status={status} />
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatDate(status.harvestAllowedFrom)}</Value>
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatDate(status.firstHarvestDate)}</Value>
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <Sources status={status} />
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
