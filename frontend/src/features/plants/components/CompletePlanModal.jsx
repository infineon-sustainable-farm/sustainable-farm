import { useState } from "react";
import { X } from "lucide-react";
import { useFertilizers } from "../hooks/useFertilizers";
import { formatBlock, formatDate, formatQuantity, formatText, formatUnit } from "../utils/format";
import { completionRequestBody, validateCompletion } from "../utils/fertilizerPlans";
import PlantsModal from "./PlantsModal";
import { FIELD_INPUT, FIELD_LABEL, PRIMARY_BUTTON, SECONDARY_BUTTON } from "./plantsStyles";

const LABEL_CLASS = FIELD_LABEL;
const INPUT_CLASS = `mt-1 w-full ${FIELD_INPUT}`;
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

function FormField({ id, label, error, note, children }) {
    return (
        <div>
            <label htmlFor={id} className={LABEL_CLASS}>
                {label}
            </label>
            {children}
            {error && (
                <p id={`${id}-error`} className={ERROR_CLASS}>
                    {error}
                </p>
            )}
            {note && (
                <p id={`${id}-note`} className={NOTE_CLASS}>
                    {note}
                </p>
            )}
        </div>
    );
}

/**
 * Form that marks a planned application as done: the day it was applied, the
 * quantity really applied, who applied it and, optionally, how. The API
 * records the application on the fertilizer and the block of the plan, takes
 * the quantity from the stock and links it to the plan.
 *
 * The quantity starts at the planned one and can be changed: applying more or
 * less than planned is what the comparison shows. The date starts at `today`,
 * the day of the API, which is also the latest date the form accepts; while
 * that day is not known the date starts empty and is left to the API.
 *
 * What the API still refuses is shown the same way as for a movement:
 * - a 400 with fieldErrors: each message under its field;
 * - a 422 (the quantity exceeds the stock), a 409 (the plan was done or
 *   cancelled meanwhile, for example from another tab), or any other error:
 *   one message above the form. The plans are refreshed after a 409, so the
 *   row shows its state.
 */
export default function CompletePlanModal({ plan, today, isSubmitting, error, onSubmit, onClose }) {
    const [values, setValues] = useState(() => ({
        applicationDate: today ?? "",
        quantity: String(plan.plannedQuantity),
        applicator: "",
        method: "",
    }));
    // A number field holds "" for text it cannot read, such as "1e".
    const [unreadableQuantity, setUnreadableQuantity] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});

    const { data: fertilizers } = useFertilizers();
    const fertilizer = (fertilizers ?? []).find((item) => item.id === plan.fertilizerId) ?? null;
    const stockNote = fertilizer ? `In stock: ${formatQuantity(fertilizer.currentStock, fertilizer.unit)}` : null;
    const dateNote = values.applicationDate ? formatDate(values.applicationDate) : null;

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];
    const fieldProps = (field, id, note = null) => ({
        "aria-invalid": fieldError(field) ? true : undefined,
        "aria-describedby":
            [fieldError(field) ? `${id}-error` : null, note ? `${id}-note` : null].filter(Boolean).join(" ")
            || undefined,
    });

    function setValue(field, value) {
        setValues((current) => ({ ...current, [field]: value }));
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validateCompletion(values, { today, unreadableQuantity });
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit({ planId: plan.id, body: completionRequestBody(values) });
    }

    return (
        <PlantsModal labelledBy="complete-plan-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <div>
                    <h2 id="complete-plan-title" className="font-heading text-lg font-bold text-on-background">
                        Mark as done
                    </h2>
                    {/* gray-600 #4a5565 on white -> 7.56:1. */}
                    <p className="mt-1 text-sm text-gray-600">
                        {`${formatText(plan.fertilizerName)} · ${formatBlock(plan.blockCode)} · planned `
                            + `${formatQuantity(plan.plannedQuantity, plan.unit)} on ${formatDate(plan.plannedDate)}`}
                    </p>
                </div>
                <button
                    type="button"
                    onClick={onClose}
                    aria-label="Close"
                    className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-gray-800"
                >
                    <X size={20} />
                </button>
            </div>

            <form noValidate onSubmit={handleSubmit} className="px-6 py-5">
                {/* #9a3f32 on the tinted alert (#fcf6f5) -> 6.27:1, as in the movement form. */}
                {formError && (
                    <p
                        role="alert"
                        className="mb-5 rounded-lg border border-error/30 bg-error/5 px-3 py-2 text-sm text-[#9a3f32]"
                    >
                        {formError}
                    </p>
                )}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    {/* The chosen date in letters, as the browser shows its own day/month order. */}
                    <FormField
                        id="complete-plan-date"
                        label="Date applied"
                        error={fieldError("applicationDate")}
                        note={dateNote}
                    >
                        <input
                            id="complete-plan-date"
                            type="date"
                            autoFocus
                            max={today ?? undefined}
                            value={values.applicationDate}
                            onChange={(event) => setValue("applicationDate", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("applicationDate", "complete-plan-date", dateNote)}
                        />
                    </FormField>

                    <FormField
                        id="complete-plan-quantity"
                        label={`Quantity applied (${formatUnit(plan.unit)})`}
                        error={fieldError("quantity")}
                        note={stockNote}
                    >
                        <input
                            id="complete-plan-quantity"
                            type="number"
                            min="0"
                            step="any"
                            inputMode="decimal"
                            value={values.quantity}
                            onChange={(event) => {
                                setValue("quantity", event.target.value);
                                setUnreadableQuantity(event.target.validity.badInput);
                            }}
                            className={INPUT_CLASS}
                            {...fieldProps("quantity", "complete-plan-quantity", stockNote)}
                        />
                    </FormField>

                    <FormField id="complete-plan-applicator" label="Applicator" error={fieldError("applicator")}>
                        <input
                            id="complete-plan-applicator"
                            type="text"
                            value={values.applicator}
                            onChange={(event) => setValue("applicator", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("applicator", "complete-plan-applicator")}
                        />
                    </FormField>

                    <FormField id="complete-plan-method" label="Method (optional)" error={fieldError("method")}>
                        <input
                            id="complete-plan-method"
                            type="text"
                            value={values.method}
                            onChange={(event) => setValue("method", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("method", "complete-plan-method")}
                        />
                    </FormField>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <button type="button" onClick={onClose} className={SECONDARY_BUTTON}>
                        Cancel
                    </button>
                    <button type="submit" disabled={isSubmitting} className={PRIMARY_BUTTON}>
                        {isSubmitting ? "Recording…" : "Mark as done"}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
