import { Pencil, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { IconButton, StatusBadge, Table } from "../../../shared/ui";
import { STATUS_TONES } from "../constants";
import { useEnumLabels } from "../format";

function EquipmentTable({ equipments = [], onEditStatus, onDelete, loading = false }) {
  const { t } = useTranslation("machinery");
  const enumLabels = useEnumLabels();
  const { t: tCommon } = useTranslation("common");

  const columns = [
    { key: "name", header: t("fields.name") },
    { key: "category", header: t("equipment.category"), cell: (row) => enumLabels.category(row.category) },
    { key: "stage", header: t("equipment.stage"), cell: (row) => enumLabels.stage(row.stage) },
    {
      key: "status",
      header: t("equipment.status"),
      cell: (row) => (
        <StatusBadge tone={STATUS_TONES[row.status] ?? "neutral"} label={enumLabels.status(row.status)} />
      ),
    },
  ];

  return (
    <Table
      caption={t("equipment.title")}
      columns={columns}
      rows={equipments}
      loading={loading}
      rowActions={(equipment) => (
        <>
          <IconButton icon={Pencil} label={t("equipment.editStatus")} onClick={() => onEditStatus(equipment)} />
          <IconButton icon={Trash2} label={tCommon("actions.delete")} tone="danger" onClick={() => onDelete(equipment)} />
        </>
      )}
    />
  );
}
export default EquipmentTable;
