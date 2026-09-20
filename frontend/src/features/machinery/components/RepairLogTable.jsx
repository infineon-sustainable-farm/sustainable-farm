import { Pencil, Trash2 } from "lucide-react";
import { formatDate } from "../../../shared/utils/formatDate";
import { formatCurrency, formatDecimal } from "../../../shared/utils/formatDecimal";

function RepairLogTable({ repairLogs, getEquipmentName, onEdit, onDelete }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="border-b border-gray-200 text-sm text-gray-500">
            <th className="py-2">Date</th>
            <th className="py-2">Equipment</th>
            <th className="py-2">Issue</th>
            <th className="py-2">Downtime</th>
            <th className="py-2">Cost</th>
            <th className="py-2">Technician</th>
            <th className="py-2">Actions</th>
          </tr>
        </thead>
        <tbody>
          {repairLogs.map((repairLog) => (
            <tr key={repairLog.id} className="border-b border-gray-100">
              <td className="py-2 whitespace-nowrap">{formatDate(repairLog.date)}</td>
              <td className="py-2">{getEquipmentName(repairLog.equipmentId)}</td>
              <td className="py-2 max-w-xs truncate" title={repairLog.issue}>
                {repairLog.issue}
              </td>
              <td className="py-2 whitespace-nowrap">{formatDecimal(repairLog.downtime)} h</td>
              <td className="py-2">{formatCurrency(repairLog.cost)}</td>
              <td className="py-2">{repairLog.technician ?? "—"}</td>
              <td className="py-2 space-x-2">
                <button type="button" onClick={() => onEdit(repairLog)} className="text-primary hover:opacity-70" title="Edit repair log">
                  <Pencil size={16} />
                </button>
                <button type="button" onClick={() => onDelete(repairLog)} className="text-red-600 hover:opacity-70" title="Delete">
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
export default RepairLogTable;
