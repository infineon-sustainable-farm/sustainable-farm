import { Check, X } from "lucide-react";
import { formatBlock, formatDate, formatQuantity, formatText } from "../utils/format";
import { planBadge, planDelayLabel } from "../utils/fertilizerPlans";
import PlanCost from "./PlanCost";
import PlantsBadge from "./PlantsBadge";
import { CELL_NOTE, ROW_BUTTON, TABLE_CARD, TABLE_HEAD, TABLE_HEAD_NUM } from "./plantsStyles";

// The unit sits in each cell rather than in the header: the plans can mix
// kilograms and litres. The cost in euros sits under the cost in FCFA, and
// the note of a plan under its fertilizer.
const COLUMNS = [
    "Planned date",
    "Block",
    "Fertilizer",
    "Quantity",
    "Planned cost",
    "Status",
    "Actions",
];

// Quantities and amounts line up on the right, header included.
const NUMERIC = new Set(["Quantity", "Planned cost"]);

/**
 * Planned applications, one row each, in the order the API sends them. The
 * delay, the late flag and the costs are the API's own; nothing is computed
 * here. A planned row offers "Mark done" and "Cancel"; a done one shows what
 * was really applied.
 */
export default function FertilizerPlansTable({ plans, onComplete, onCancel }) {
    return (
        <div className={TABLE_CARD}>
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th
                                key={column}
                                scope="col"
                                className={NUMERIC.has(column) ? TABLE_HEAD_NUM : TABLE_HEAD}
                            >
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {plans.map((plan) => {
                        const badge = planBadge(plan);
                        const delay = planDelayLabel(plan);
                        const label = `${formatText(plan.fertilizerName)}, ${formatBlock(plan.blockCode)}, `
                            + formatDate(plan.plannedDate);
                        return (
                            <tr key={plan.id} className="border-b border-gray-100 align-top last:border-b-0">
                                <td className="px-4 py-3 font-semibold text-gray-800">
                                    {formatDate(plan.plannedDate)}
                                    {delay && <span className={`${CELL_NOTE} font-normal`}>{delay}</span>}
                                </td>
                                <td className="px-4 py-3 text-gray-700">
                                    {formatBlock(plan.blockCode)}
                                </td>
                                <td className="px-4 py-3 text-gray-700">
                                    {formatText(plan.fertilizerName)}
                                    {plan.note && (
                                        <span className={`${CELL_NOTE} w-56 whitespace-normal`}>{plan.note}</span>
                                    )}
                                </td>
                                <td className="px-4 py-3 text-right text-gray-700">
                                    {formatQuantity(plan.plannedQuantity, plan.unit)}
                                </td>
                                <td className="px-4 py-3 text-right text-gray-700">
                                    <PlanCost xof={plan.plannedCostXof} eur={plan.plannedCostEur} />
                                </td>
                                <td className="px-4 py-3 text-gray-700">
                                    <PlantsBadge tone={badge.tone}>{badge.label}</PlantsBadge>
                                    {plan.status === "DONE" && (
                                        <span className={`${CELL_NOTE} mt-1`}>
                                            {`Applied ${formatQuantity(plan.actualQuantity, plan.unit)} on `
                                                + formatDate(plan.actualDate)}
                                        </span>
                                    )}
                                </td>
                                <td className="px-4 py-3">
                                    {plan.status === "PLANNED" && (
                                        <div className="flex gap-2">
                                            <button
                                                type="button"
                                                onClick={() => onComplete(plan)}
                                                aria-label={`Mark done: ${label}`}
                                                className={ROW_BUTTON}
                                            >
                                                <Check size={14} />
                                                Mark done
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => onCancel(plan)}
                                                aria-label={`Cancel: ${label}`}
                                                className={ROW_BUTTON}
                                            >
                                                <X size={14} />
                                                Cancel
                                            </button>
                                        </div>
                                    )}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
