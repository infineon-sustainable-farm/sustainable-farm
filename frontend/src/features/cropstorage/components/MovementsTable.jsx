import { useTranslation } from "react-i18next";
import { Table } from "../../../shared/ui";
import { GROUP, enumLabel } from "../utils/enums";
import { EMPTY, formatDateTime, formatQuantity } from "../utils/formatters";

function zoneName(zone) {
    return zone?.name ?? zone?.zoneCode ?? EMPTY;
}

export default function MovementsTable({ movements }) {
    const { t } = useTranslation("cropstorage");
    const columns = [
        { key: "id", header: t("movements.columns.id") },
        { key: "type", header: t("movements.columns.type"), cell: (m) => enumLabel(t, GROUP.movementType, m.type) },
        { key: "quantityKg", header: t("movements.columns.quantity"), align: "right", cell: (m) => formatQuantity(m.quantityKg ?? 0) },
        { key: "fromZone", header: t("movements.columns.fromZone"), cell: (m) => zoneName(m.fromZone) },
        { key: "toZone", header: t("movements.columns.toZone"), cell: (m) => zoneName(m.toZone) },
        { key: "reason", header: t("movements.columns.reason"), cell: (m) => m.reason || EMPTY },
        { key: "movedAt", header: t("movements.columns.movedAt"), cell: (m) => formatDateTime(m.movedAt) },
    ];
    return <Table columns={columns} rows={movements} caption={t("movements.tableCaption")} />;
}
