import { Pencil, Trash2 } from "lucide-react";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";
import { formatDate } from "../../../shared/utils/formatDate";
import DueBadge from "./DueBadge";

function MaintenanceScheduleTable({ schedules, getEquipmentName, onEdit, onDelete }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="border-b border-gray-200 text-sm text-gray-500">
            <th className="py-2">Type</th>
            <th className="py-2">Equipment</th>
            <th className="py-2">Frequency</th>
            <th className="py-2">Last completed</th>
            <th className="py-2">Next due</th>
            <th className="py-2">Operator</th>
            <th className="py-2">Actions</th>
          </tr>
        </thead>
        <tbody>
          {schedules.map((schedule, index) => (
            <tr
              key={schedule.id}
              className="border-b border-gray-100 animate-fade-up-sm"
              style={{ animationDelay: `${Math.min(index, 8) * 30}ms` }}
            >
              <td className="py-2">{formatEnumLabel(schedule.type)}</td>
              <td className="py-2">{getEquipmentName(schedule.equipmentId)}</td>
              <td className="py-2">{schedule.frequency ?? "—"}</td>
              <td className="py-2 whitespace-nowrap">{formatDate(schedule.lastCompleted)}</td>
              <td className="py-2 whitespace-nowrap">
                <div>{formatDate(schedule.nextDue)}</div>
                <DueBadge nextDue={schedule.nextDue} />
              </td>
              <td className="py-2">{schedule.operator ?? "—"}</td>
              <td className="py-2 space-x-2">
                <button type="button" onClick={() => onEdit(schedule)} className="text-primary hover:opacity-70" title="Edit maintenance schedule">
                  <Pencil size={16} />
                </button>
                <button type="button" onClick={() => onDelete(schedule)} className="text-red-600 hover:opacity-70" title="Delete">
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
export default MaintenanceScheduleTable;
