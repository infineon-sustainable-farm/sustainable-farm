import { Pencil, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { IconButton, Table } from "../../../shared/ui";
import { useMachineryFormat } from "../format";

function FuelLogTable({ fuelLogs = [], getEquipmentName, onEdit, onDelete, loading = false }) {
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
    { key: "liters", header: t("fuel.liters"), cell: (row) => t("units.liters", { value: format.decimal(row.liters) }) },
    { key: "cost", header: t("fields.cost"), cell: (row) => format.currency(row.cost) },
  ];

  return (
    <Table
      caption={t("fuel.title")}
      columns={columns}
      rows={fuelLogs}
      loading={loading}
      rowActions={(fuelLog) => (
        <>
          <IconButton icon={Pencil} label={t("fuel.edit")} onClick={() => onEdit(fuelLog)} />
          <IconButton icon={Trash2} label={tCommon("actions.delete")} tone="danger" onClick={() => onDelete(fuelLog)} />
        </>
      )}
    />
  );
}
export default FuelLogTable;
