import { useState } from "react";
import { Loader2, Plus, TriangleAlert } from "lucide-react";
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
import { formatCode, formatDate } from "../utils/format";
import { PLAN_STATUSES, stockWarningText } from "../utils/fertilizerPlans";
import CancelPlanModal from "./CancelPlanModal";
import CompletePlanModal from "./CompletePlanModal";
import FertilizerPlanComparisonTable from "./FertilizerPlanComparisonTable";
import FertilizerPlansTable from "./FertilizerPlansTable";
import PlanApplicationModal from "./PlanApplicationModal";
import PlantsEmptyState from "./PlantsEmptyState";
import { CARD_FRAME, FIELD_INPUT, FIELD_LABEL, ICON_SIZE, PRIMARY_BUTTON, SECONDARY_BUTTON } from "./plantsStyles";

const CONTROL_CLASS = `min-w-0 max-w-full ${FIELD_INPUT}`;

/**
 * One filter on a title row. Its accessible name joins the title it filters
 * and its own label, so "From" here is told apart from any other "From".
 */
function FilterField({ id, titleId, label, children }) {
    return (
        <div className="flex max-w-full min-w-0 items-center gap-3">
            <label id={`${id}-label`} htmlFor={id} className={`${FIELD_LABEL} shrink-0`}>
                {label}
            </label>
            {children({ id, "aria-labelledby": `${titleId} ${id}-label`, className: CONTROL_CLASS })}
        </div>
    );
}

function LoadingCard({ children }) {
    return (
        <div className={`flex items-center justify-center gap-3 px-6 py-16 text-sm text-gray-500 ${CARD_FRAME}`}>
            <Loader2 size={18} className="animate-spin" />
            {children}
        </div>
    );
}

function ErrorCard({ title, onRetry, isRetrying }) {
    return (
        <div className="flex flex-col items-center gap-4 rounded-2xl border border-error/30 bg-error/5 px-6 py-16 text-center">
            <TriangleAlert size={28} className="text-error" />
            <div>
                <p className="font-heading text-lg font-bold text-on-background">{title}</p>
                {/* gray-600: #4a5565 on the tinted box (#f5f1f5) -> 6.76:1; gray-500 gave 4.32:1. */}
                <p className="mt-1 text-sm text-gray-600">The request to the server did not succeed.</p>
            </div>
            <button type="button" onClick={onRetry} disabled={isRetrying} className={PRIMARY_BUTTON}>
                {isRetrying ? "Retrying…" : "Retry"}
            </button>
        </div>
    );
}

/** The period the API compared, in words: both ends, or the one that is set. */
function periodLabel(comparison) {
    const { from, to } = comparison;
    if (from && to) return `${formatDate(from)} – ${formatDate(to)}`;
    if (from) return `From ${formatDate(from)}`;
    if (to) return `Until ${formatDate(to)}`;
    return "All dates";
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
        <button type="button" onClick={openPlanForm} className={SECONDARY_BUTTON}>
            <Plus size={16} />
            Plan an application
        </button>
    );

    return (
        <section aria-labelledby="planned-applications-title" className="flex flex-col gap-4">
            <div className="flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
                <div className="min-w-0">
                    <h2 id="planned-applications-title" className="font-heading text-lg font-bold text-on-background">
                        Planned applications
                    </h2>
                    {/* gray-600 #4a5565 on the page (#f8f9ff) -> 7.19:1. */}
                    <p className="mt-1 text-sm text-gray-600">
                        Your own quantities: no rate is recommended. Costs at last purchase price.
                    </p>
                </div>
                <div className="flex max-w-full flex-wrap items-center gap-x-6 gap-y-3">
                    <FilterField id="planned-applications-status" titleId="planned-applications-title" label="Status">
                        {(props) => (
                            <select {...props} value={status} onChange={(event) => setStatus(event.target.value)}>
                                <option value="">All statuses</option>
                                {PLAN_STATUSES.map((code) => (
                                    <option key={code} value={code}>
                                        {formatCode(code)}
                                    </option>
                                ))}
                            </select>
                        )}
                    </FilterField>
                    {planButton}
                </div>
            </div>

            {/* Title in the main text colour, lines in gray-700 #364153: both above 9:1 on the tinted box. */}
            {warnings.length > 0 && (
                <div
                    role="status"
                    className="flex items-start gap-3 rounded-2xl border border-warning/50 bg-warning/10 px-4 py-3"
                >
                    <TriangleAlert size={ICON_SIZE} aria-hidden="true" className="mt-0.5 shrink-0 text-[#8a631b]" />
                    <div className="min-w-0">
                        <p className="text-sm font-semibold text-on-background">
                            {`Planned applications exceed the stock in the next ${stockCheck.data.days} days`}
                        </p>
                        <ul className="mt-1 flex flex-col gap-1 text-sm text-gray-700">
                            {warnings.map((warning) => (
                                <li key={warning.fertilizerId}>{stockWarningText(warning, stockCheck.data.days)}</li>
                            ))}
                        </ul>
                    </div>
                </div>
            )}

            {plans.isPending && <LoadingCard>Loading planned applications…</LoadingCard>}

            {plans.isError && (
                <ErrorCard
                    title="Planned applications could not be loaded"
                    onRetry={() => plans.refetch()}
                    isRetrying={plans.isFetching}
                />
            )}

            {plans.isSuccess && plans.data.length === 0 && (
                status ? (
                    <PlantsEmptyState
                        title="No planned application has this status"
                        hint="Try another status."
                    />
                ) : (
                    <PlantsEmptyState title="No application planned yet" action={planButton} />
                )
            )}

            {plans.isSuccess && plans.data.length > 0 && (
                <FertilizerPlansTable plans={plans.data} onComplete={openCompletion} onCancel={openCancellation} />
            )}

            <div className="mt-2 flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
                <div className="min-w-0">
                    <h3 id="planned-vs-applied-title" className="font-heading text-sm font-bold text-on-background">
                        Planned and applied by block
                    </h3>
                    {/* gray-600 #4a5565 on the page (#f8f9ff) -> 7.19:1. */}
                    <p className="mt-1 text-xs text-gray-600">
                        {comparison.isSuccess
                            ? `${periodLabel(comparison.data)} · costs at last purchase price`
                            : "Costs at last purchase price"}
                    </p>
                </div>
                <div className="flex max-w-full flex-wrap items-center gap-x-6 gap-y-3">
                    <FilterField id="planned-vs-applied-from" titleId="planned-vs-applied-title" label="From">
                        {(props) => (
                            <input
                                {...props}
                                type="date"
                                value={from}
                                onChange={(event) => setFrom(event.target.value)}
                            />
                        )}
                    </FilterField>
                    <FilterField id="planned-vs-applied-to" titleId="planned-vs-applied-title" label="To">
                        {(props) => (
                            <input {...props} type="date" value={to} onChange={(event) => setTo(event.target.value)} />
                        )}
                    </FilterField>
                </div>
            </div>

            {comparison.isPending && <LoadingCard>Loading the comparison…</LoadingCard>}

            {comparison.isError && (
                <ErrorCard
                    title="The comparison could not be loaded"
                    onRetry={() => comparison.refetch()}
                    isRetrying={comparison.isFetching}
                />
            )}

            {comparison.isSuccess && comparison.data.rows.length === 0 && (
                <PlantsEmptyState
                    title="Nothing planned or applied over this period"
                    hint="Plan an application, or choose other dates."
                />
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
