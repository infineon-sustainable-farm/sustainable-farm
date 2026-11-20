import { useState } from "react";
import { Plus } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Card, EmptyState, Input, Select } from "../../../shared/ui";
import {
    useCancelFertilizerPlan,
    useCompleteFertilizerPlan,
    usePlanFertilizerApplication,
} from "../hooks/useFertilizerPlanActions";
import {
    useFertilizerPlanComparison,
    useFertilizerPlans,
    useFertilizerPlanStockCheck,
} from "../hooks/useFertilizerPlans";
import { formatDate } from "../utils/format";
import { PLAN_STATUSES, planStatusLabel, stockWarningText } from "../utils/fertilizerPlans";
import CancelPlanModal from "./CancelPlanModal";
import CompletePlanModal from "./CompletePlanModal";
import FertilizerPlanComparisonTable from "./FertilizerPlanComparisonTable";
import FertilizerPlansTable from "./FertilizerPlansTable";
import { SectionLoadError, SectionLoading } from "./FertilizerSectionStatus";
import FormAlert from "./FormAlert";
import PlanApplicationModal from "./PlanApplicationModal";

/**
 * One filter on a title row. Its accessible name joins the title it filters
 * and its own label, so "From" here is told apart from any other "From".
 */
function FilterField({ id, titleId, label, children }) {
    return (
        <div className="flex max-w-full min-w-0 items-center gap-3">
            <label id={`${id}-label`} htmlFor={id} className="shrink-0 text-sm font-medium">
                {label}
            </label>
            <div className="min-w-0 max-w-full">{children({ id, "aria-labelledby": `${titleId} ${id}-label` })}</div>
        </div>
    );
}

/** The period the API compared, in words: both ends, or the one that is set. */
function periodLabel(comparison, t) {
    const { from, to } = comparison;
    if (from && to) return t("format.range", { start: formatDate(from), end: formatDate(to) });
    if (from) return t("fertilizer.plans.comparison.periodFrom", { date: formatDate(from) });
    if (to) return t("fertilizer.plans.comparison.periodUntil", { date: formatDate(to) });
    return t("fertilizer.plans.comparison.periodAll");
}

/**
 * Applications of fertilizer planned ahead: a warning when the plans of the
 * next 30 days exceed a stock, the plans with their cost, and the planned
 * quantities against the applied ones, by block.
 *
 * Every figure comes from the API: the delay and the late flag of a plan,
 * the costs at the last purchase price, the totals of the comparison, the
 * warning and the day it was computed for, which the forms take as today.
 * No quantity is recommended: the farmer enters each one.
 *
 * The section opens its own dialogs. Marking a plan as done records a real
 * application, so the stock and the history above and below refresh with it.
 */
export default function FertilizerPlansSection() {
    const { t } = useTranslation("plants");
    // Raw values as the API takes them; an empty string means "no filter".
    const [status, setStatus] = useState("");
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");
    const [isPlanning, setIsPlanning] = useState(false);
    const [planToComplete, setPlanToComplete] = useState(null);
    const [planToCancel, setPlanToCancel] = useState(null);

    const plans = useFertilizerPlans({ status: status || null });
    const comparison = useFertilizerPlanComparison({ from: from || null, to: to || null });
    const stockCheck = useFertilizerPlanStockCheck();
    const today = stockCheck.data?.today ?? null;
    const warnings = stockCheck.data?.warnings ?? [];

    const planApplication = usePlanFertilizerApplication();
    const completePlan = useCompleteFertilizerPlan();
    const cancelPlan = useCancelFertilizerPlan();

    function openPlanForm() {
        planApplication.reset();
        setIsPlanning(true);
    }

    function closePlanForm() {
        planApplication.reset();
        setIsPlanning(false);
    }

    function openCompletion(plan) {
        completePlan.reset();
        setPlanToComplete(plan);
    }

    function closeCompletion() {
        completePlan.reset();
        setPlanToComplete(null);
    }

    function openCancellation(plan) {
        cancelPlan.reset();
        setPlanToCancel(plan);
    }

    function closeCancellation() {
        cancelPlan.reset();
        setPlanToCancel(null);
    }

    const planButton = (
        <Button variant="secondary" icon={Plus} onClick={openPlanForm}>
            {t("fertilizer.plans.planAction")}
        </Button>
    );

    return (
        <section aria-labelledby="planned-applications-title" className="flex flex-col gap-4">
            <div className="flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
                <div className="min-w-0">
                    <h2 id="planned-applications-title" className="text-lg font-semibold">
                        {t("fertilizer.plans.title")}
                    </h2>
                    <p className="mt-1 text-sm text-text-muted">{t("fertilizer.plans.subtitle")}</p>
                </div>
                <div className="flex max-w-full flex-wrap items-center gap-x-6 gap-y-3">
                    <FilterField
                        id="planned-applications-status"
                        titleId="planned-applications-title"
                        label={t("fertilizer.plans.statusFilter")}
                    >
                        {(props) => (
                            <Select {...props} value={status} onChange={(event) => setStatus(event.target.value)}>
                                <option value="">{t("fertilizer.plans.allStatuses")}</option>
                                {PLAN_STATUSES.map((code) => (
                                    <option key={code} value={code}>
                                        {planStatusLabel(code)}
                                    </option>
                                ))}
                            </Select>
                        )}
                    </FilterField>
                    {planButton}
                </div>
            </div>

            {warnings.length > 0 && (
                <div role="status">
                    <FormAlert tone="warning">
                        <p className="font-semibold">
                            {t("fertilizer.plans.stockWarning.title", { days: stockCheck.data.days })}
                        </p>
                        <ul className="mt-1 flex flex-col gap-1">
                            {warnings.map((warning) => (
                                <li key={warning.fertilizerId}>{stockWarningText(warning, stockCheck.data.days)}</li>
                            ))}
                        </ul>
                    </FormAlert>
                </div>
            )}

            {plans.isPending && <SectionLoading>{t("fertilizer.plans.loading")}</SectionLoading>}

            {plans.isError && <SectionLoadError title={t("fertilizer.plans.loadError")} query={plans} />}

            {plans.isSuccess && plans.data.length === 0 && (
                <Card>
                    {status ? (
                        <EmptyState
                            className="py-8"
                            title={t("fertilizer.plans.emptyFiltered")}
                            message={t("fertilizer.plans.emptyFilteredHint")}
                        />
                    ) : (
                        <EmptyState className="py-8" message={t("fertilizer.plans.empty")} action={planButton} />
                    )}
                </Card>
            )}

            {plans.isSuccess && plans.data.length > 0 && (
                <FertilizerPlansTable plans={plans.data} onComplete={openCompletion} onCancel={openCancellation} />
            )}

            <div className="mt-2 flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
                <div className="min-w-0">
                    <h3 id="planned-vs-applied-title" className="text-base font-semibold">
                        {t("fertilizer.plans.comparison.title")}
                    </h3>
                    <p className="mt-1 text-xs text-text-muted">
                        {comparison.isSuccess
                            ? t("fertilizer.plans.comparison.subtitle", { period: periodLabel(comparison.data, t) })
                            : t("fertilizer.plans.comparison.subtitleNoPeriod")}
                    </p>
                </div>
                <div className="flex max-w-full flex-wrap items-center gap-x-6 gap-y-3">
                    <FilterField
                        id="planned-vs-applied-from"
                        titleId="planned-vs-applied-title"
                        label={t("fertilizer.plans.comparison.from")}
                    >
                        {(props) => (
                            <Input {...props} type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
                        )}
                    </FilterField>
                    <FilterField
                        id="planned-vs-applied-to"
                        titleId="planned-vs-applied-title"
                        label={t("fertilizer.plans.comparison.to")}
                    >
                        {(props) => (
                            <Input {...props} type="date" value={to} onChange={(event) => setTo(event.target.value)} />
                        )}
                    </FilterField>
                </div>
            </div>

            {comparison.isPending && <SectionLoading>{t("fertilizer.plans.comparison.loading")}</SectionLoading>}

            {comparison.isError && (
                <SectionLoadError title={t("fertilizer.plans.comparison.loadError")} query={comparison} />
            )}

            {comparison.isSuccess && comparison.data.rows.length === 0 && (
                <Card>
                    <EmptyState
                        className="py-8"
                        title={t("fertilizer.plans.comparison.empty")}
                        message={t("fertilizer.plans.comparison.emptyHint")}
                    />
                </Card>
            )}

            {comparison.isSuccess && comparison.data.rows.length > 0 && (
                <FertilizerPlanComparisonTable rows={comparison.data.rows} />
            )}

            {isPlanning && (
                <PlanApplicationModal
                    today={today}
                    isSubmitting={planApplication.isPending}
                    error={planApplication.error}
                    onSubmit={(body) => planApplication.mutate(body, { onSuccess: closePlanForm })}
                    onClose={closePlanForm}
                />
            )}

            {planToComplete && (
                <CompletePlanModal
                    plan={planToComplete}
                    today={today}
                    isSubmitting={completePlan.isPending}
                    error={completePlan.error}
                    onSubmit={(completion) => completePlan.mutate(completion, { onSuccess: closeCompletion })}
                    onClose={closeCompletion}
                />
            )}

            {planToCancel && (
                <CancelPlanModal
                    plan={planToCancel}
                    isSubmitting={cancelPlan.isPending}
                    error={cancelPlan.error}
                    onConfirm={(planId) => cancelPlan.mutate(planId, { onSuccess: closeCancellation })}
                    onClose={closeCancellation}
                />
            )}
        </section>
    );
}
