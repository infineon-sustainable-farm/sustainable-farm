import { Pencil, Trash2 } from "lucide-react";
import { formatDate } from "../../../shared/utils/formatDate";
import { formatDecimal } from "../../../shared/utils/formatDecimal";

function UsageLogTable({ usageLogs, getEquipmentName, onEdit, onDelete }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="border-b border-gray-200 text-sm text-gray-500">
            <th className="py-2">Date</th>
            <th className="py-2">Equipment</th>
            <th className="py-2">Hours used</th>
            <th className="py-2">Operator</th>
            <th className="py-2">Notes</th>
            <th className="py-2">Actions</th>
          </tr>
        </thead>
        <tbody>
          {usageLogs.map((usageLog, index) => (
            <tr
              key={usageLog.id}
              className="border-b border-gray-100 animate-fade-up-sm"
              style={{ animationDelay: `${Math.min(index, 8) * 30}ms` }}
            >
              <td className="py-2 whitespace-nowrap">{formatDate(usageLog.date)}</td>
              <td className="py-2">{getEquipmentName(usageLog.equipmentId)}</td>
              <td className="py-2">{formatDecimal(usageLog.hoursUsed)} h</td>
              <td className="py-2">{usageLog.operator ?? "—"}</td>
              <td className="py-2 max-w-xs truncate" title={usageLog.notes ?? ""}>
                {usageLog.notes ?? "—"}
              </td>
              <td className="py-2 space-x-2">
                <button type="button" onClick={() => onEdit(usageLog)} className="text-primary hover:opacity-70" title="Edit usage log">
                  <Pencil size={16} />
                </button>
                <button type="button" onClick={() => onDelete(usageLog)} className="text-red-600 hover:opacity-70" title="Delete">
                  <Trash2 size={16} />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
export default UsageLogTable;
