import {
    formatBlock,
    formatDate,
    formatNumber,
    formatOrigin,
    formatPercent,
    formatText,
    NO_VALUE,
} from "../utils/format";
import NurseryStageBadge from "./NurseryStageBadge";
import Value from "./Value";

// The mock-up's columns, with "Batch" for its "Lot", as the API's messages say,
// and the plants still in the nursery where it showed the starting count.
const COLUMNS = [
    "Batch",
    "Variety",
    "In nursery",
    "Dest. block",
    "Stage",
    "Planned transplant",
    "Survival rate",
    "Origin",
];

/**
 * Stage of a batch, with the day it was reached. A batch with no plant left
 * keeps its last stage at the API, which no longer says anything useful: it
 * reads "No plants left" instead. That is a display rule on the count the API
 * sends, not a computation.
 */
function StageCell({ batch }) {
    if (batch.currentCount === 0) {
        return <span className="text-gray-700">No plants left</span>;
    }
    const since = formatDate(batch.currentStageSince);
    return (
        <>
            <NurseryStageBadge stage={batch.currentStage} />
            {since !== NO_VALUE && <p className="mt-1 text-xs text-gray-600">{`since ${since}`}</p>}
        </>
    );
}

/** Origin of a batch; a purchased one also names its supplier and, if any, the supplier's lot number. */
function OriginCell({ batch }) {
    const supplier = formatText(batch.supplier);
    const lotNumber = formatText(batch.supplierLotNumber);
    return (
        <>
            {formatOrigin(batch.origin)}
            {supplier !== NO_VALUE && (
                <p className="mt-1 text-xs text-gray-600">
                    {lotNumber === NO_VALUE ? supplier : `${supplier} · lot ${lotNumber}`}
                </p>
            )}
        </>
    );
}

/**
 * Nursery batches, one row per batch, in the order the API sends them. Every
 * count, rate and stage is the API's.
 */
export default function NurseryBatchesTable({ batches }) {
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
                    {batches.map((batch) => (
                        <tr key={batch.id} className="border-b border-gray-100 align-top last:border-b-0">
                            <td className="px-4 py-3 font-medium text-gray-800">
                                {formatText(batch.batchCode)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatText(batch.varietyName)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatNumber(batch.currentCount)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <Value>{formatBlock(batch.plannedBlockCode)}</Value>
                            </td>
                            <td className="px-4 py-3">
                                <StageCell batch={batch} />
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatDate(batch.plannedTransplantOn)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatPercent(batch.survivalRatePct)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                <OriginCell batch={batch} />
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
