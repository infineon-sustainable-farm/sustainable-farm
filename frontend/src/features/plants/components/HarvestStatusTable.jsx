import { formatBlock, formatCode, formatDate, formatDaysAhead, formatText, NO_VALUE } from "../utils/format";
import { formatObservationMethod } from "../utils/maturityObservations";
import MaturityStatusBadge from "./MaturityStatusBadge";
import Value from "./Value";

// The mock-up's columns that the API gives: "Est. Harvest" is the harvest
// window, "Actual Harvest" the first harvest in it, and "Maturity" is shown
// twice, as estimated and as observed in the field. The running pre-harvest
// interval and the sources are added; Variance and Quantity are not computed
// by the API, and each picking has its own Lot No., in the recorded harvests.
const COLUMNS = [
    "Block",
    "Variety",
    "Estimated",
    "Observed",
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

/**
 * The field observation of the row, as the API sends it: its status, its day
 * and method, the observer, and until when it counts. Without an agronomic
 * reference the API sends the latest observation, which does not count. When
 * a running pre-harvest interval holds an observed "Ready for harvest" back,
 * the status that holds is said under it; nothing is decided here.
 */
function Observed({ status }) {
    const observation = status.observation;
    if (!observation) {
        return <Value>{NO_VALUE}</Value>;
    }
    const isHeldBack = observation.countsUntil !== null && status.maturityStatus !== observation.maturityStatus;
    return (
        <>
            <MaturityStatusBadge status={observation.maturityStatus} />
            {/* gray-600: #4a5565 on white -> 7.56:1. */}
            <span className="mt-1 block text-xs text-gray-600">
                {`${formatDate(observation.observedOn)} · `
                    + formatObservationMethod(observation.method, observation.measuredValue, observation.measuredUnit)}
            </span>
            {observation.observer && <span className="block text-xs text-gray-600">{observation.observer}</span>}
            <span className="block text-xs text-gray-600">
                {observation.countsUntil === null
                    ? "Not counted: no reference value"
                    : `Counts until ${formatDate(observation.countsUntil)}`}
            </span>
            {isHeldBack && (
                <span className="block text-xs font-semibold text-gray-700">
                    {`Held at ${formatCode(status.maturityStatus)}: pre-harvest interval`}
                </span>
            )}
        </>
    );
}

/** Source of the harvest season, of the yield share that opens the window, and of the observation. */
function Sources({ status }) {
    const lines = [];
    if (status.seasonSource !== null || status.phaseShareSource !== null) {
        lines.push(`Season: ${formatText(status.seasonSource)}`, `Share: ${formatText(status.phaseShareSource)}`);
    }
    if (status.observation) {
        lines.push(`Observation: ${formatText(status.observation.source)}`);
    }
    if (lines.length === 0) {
        return <Value>{NO_VALUE}</Value>;
    }
    return lines.map((line) => (
        <span key={line} className="block">
            {line}
        </span>
    ));
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
                                {/* Without a reference there is no season, hence no estimate: said, not dashed. */}
                                {status.estimatedStatus === null
                                    ? <span className="text-gray-600">No reference value</span>
                                    : <MaturityStatusBadge status={status.estimatedStatus} />}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <Observed status={status} />
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
