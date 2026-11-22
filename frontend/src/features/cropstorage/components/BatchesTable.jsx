import { useTranslation } from "react-i18next";
import { Table } from "../../../shared/ui";
import { GROUP, enumLabel } from "../utils/enums";
import { EMPTY, formatDate, formatQuantity } from "../utils/formatters";
import { varietyLabel } from "../utils/varieties";

export default function BatchesTable({ batches }) {
    const { t } = useTranslation("cropstorage");
    const columns = [
        { key: "batchCode", header: t("batches.columns.batchCode"), sortable: true },
        { key: "productType", header: t("batches.columns.product"), cell: (b) => enumLabel(t, GROUP.productType, b.productType) },
        { key: "mangoVariety", header: t("batches.columns.variety"), cell: (b) => varietyLabel(t, b.varietyKey, b.mangoVariety) ?? EMPTY },
        { key: "productStage", header: t("batches.columns.stage"), cell: (b) => enumLabel(t, GROUP.productStage, b.productStage) },
        { key: "status", header: t("batches.columns.status"), cell: (b) => enumLabel(t, GROUP.batchStatus, b.status) },
        {
            key: "currentQuantityKg",
            header: t("batches.columns.currentStock"),
            align: "right",
            sortable: true,
            sortValue: (b) => Number(b.currentQuantityKg ?? 0),
            cell: (b) => formatQuantity(b.currentQuantityKg ?? 0),
        },
        {
            key: "storageZone",
            header: t("batches.columns.zone"),
            cell: (b) => b.storageZone?.name ?? b.storageZone?.zoneCode ?? EMPTY,
        },
        { key: "expiryDate", header: t("batches.columns.expiry"), sortable: true, cell: (b) => formatDate(b.expiryDate) },
    ];
    return <Table columns={columns} rows={batches} getRowKey={(b) => b.id ?? b.batchCode} caption={t("batches.tableCaption")} />;
}
