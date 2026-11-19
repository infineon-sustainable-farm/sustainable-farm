import { Pencil, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Badge, IconButton, Table } from "../../../shared/ui";
import { daysUntil } from "../dashboardMetrics";
import { useEnumLabels, useMachineryFormat } from "../format";

/** Badge under the next due date: overdue (red), due within 7 days (amber), later (gray). */
function DueBadge({ nextDue }) {
  const { t } = useTranslation("machinery");
  const diff = daysUntil(nextDue);
  if (diff === null) return null;
  if (diff < 0) return <Badge tone="error">{t("maintenance.overdueBy", { number: Math.abs(diff) })}</Badge>;
  if (diff <= 7) return <Badge tone="warning">{t("maintenance.dueIn", { number: diff })}</Badge>;
  return <Badge tone="neutral">{t("maintenance.inDays", { number: diff })}</Badge>;
}

function MaintenanceScheduleTable({ schedules = [], getEquipmentName, onEdit, onDelete, loading = false }) {
  const { t } = useTranslation("machinery");
  const { t: tCommon } = useTranslation("common");
  const format = useMachineryFormat();
  const enumLabels = useEnumLabels();

  const columns = [
    {
      key: "type",
      header: t("fields.type"),
      // The kit sets the first column in medium weight; this list keeps regular text.
      cell: (row) => <span className="font-normal">{enumLabels.maintenanceType(row.type)}</span>,
    },
    { key: "equipment", header: t("fields.equipment"), cell: (row) => getEquipmentName(row.equipmentId) },
    { key: "frequency", header: t("maintenance.frequency"), cell: (row) => row.frequency ?? format.none },
    {
      key: "lastCompleted",
      header: t("maintenance.lastCompleted"),
      className: "whitespace-nowrap",
      cell: (row) => format.date(row.lastCompleted),
    },
    {
      key: "nextDue",
      header: t("maintenance.nextDue"),
      className: "whitespace-nowrap",
      cell: (row) => (
        <>
          <div>{format.date(row.nextDue)}</div>
          <DueBadge nextDue={row.nextDue} />
        </>
      ),
    },
    { key: "operator", header: t("fields.operator"), cell: (row) => row.operator ?? format.none },
  ];

  return (
    <Table
      caption={t("maintenance.title")}
      columns={columns}
      rows={schedules}
      loading={loading}
      rowActions={(schedule) => (
        <>
          <IconButton icon={Pencil} label={t("maintenance.edit")} onClick={() => onEdit(schedule)} />
          <IconButton icon={Trash2} label={tCommon("actions.delete")} tone="danger" onClick={() => onDelete(schedule)} />
        </>
      )}
    />
  );
}
export default MaintenanceScheduleTable;
