import { CheckCircle2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Badge, Card } from "../../../shared/ui";
import { useMachineryFormat } from "../format";

function NeedsAttentionPanel({ overdueSchedules, lowStockList, getEquipmentName }) {
  const { t } = useTranslation("machinery");
  const format = useMachineryFormat();
  const hasAlerts = overdueSchedules.length > 0 || lowStockList.length > 0;

  return (
    <Card title={t("dashboard.attentionTitle")} subtitle={t("dashboard.attentionSubtitle")}>
      {!hasAlerts && (
        <div className="flex flex-col items-center justify-center py-10 text-center text-text-muted">
          <CheckCircle2 size={28} aria-hidden="true" className="text-success-fg mb-2" />
          <p className="text-sm">{t("dashboard.allClear")}</p>
        </div>
      )}

      {overdueSchedules.length > 0 && (
        <div className="mb-5">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-text-muted mb-2">
            {t("dashboard.overdueMaintenance")}
          </h3>
          <ul className="flex flex-col gap-2">
            {overdueSchedules.slice(0, 4).map((schedule) => (
              <li key={schedule.id} className="flex items-center justify-between gap-3 text-sm">
                <span className="truncate font-medium text-text">{getEquipmentName(schedule.equipmentId)}</span>
                <span className="shrink-0 text-xs text-text-muted">{format.date(schedule.nextDue)}</span>
                <Badge tone="error" className="shrink-0">
                  {t("dashboard.daysLate", { number: Math.abs(schedule.dueInDays) })}
                </Badge>
              </li>
            ))}
          </ul>
        </div>
      )}

      {lowStockList.length > 0 && (
        <div>
          <h3 className="text-xs font-semibold uppercase tracking-wide text-text-muted mb-2">
            {t("dashboard.lowStockParts")}
          </h3>
          <ul className="flex flex-col gap-2">
            {lowStockList.map((sparePart) => (
              <li key={sparePart.id} className="flex items-center justify-between gap-3 text-sm">
                <span className="truncate font-medium text-text">{sparePart.name}</span>
                <Badge tone="warning" className="shrink-0">
                  {t("dashboard.stockRatio", {
                    quantity: format.decimal(sparePart.quantity, 0),
                    threshold: format.decimal(sparePart.reorderThreshold, 0),
                  })}
                </Badge>
              </li>
            ))}
          </ul>
        </div>
      )}
    </Card>
  );
}
export default NeedsAttentionPanel;
