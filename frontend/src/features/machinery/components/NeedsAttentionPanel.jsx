import { CheckCircle2 } from "lucide-react";
import { formatDate } from "../../../shared/utils/formatDate";
import { formatDecimal } from "../../../shared/utils/formatDecimal";

function NeedsAttentionPanel({ overdueSchedules, lowStockList, getEquipmentName }) {
  const hasAlerts = overdueSchedules.length > 0 || lowStockList.length > 0;

  return (
    <section
      className="animate-fade-up bg-white rounded-2xl border border-gray-100 shadow-sm p-5"
      style={{ animationDelay: "400ms" }}
    >
      <h2 className="text-lg font-semibold">Needs attention</h2>
      <p className="text-sm text-gray-500 mb-4">Overdue maintenance and parts to reorder</p>

      {!hasAlerts && (
        <div className="flex flex-col items-center justify-center py-10 text-center text-gray-500">
          <CheckCircle2 size={28} className="text-green-600 mb-2" />
          <p className="text-sm">All caught up — nothing overdue and no low stock.</p>
        </div>
      )}

      {overdueSchedules.length > 0 && (
        <div className="mb-5">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-gray-400 mb-2">Overdue maintenance</h3>
          <ul className="flex flex-col gap-2">
            {overdueSchedules.slice(0, 4).map((schedule) => (
              <li key={schedule.id} className="flex items-center justify-between gap-3 text-sm">
                <span className="truncate font-medium text-gray-700">
                  {getEquipmentName(schedule.equipmentId)}
                </span>
                <span className="shrink-0 text-xs text-gray-400">{formatDate(schedule.nextDue)}</span>
                <span className="shrink-0 rounded-full bg-red-100 px-2 py-0.5 text-xs font-semibold text-red-700">
                  {Math.abs(schedule.dueInDays)}d late
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {lowStockList.length > 0 && (
        <div>
          <h3 className="text-xs font-semibold uppercase tracking-wide text-gray-400 mb-2">Low stock parts</h3>
          <ul className="flex flex-col gap-2">
            {lowStockList.map((sparePart) => (
              <li key={sparePart.id} className="flex items-center justify-between gap-3 text-sm">
                <span className="truncate font-medium text-gray-700">{sparePart.name}</span>
                <span className="shrink-0 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-700">
                  {formatDecimal(sparePart.quantity, 0)} / {formatDecimal(sparePart.reorderThreshold, 0)}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
export default NeedsAttentionPanel;
