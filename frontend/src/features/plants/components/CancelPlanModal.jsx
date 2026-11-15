import { X } from "lucide-react";
import { formatBlock, formatDate, formatQuantity, formatText } from "../utils/format";
import PlantsModal from "./PlantsModal";
import { PRIMARY_BUTTON, SECONDARY_BUTTON } from "./plantsStyles";

/**
 * Confirmation before a planned application is cancelled. Cancelling is
 * final and records nothing in the stock; the plan stays in the list as
 * cancelled and no longer counts in the comparison or in the stock warning.
 *
 * Whatever the API refuses is shown above the buttons: a 409 means the plan
 * was done or cancelled meanwhile, and the plans are refreshed so its row
 * shows it.
 */
export default function CancelPlanModal({ plan, isSubmitting, error, onConfirm, onClose }) {
    return (
        <PlantsModal labelledBy="cancel-plan-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h2 id="cancel-plan-title" className="font-heading text-lg font-bold text-on-background">
                    Cancel this planned application?
                </h2>
                <button
                    type="button"
                    onClick={onClose}
                    aria-label="Close"
                    className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-gray-800"
                >
                    <X size={20} />
                </button>
            </div>

            <div className="px-6 py-5">
                {/* #9a3f32 on the tinted alert (#fcf6f5) -> 6.27:1, as in the movement form. */}
                {error && (
                    <p
                        role="alert"
                        className="mb-5 rounded-lg border border-error/30 bg-error/5 px-3 py-2 text-sm text-[#9a3f32]"
                    >
                        {error.message}
                    </p>
                )}

                <p className="text-sm font-semibold text-gray-800">
                    {`${formatText(plan.fertilizerName)} · ${formatBlock(plan.blockCode)} · `
                        + `${formatQuantity(plan.plannedQuantity, plan.unit)} on ${formatDate(plan.plannedDate)}`}
                </p>
                {/* gray-600 #4a5565 on white -> 7.56:1. */}
                <p className="mt-2 text-sm text-gray-600">
                    Nothing is taken from the stock. A cancelled application cannot be planned again: plan a new one
                    instead.
                </p>

                <div className="mt-6 flex justify-end gap-2">
                    <button type="button" autoFocus onClick={onClose} className={SECONDARY_BUTTON}>
                        Keep it
                    </button>
                    <button
                        type="button"
                        disabled={isSubmitting}
                        onClick={() => onConfirm(plan.id)}
                        className={PRIMARY_BUTTON}
                    >
                        {isSubmitting ? "Cancelling…" : "Cancel application"}
                    </button>
                </div>
            </div>
        </PlantsModal>
    );
}
