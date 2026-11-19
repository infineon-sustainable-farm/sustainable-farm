import { useTranslation } from "react-i18next";
import { ConfirmDialog } from "../../../shared/ui";
import { formatBlock, formatDate, formatQuantity, formatText } from "../utils/format";

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
    const { t } = useTranslation("plants");
    return (
        <ConfirmDialog
            open
            onClose={onClose}
            onConfirm={() => onConfirm(plan.id)}
            title={t("fertilizer.plans.cancelForm.title")}
            message={
                <>
                    <span className="block font-semibold text-text">
                        {t("fertilizer.plans.cancelForm.summary", {
                            fertilizer: formatText(plan.fertilizerName),
                            block: formatBlock(plan.blockCode),
                            quantity: formatQuantity(plan.plannedQuantity, plan.unit),
                            date: formatDate(plan.plannedDate),
                        })}
                    </span>
                    <span className="mt-2 block">{t("fertilizer.plans.cancelForm.message")}</span>
                </>
            }
            confirmLabel={isSubmitting ? t("fertilizer.plans.cancelForm.confirming") : t("fertilizer.plans.cancelForm.confirm")}
            cancelLabel={t("fertilizer.plans.cancelForm.keep")}
            variant="primary"
            loading={isSubmitting}
            error={error?.message}
        />
    );
}
