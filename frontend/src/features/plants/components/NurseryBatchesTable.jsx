import { useTranslation } from "react-i18next";
import { Table } from "../../../shared/ui";
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

/**
 * Stage of a batch, with the day it was reached. A batch with no plant left
 * keeps its last stage at the API, which no longer says anything useful: it
 * reads "No plants left" instead. That is a display rule on the count the API
 * sends, not a computation.
 */
function StageCell({ batch }) {
    const { t } = useTranslation("plants");
    if (batch.currentCount === 0) {
        return <span className="text-text">{t("nursery.batches.noPlantsLeft")}</span>;
    }
    const since = formatDate(batch.currentStageSince);
    return (
        <>
            <NurseryStageBadge stage={batch.currentStage} />
            {since !== NO_VALUE && (
                <p className="mt-1 text-xs text-text-muted">{t("nursery.batches.since", { date: since })}</p>
            )}
        </>
    );
}

/** Origin of a batch; a purchased one also names its supplier and, if any, the supplier's lot number. */
function OriginCell({ batch }) {
    const { t } = useTranslation("plants");
    const supplier = formatText(batch.supplier);
    const lotNumber = formatText(batch.supplierLotNumber);
    return (
        <>
            {formatOrigin(batch.origin)}
            {supplier !== NO_VALUE && (
                <p className="mt-1 text-xs text-text-muted">
                    {lotNumber === NO_VALUE ? supplier : t("nursery.batches.supplierLot", { supplier, lot: lotNumber })}
                </p>
            )}
        </>
    );
}

/**
 * Nursery batches, one row per batch, in the order the API sends them. Every
 * count, rate and stage is the API's. The columns are those of the mock-up,
 * with "Batch" for its "Lot", as the API's messages say, and the plants still
 * in the nursery where it showed the starting count.
 */
export default function NurseryBatchesTable({ batches = [], loading = false }) {
    const { t } = useTranslation("plants");
    const header = (key) => t(`nursery.batches.columns.${key}`);
    const columns = [
        { key: "batchCode", header: header("batch"), cell: (batch) => formatText(batch.batchCode) },
        { key: "varietyName", header: header("variety"), cell: (batch) => formatText(batch.varietyName) },
        { key: "currentCount", header: header("inNursery"), cell: (batch) => formatNumber(batch.currentCount) },
        {
            key: "plannedBlockCode",
            header: header("destBlock"),
            cell: (batch) => <Value>{formatBlock(batch.plannedBlockCode)}</Value>,
        },
        { key: "currentStage", header: header("stage"), cell: (batch) => <StageCell batch={batch} /> },
        {
            key: "plannedTransplantOn",
            header: header("plannedTransplant"),
            cell: (batch) => formatDate(batch.plannedTransplantOn),
        },
        { key: "survivalRatePct", header: header("survivalRate"), cell: (batch) => formatPercent(batch.survivalRatePct) },
        { key: "origin", header: header("origin"), cell: (batch) => <OriginCell batch={batch} /> },
    ];
    return <Table caption={t("nursery.batches.caption")} columns={columns} rows={batches} loading={loading} />;
}
