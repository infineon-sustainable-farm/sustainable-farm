import { Pencil, Trash2, TriangleAlert } from "lucide-react";

function formatUnitCost(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return "—";
  return `$${amount.toFixed(2)}`;
}

function SparePartTable({ spareParts, getEquipmentName, onEdit, onDelete }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="border-b border-gray-200 text-sm text-gray-500">
            <th className="py-2">Name</th>
            <th className="py-2">Compatible asset</th>
            <th className="py-2">Quantity in stock</th>
            <th className="py-2">Reorder threshold</th>
            <th className="py-2">Unit cost</th>
            <th className="py-2">Actions</th>
          </tr>
        </thead>
        <tbody>
          {spareParts.map((sparePart, index) => {
            const isLowStock = sparePart.quantity <= sparePart.reorderThreshold;

            return (
              <tr
                key={sparePart.id}
                className="border-b border-gray-100 animate-fade-up-sm"
                style={{ animationDelay: `${Math.min(index, 8) * 30}ms` }}
              >
                <td className="py-2 font-medium">{sparePart.name}</td>
                <td className="py-2">{getEquipmentName(sparePart.equipmentId)}</td>
                <td className="py-2 whitespace-nowrap">
                  <span className={isLowStock ? "font-semibold text-amber-700" : ""}>
                    {sparePart.quantity}
                  </span>
                  {isLowStock && (
                    <span
                      className="ml-2 inline-flex items-center gap-1 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-700"
                      title={`At or below the reorder threshold (${sparePart.reorderThreshold})`}
                    >
                      <TriangleAlert size={12} />
                      Low stock
                    </span>
                  )}
                </td>
                <td className="py-2">{sparePart.reorderThreshold}</td>
                <td className="py-2">{formatUnitCost(sparePart.unitCost)}</td>
                <td className="py-2 space-x-2">
                  <button type="button" onClick={() => onEdit(sparePart)} className="text-primary hover:opacity-70" title="Edit spare part">
                    <Pencil size={16} />
                  </button>
                  <button type="button" onClick={() => onDelete(sparePart)} className="text-red-600 hover:opacity-70" title="Delete">
                    <Trash2 size={16} />
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
export default SparePartTable;
