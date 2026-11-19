import { Pencil, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { IconButton, Table } from "../../../shared/ui";
import { useMachineryFormat } from "../format";

function UsageLogTable({ usageLogs = [], getEquipmentName, onEdit, onDelete, loading = false }) {
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
      key: "hoursUsed",
      header: t("usage.hoursUsed"),
      cell: (row) => t("units.hours", { value: format.decimal(row.hoursUsed) }),
    },
    { key: "operator", header: t("fields.operator"), cell: (row) => row.operator ?? format.none },
    {
      key: "notes",
      header: t("fields.notes"),
      className: "max-w-xs truncate",
      cell: (row) => <span title={row.notes ?? ""}>{row.notes ?? format.none}</span>,
    },
  ];

  return (
    <Table
      caption={t("usage.title")}
      columns={columns}
      rows={usageLogs}
      loading={loading}
      rowActions={(usageLog) => (
        <>
          <IconButton icon={Pencil} label={t("usage.edit")} onClick={() => onEdit(usageLog)} />
          <IconButton icon={Trash2} label={tCommon("actions.delete")} tone="danger" onClick={() => onDelete(usageLog)} />
        </>
      )}
    />
  );
}
export default UsageLogTable;
