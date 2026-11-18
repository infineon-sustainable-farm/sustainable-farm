import { Pencil, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { IconButton, Table } from "../../../shared/ui";
import { useMachineryFormat } from "../format";

function RepairLogTable({ repairLogs = [], getEquipmentName, onEdit, onDelete, loading = false }) {
  const { t } = useTranslation("machinery");
  const { t: tCommon } = useTranslation("common");
  const format = useMachineryFormat();

  const columns = [
    {
      key: "date",
      header: t("fields.date"),
      className: "whitespace-nowrap",
      // The kit sets the first column in medium weight; this list keeps regular text.
      cell: (row) => <span className="font-normal">{format.date(row.date)}</span>,
    },
    { key: "equipment", header: t("fields.equipment"), cell: (row) => getEquipmentName(row.equipmentId) },
    {
      key: "issue",
      header: t("repair.issue"),
      className: "max-w-xs truncate",
      cell: (row) => <span title={row.issue}>{row.issue}</span>,
    },
    {
      key: "downtime",
      header: t("repair.downtime"),
      className: "whitespace-nowrap",
      cell: (row) => t("units.hours", { value: format.decimal(row.downtime) }),
    },
    { key: "cost", header: t("fields.cost"), cell: (row) => format.currency(row.cost) },
    { key: "technician", header: t("repair.technician"), cell: (row) => row.technician ?? format.none },
  ];

  return (
    <Table
      caption={t("repair.title")}
      columns={columns}
      rows={repairLogs}
      loading={loading}
      rowActions={(repairLog) => (
        <>
          <IconButton icon={Pencil} label={t("repair.edit")} onClick={() => onEdit(repairLog)} />
          <IconButton icon={Trash2} label={tCommon("actions.delete")} tone="danger" onClick={() => onDelete(repairLog)} />
        </>
      )}
    />
  );
}
export default RepairLogTable;
