import { formatBlock, formatKilograms, formatPercent, formatShare, formatText, NO_VALUE } from "../utils/format";
import Value from "./Value";

const TH_CLASS =
    "border-b border-gray-200 bg-gray-50 px-4 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase";

/** The growth phases of the trees in the harvest months, each with its share and the source of that share. */
function formatPhases(phases) {
    if (!Array.isArray(phases) || phases.length === 0) return NO_VALUE;
    return phases
        .map((phase) => `${formatText(phase.growthPhase)} ${formatShare(phase.yieldShare)} (${formatText(phase.source)})`)
        .join(", ");
}

/** The lines added up by block, as the API sends them. */
function BlockTable({ blocks }) {
    return (
        <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <caption className="px-4 py-3 text-left text-sm font-semibold text-gray-900">By block</caption>
                <thead>
                    <tr>
                        <th scope="col" className={TH_CLASS}>Block</th>
                        <th scope="col" className={TH_CLASS}>Expected</th>
                        <th scope="col" className={TH_CLASS}>Actual</th>
                        <th scope="col" className={TH_CLASS}>Achieved</th>
                    </tr>
                </thead>
                <tbody>
                    {blocks.map((block) => (
                        <tr key={`${block.farmId}-${block.blockCode}`} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-2.5 font-medium text-gray-800">{formatBlock(block.blockCode)}</td>
                            <td className="px-4 py-2.5 text-gray-700">{formatKilograms(block.expectedKg)}</td>
                            <td className="px-4 py-2.5 text-gray-700">{formatKilograms(block.actualKg)}</td>
                            <td className="px-4 py-2.5 text-gray-700">{formatPercent(block.achievedPct)}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

/**
 * How the expected yield of each line is computed: the yield per tree, the
 * growth phases of the trees in the harvest months, and the sources. Closed by
 * default, as the forecast's details are.
 */
function ComputationDetails({ lines }) {
    return (
        <details className="rounded-xl border border-gray-200 bg-white">
            <summary className="cursor-pointer px-4 py-3 text-sm font-semibold text-gray-700">
                How the expected yield is computed
            </summary>
            <div className="overflow-x-auto border-t border-gray-200">
                <table className="w-full border-collapse text-sm whitespace-nowrap">
                    <thead>
                        <tr>
                            <th scope="col" className={TH_CLASS}>Variety</th>
                            <th scope="col" className={TH_CLASS}>Block</th>
                            <th scope="col" className={TH_CLASS}>Yield per tree</th>
                            <th scope="col" className={TH_CLASS}>Growth phases in the harvest months</th>
                            <th scope="col" className={TH_CLASS}>Sources</th>
                        </tr>
                    </thead>
                    <tbody>
                        {lines.map((line) => (
                            <tr key={line.varietyId} className="border-b border-gray-100 align-top last:border-b-0">
                                <td className="px-4 py-2.5 font-medium text-gray-800">{formatText(line.varietyName)}</td>
                                <td className="px-4 py-2.5 text-gray-700">{formatBlock(line.blockCode)}</td>
                                <td className="px-4 py-2.5 text-gray-700">{formatKilograms(line.yieldPerTreeKg)}</td>
                                <td className="px-4 py-2.5 text-gray-700">
                                    <Value>{formatPhases(line.phases)}</Value>
                                </td>
                                <td className="px-4 py-2.5 text-gray-700">
                                    <span className="block">{`Yield: ${formatText(line.yieldSource)}`}</span>
                                    <span className="block">{`Season: ${formatText(line.seasonSource)}`}</span>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </details>
    );
}

/** The comparison by block, then the details of each line's expected yield. */
export default function YieldComparisonTables({ blocks, lines }) {
    return (
        <>
            <BlockTable blocks={blocks} />
            <ComputationDetails lines={lines} />
        </>
    );
}
