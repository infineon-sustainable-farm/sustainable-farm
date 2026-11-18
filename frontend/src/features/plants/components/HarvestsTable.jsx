import { useTranslation } from "react-i18next";
import { Card, Table } from "../../../shared/ui";
import { formatBlock, formatDate, formatNumber, formatText } from "../utils/format";

const CELL = "whitespace-nowrap";

/**
 * Recorded harvests, one row per picking, in the order the API sends them. A
 * harvest recorded before lot numbers existed has none and shows a dash.
 */
export default function HarvestsTable({ harvests }) {
    const { t } = useTranslation("plants");
    const columns = [
        { key: "block", header: t("harvest.harvestsTable.block"), className: CELL, cell: (h) => formatBlock(h.blockCode) },
        { key: "variety", header: t("harvest.harvestsTable.variety"), className: CELL, cell: (h) => formatText(h.varietyName) },
        { key: "date", header: t("harvest.harvestsTable.harvestDate"), className: CELL, cell: (h) => formatDate(h.harvestDate) },
        { key: "quantity", header: t("harvest.harvestsTable.quantity"), className: CELL, cell: (h) => formatNumber(h.quantityKg) },
        { key: "lot", header: t("harvest.harvestsTable.lot"), className: CELL, cell: (h) => formatText(h.lotNumber) },
        { key: "source", header: t("harvest.harvestsTable.source"), className: CELL, cell: (h) => formatText(h.source) },
    ];
    return (
        <Card>
            <Table columns={columns} rows={harvests} caption={t("harvest.harvests.title")} className="text-sm" />
        </Card>
    );
}
