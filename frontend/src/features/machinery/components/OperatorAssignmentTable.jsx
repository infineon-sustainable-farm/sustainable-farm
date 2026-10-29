import { Pencil, Trash2 } from "lucide-react";
import { formatDate } from "../../../shared/utils/formatDate";

function OperatorAssignmentTable({ assignments, getEquipmentName, onEdit, onDelete }) {
  return (
    <table className="w-full text-left border-collapse">
      <thead>
        <tr className="border-b border-gray-200 text-sm text-gray-500">
          <th className="py-2">Operator</th>
          <th className="py-2">Job title</th>
          <th className="py-2">Equipment</th>
          <th className="py-2">Start date</th>
          <th className="py-2">End date</th>
          <th className="py-2">Actions</th>
        </tr>
      </thead>
      <tbody>
        {assignments.map((assignment, index) => (
          <tr
            key={assignment.id}
            className="border-b border-gray-100 animate-fade-up-sm"
            style={{ animationDelay: `${Math.min(index, 8) * 30}ms` }}
          >
            <td className="py-2 font-medium">{assignment.fullName}</td>
            <td className="py-2">{assignment.jobTitle}</td>
            <td className="py-2">{getEquipmentName(assignment.equipmentId)}</td>
            <td className="py-2">{formatDate(assignment.startDate)}</td>
            <td className="py-2">{formatDate(assignment.endDate)}</td>
            <td className="py-2 space-x-2">
              <button type="button" onClick={() => onEdit(assignment)} className="text-primary hover:opacity-70" title="Edit assignment">
                <Pencil size={16} />
              </button>
              <button type="button" onClick={() => onDelete(assignment)} className="text-red-600 hover:opacity-70" title="Delete">
                <Trash2 size={16} />
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
export default OperatorAssignmentTable;
