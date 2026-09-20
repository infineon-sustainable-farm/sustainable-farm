import { Pencil, Trash2 } from "lucide-react";
import { formatDate } from "../../../shared/utils/formatDate";
import { formatCurrency, formatDecimal } from "../../../shared/utils/formatDecimal";

function FuelLogTable({ fuelLogs, getEquipmentName, onEdit, onDelete }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="border-b border-gray-200 text-sm text-gray-500">
            <th className="py-2">Date</th>
            <th className="py-2">Equipment</th>
            <th className="py-2">Liters</th>
            <th className="py-2">Cost</th>
            <th className="py-2">Actions</th>
          </tr>
        </thead>
        <tbody>
          {fuelLogs.map((fuelLog) => (
            <tr key={fuelLog.id} className="border-b border-gray-100">
              <td className="py-2 whitespace-nowrap">{formatDate(fuelLog.date)}</td>
              <td className="py-2">{getEquipmentName(fuelLog.equipmentId)}</td>
              <td className="py-2">{formatDecimal(fuelLog.liters)} L</td>
              <td className="py-2">{formatCurrency(fuelLog.cost)}</td>
              <td className="py-2 space-x-2">
                <button type="button" onClick={() => onEdit(fuelLog)} className="text-primary hover:opacity-70" title="Edit fuel log">
                  <Pencil size={16} />
                </button>
                <button type="button" onClick={() => onDelete(fuelLog)} className="text-red-600 hover:opacity-70" title="Delete">
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
export default FuelLogTable;
