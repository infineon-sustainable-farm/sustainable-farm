import { useState } from "react";
import { useTranslation } from "react-i18next";
import PersonSelect from "../../../app/PersonSelect";
import { Button, Input, Modal } from "../../../shared/ui";
import { useFertilizers } from "../hooks/useFertilizers";
import { formatBlock, formatDate, formatQuantity, formatText, formatUnit } from "../utils/format";
import { completionRequestBody, validateCompletion } from "../utils/fertilizerPlans";
import FertilizerFormField from "./FertilizerFormField";
import FormAlert from "./FormAlert";

/**
 * Form that marks a planned application as done: the day it was applied, the
 * quantity really applied, who applied it (a person of the farm people
 * directory, owner: the Staff screen) and, optionally, how. The API records
 * the application on the fertilizer and the block of the plan, takes the
 * quantity from the stock and links it to the plan.
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
    const { t } = useTranslation("plants");
    const [values, setValues] = useState(() => ({
        applicationDate: today ?? "",
        quantity: String(plan.plannedQuantity),
        applicatorPersonId: "",
        method: "",
    }));
    // A number field holds "" for text it cannot read, such as "1e".
    const [unreadableQuantity, setUnreadableQuantity] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});

    const { data: fertilizers } = useFertilizers();
    const fertilizer = (fertilizers ?? []).find((item) => item.id === plan.fertilizerId) ?? null;
    const stockNote = fertilizer
        ? t("fertilizer.plans.planForm.inStock", { quantity: formatQuantity(fertilizer.currentStock, fertilizer.unit) })
        : null;
    const dateNote = values.applicationDate ? formatDate(values.applicationDate) : null;

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

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
        <Modal
            open
            onClose={onClose}
            title={t("fertilizer.plans.completeForm.title")}
            description={t("fertilizer.plans.completeForm.summary", {
                fertilizer: formatText(plan.fertilizerName),
                block: formatBlock(plan.blockCode),
                quantity: formatQuantity(plan.plannedQuantity, plan.unit),
                date: formatDate(plan.plannedDate),
            })}
            size="xl"
        >
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <FertilizerFormField
                        id="complete-plan-date"
                        label={t("fertilizer.plans.completeForm.date")}
                        error={fieldError("applicationDate")}
                        note={dateNote}
                    >
                        <Input
                            id="complete-plan-date"
                            type="date"
                            max={today ?? undefined}
                            value={values.applicationDate}
                            onChange={(event) => setValue("applicationDate", event.target.value)}
                            aria-describedby={dateNote ? "complete-plan-date-note" : undefined}
                        />
                    </FertilizerFormField>

                    <FertilizerFormField
                        id="complete-plan-quantity"
                        label={t("fertilizer.plans.completeForm.quantityWithUnit", { unit: formatUnit(plan.unit) })}
                        error={fieldError("quantity")}
                        note={stockNote}
                    >
                        <Input
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
                            aria-describedby={stockNote ? "complete-plan-quantity-note" : undefined}
                        />
                    </FertilizerFormField>

                    <PersonSelect
                        id="complete-plan-applicator"
                        label={t("fertilizer.plans.completeForm.applicator")}
                        value={values.applicatorPersonId}
                        onChange={(id) => setValue("applicatorPersonId", id)}
                        error={fieldError("applicatorPersonId")}
                        required
                    />

                    <FertilizerFormField
                        id="complete-plan-method"
                        label={t("fertilizer.plans.completeForm.method")}
                        error={fieldError("method")}
                    >
                        <Input
                            id="complete-plan-method"
                            type="text"
                            value={values.method}
                            onChange={(event) => setValue("method", event.target.value)}
                        />
                    </FertilizerFormField>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button variant="secondary" onClick={onClose}>
                        {t("common:actions.cancel")}
                    </Button>
                    <Button type="submit" disabled={isSubmitting}>
                        {isSubmitting ? t("fertilizer.plans.completeForm.submitting") : t("fertilizer.plans.completeForm.submit")}
                    </Button>
                </div>
            </form>
        </Modal>
    );
}
