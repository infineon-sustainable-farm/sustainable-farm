import { Pencil, Trash2, TriangleAlert } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Badge, IconButton, Table } from "../../../shared/ui";
import { useMachineryFormat } from "../format";

function SparePartTable({ spareParts = [], getEquipmentName, onEdit, onDelete, loading = false }) {
  const { t } = useTranslation("machinery");
  const { t: tCommon } = useTranslation("common");
  const format = useMachineryFormat();

  const columns = [
    { key: "name", header: t("fields.name") },
    {
      key: "equipment",
      header: t("spareParts.compatibleAsset"),
      cell: (row) => getEquipmentName(row.equipmentId),
    },
    {
      key: "quantity",
      header: t("spareParts.quantity"),
      className: "whitespace-nowrap",
      cell: (row) => {
        const isLowStock = row.quantity <= row.reorderThreshold;
        return (
          <>
            <span className={isLowStock ? "font-semibold text-warning-fg" : ""}>{row.quantity}</span>
            {isLowStock && (
              <Badge
                tone="warning"
                icon={TriangleAlert}
                className="ml-2"
                title={t("spareParts.lowStockTitle", { threshold: row.reorderThreshold })}
              >
                {t("spareParts.lowStock")}
              </Badge>
            )}
          </>
        );
      },
    },
    { key: "reorderThreshold", header: t("spareParts.reorderThreshold") },
    { key: "unitCost", header: t("spareParts.unitCost"), cell: (row) => format.currency(row.unitCost) },
  ];

  return (
    <Table
      caption={t("spareParts.title")}
      columns={columns}
      rows={spareParts}
      loading={loading}
      rowActions={(sparePart) => (
        <>
          <IconButton icon={Pencil} label={t("spareParts.edit")} onClick={() => onEdit(sparePart)} />
          <IconButton icon={Trash2} label={tCommon("actions.delete")} tone="danger" onClick={() => onDelete(sparePart)} />
        </>
      )}
    />
  );
}
export default SparePartTable;
