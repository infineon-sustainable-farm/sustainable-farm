import { Check, X } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Badge, Button, Card, Table } from "../../../shared/ui";
import { formatBlock, formatDate, formatQuantity, formatText } from "../utils/format";
import { planBadge, planDelayLabel } from "../utils/fertilizerPlans";
import CellNote from "./CellNote";
import PlanCost from "./PlanCost";


/**
 * Planned applications, one row each, in the order the API sends them. The
 * delay, the late flag and the costs are the API's own; nothing is computed
 * here. A planned row offers "Mark done" and "Cancel"; a done one shows what
 * was really applied. The unit sits in each cell rather than in the header:
 * the plans can mix kilograms and litres.
 */
export default function FertilizerPlansTable({ plans, onComplete, onCancel }) {
    const { t } = useTranslation("plants");
    const header = (key) => t(`fertilizer.plans.columns.${key}`);
    const planLabel = (plan) => t("fertilizer.plans.planLabel", {
        fertilizer: formatText(plan.fertilizerName),
        block: formatBlock(plan.blockCode),
        date: formatDate(plan.plannedDate),
    });

    const columns = [
        {
            key: "plannedDate",
            header: header("plannedDate"),
            className: "whitespace-nowrap align-top",
            cell: (plan) => {
                const delay = planDelayLabel(plan);
                return (
                    <>
                        <span className="font-semibold">{formatDate(plan.plannedDate)}</span>
                        {delay && <CellNote>{delay}</CellNote>}
                    </>
                );
            },
        },
        { key: "blockCode", header: header("block"), className: "whitespace-nowrap align-top", cell: (plan) => formatBlock(plan.blockCode) },
        {
            key: "fertilizerName",
            header: header("fertilizer"),
            className: "align-top",
            cell: (plan) => (
                <>
                    {formatText(plan.fertilizerName)}
                    {plan.note && <CellNote className="max-w-56">{plan.note}</CellNote>}
                </>
            ),
        },
        {
            key: "plannedQuantity",
            header: header("quantity"),
            align: "right",
            className: "whitespace-nowrap align-top",
            cell: (plan) => formatQuantity(plan.plannedQuantity, plan.unit),
        },
        {
            key: "plannedCost",
            header: header("plannedCost"),
            align: "right",
            className: "whitespace-nowrap align-top",
            cell: (plan) => <PlanCost xof={plan.plannedCostXof} eur={plan.plannedCostEur} />,
        },
        {
            key: "status",
            header: header("status"),
            className: "whitespace-nowrap align-top",
            cell: (plan) => {
                const badge = planBadge(plan);
                return (
                    <>
                        <Badge tone={badge.tone}>{badge.label}</Badge>
                        {plan.status === "DONE" && (
                            <CellNote className="mt-1">
                                {t("fertilizer.plans.applied", {
                                    quantity: formatQuantity(plan.actualQuantity, plan.unit),
                                    date: formatDate(plan.actualDate),
                                })}
                            </CellNote>
                        )}
                    </>
                );
            },
        },
    ];

    return (
        <Card>
            <Table
                caption={t("fertilizer.plans.title")}
                columns={columns}
                rows={plans}
                actionsHeader={header("actions")}
                rowActions={(plan) =>
                    plan.status === "PLANNED" && (
                        <div className="flex gap-2">
                            <Button
                                size="sm"
                                variant="secondary"
                                icon={Check}
                                onClick={() => onComplete(plan)}
                                aria-label={t("fertilizer.plans.markDoneLabel", { plan: planLabel(plan) })}
                            >
                                {t("fertilizer.plans.markDone")}
                            </Button>
                            <Button
                                size="sm"
                                variant="secondary"
                                icon={X}
                                onClick={() => onCancel(plan)}
                                aria-label={t("fertilizer.plans.cancelLabel", { plan: planLabel(plan) })}
                            >
                                {t("fertilizer.plans.cancel")}
                            </Button>
                        </div>
                    )
                }
            />
        </Card>
    );
}
