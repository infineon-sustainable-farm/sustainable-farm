import { useTranslation } from "react-i18next";
import { Card, Table } from "../../../shared/ui";
import { formatBlock, formatKilograms, formatPercent, formatShare, formatText, NO_VALUE } from "../utils/format";
import Value from "./Value";

/** The lines added up by block, as the API sends them. */
function BlockTable({ blocks }) {
    const { t } = useTranslation("plants");
    const columns = [
        { key: "blockCode", header: t("varieties.comparison.block"), cell: (block) => formatBlock(block.blockCode) },
        { key: "expectedKg", header: t("varieties.comparison.expected"), cell: (block) => formatKilograms(block.expectedKg) },
        { key: "actualKg", header: t("varieties.comparison.actual"), cell: (block) => formatKilograms(block.actualKg) },
        { key: "achievedPct", header: t("varieties.comparison.achieved"), cell: (block) => formatPercent(block.achievedPct) },
    ];
    return (
        <Card title={t("varieties.comparison.byBlock")} titleAs="h3">
            <Table
                caption={t("varieties.comparison.byBlock")}
                columns={columns}
                rows={blocks}
                getRowKey={(block) => `${block.farmId}-${block.blockCode}`}
                className="whitespace-nowrap"
            />
        </Card>
    );
}

/**
 * How the expected yield of each line is computed: the yield per tree, the
 * growth phases of the trees in the harvest months, and the sources. Closed by
 * default, as the forecast's details are.
 */
function ComputationDetails({ lines }) {
    const { t } = useTranslation("plants");

    /** The growth phases of the trees in the harvest months, each with its share and the source of that share. */
    function formatPhases(phases) {
        if (!Array.isArray(phases) || phases.length === 0) return NO_VALUE;
        return phases
            .map((phase) =>
                t("varieties.comparison.phase", {
                    phase: formatText(phase.growthPhase),
                    share: formatShare(phase.yieldShare),
                    source: formatText(phase.source),
                })
            )
            .join(t("varieties.comparison.phaseSeparator"));
    }

    const columns = [
        { key: "varietyName", header: t("varieties.comparison.variety"), cell: (line) => formatText(line.varietyName) },
        { key: "blockCode", header: t("varieties.comparison.block"), cell: (line) => formatBlock(line.blockCode) },
        { key: "yieldPerTreeKg", header: t("varieties.comparison.yieldPerTree"), cell: (line) => formatKilograms(line.yieldPerTreeKg) },
        { key: "phases", header: t("varieties.comparison.phases"), cell: (line) => <Value>{formatPhases(line.phases)}</Value> },
        {
            key: "sources",
            header: t("varieties.comparison.sources"),
            cell: (line) => (
                <>
                    <span className="block">{t("varieties.comparison.yieldSource", { source: formatText(line.yieldSource) })}</span>
                    <span className="block">{t("varieties.comparison.seasonSource", { source: formatText(line.seasonSource) })}</span>
                </>
            ),
        },
    ];

    return (
        <Card as="details" padding="none" className="px-5 py-3">
            <summary className="cursor-pointer py-1 text-sm font-semibold text-text">{t("varieties.comparison.howComputed")}</summary>
            <Table
                className="mt-3 whitespace-nowrap"
                caption={t("varieties.comparison.howComputed")}
                columns={columns}
                rows={lines}
                getRowKey={(line) => line.varietyId}
            />
        </Card>
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
