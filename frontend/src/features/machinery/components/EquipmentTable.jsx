import { Pencil, Trash2 } from "lucide-react";
import { formatEnumLabel } from "../../../shared/utils/formatEnumLabel";
import StatusBadge from "./StatusBadge";

function EquipmentTable({ equipments, onEditStatus, onDelete }) {
  return (
    <table className="w-full text-left border-collapse">
      <thead>
        <tr className="border-b border-gray-200 text-sm text-gray-500">
          <th className="py-2">Name</th>
          <th className="py-2">Category</th>
          <th className="py-2">Stage</th>
          <th className="py-2">Status</th>
          <th className="py-2">Actions</th>
        </tr>
      </thead>
      <tbody>
        {equipments.map((equipment, index) => (
          <tr
            key={equipment.id}
            className="border-b border-gray-100 animate-fade-up-sm"
            style={{ animationDelay: `${Math.min(index, 8) * 30}ms` }}
          >
            <td className="py-2 font-medium">{equipment.name}</td>
            <td className="py-2">{formatEnumLabel(equipment.category)}</td>
            <td className="py-2">{formatEnumLabel(equipment.stage)}</td>
            <td className="py-2">
              <StatusBadge status={equipment.status} />
            </td>
            <td className="py-2 space-x-2">
              <button type="button" onClick={() => onEditStatus(equipment)} className="text-primary hover:opacity-70" title="Edit status">
                <Pencil size={16} />
              </button>
              <button type="button" onClick={() => onDelete(equipment)} className="text-red-600 hover:opacity-70" title="Delete">
                <Trash2 size={16} />
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
export default EquipmentTable;