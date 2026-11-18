import { Pencil, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { IconButton, Table } from "../../../shared/ui";
import { useMachineryFormat } from "../format";

function OperatorAssignmentTable({ assignments = [], getEquipmentName, onEdit, onDelete, loading = false }) {
  const { t } = useTranslation("machinery");
  const { t: tCommon } = useTranslation("common");
  const format = useMachineryFormat();

  const columns = [
    { key: "fullName", header: t("fields.operator") },
    { key: "jobTitle", header: t("operators.jobTitle") },
    { key: "equipment", header: t("fields.equipment"), cell: (row) => getEquipmentName(row.equipmentId) },
    { key: "startDate", header: t("operators.startDate"), cell: (row) => format.date(row.startDate) },
    { key: "endDate", header: t("operators.endDate"), cell: (row) => format.date(row.endDate) },
  ];

  return (
    <Table
      caption={t("operators.title")}
      columns={columns}
      rows={assignments}
      loading={loading}
      rowActions={(assignment) => (
        <>
          <IconButton icon={Pencil} label={t("operators.edit")} onClick={() => onEdit(assignment)} />
          <IconButton icon={Trash2} label={tCommon("actions.delete")} tone="danger" onClick={() => onDelete(assignment)} />
        </>
      )}
    />
  );
}
export default OperatorAssignmentTable;
