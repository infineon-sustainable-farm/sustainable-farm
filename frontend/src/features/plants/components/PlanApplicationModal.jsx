import { useState } from "react";
import { X } from "lucide-react";
import { useFertilizers } from "../hooks/useFertilizers";
import { useVarietyBlocks } from "../hooks/useVarieties";
import { formatDate, formatQuantity, formatUnit } from "../utils/format";
import { planRequestBody, validatePlan } from "../utils/fertilizerPlans";
import PlantsModal from "./PlantsModal";
import { FIELD_INPUT, FIELD_LABEL, PRIMARY_BUTTON, SECONDARY_BUTTON } from "./plantsStyles";

const LABEL_CLASS = FIELD_LABEL;
const INPUT_CLASS = `mt-1 w-full ${FIELD_INPUT} disabled:bg-gray-50`;
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

const INITIAL_VALUES = {
    fertilizerId: "",
    blockCode: "",
    plannedDate: "",
    plannedQuantity: "",
    note: "",
};

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
 * Form of a planned application: a fertilizer of the catalogue, a block, a
 * date and a quantity in the unit of that fertilizer, with an optional note.
 *
 * The quantity is the farmer's own figure: nothing is suggested, and it is
 * not compared with the stock, shown under the field for information only,
 * since the fertilizer may be bought before the date. The block is typed with
 * the same rule as an application, the blocks of the recorded varieties being
 * offered as suggestions. No farm is sent, as for a planting.
 *
 * `today` is the day of the API, the earliest date the form accepts; while it
 * is not known the date is left to the API. What the API still refuses is
 * shown the same way as for a movement: a 400 with fieldErrors under each
 * field, any other error above the form.
 */
export default function PlanApplicationModal({ today, isSubmitting, error, onSubmit, onClose }) {
    const [values, setValues] = useState(INITIAL_VALUES);
    // A number field holds "" for text it cannot read, such as "1e"; this
    // keeps such a value from passing as an empty one.
    const [unreadableQuantity, setUnreadableQuantity] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});

    const fertilizers = useFertilizers();
    const { data: blocks } = useVarietyBlocks();
    const catalogue = fertilizers.data ?? [];
    const fertilizer = catalogue.find((item) => String(item.id) === values.fertilizerId) ?? null;

    let fertilizerNote = null;
    if (fertilizers.isError) {
        fertilizerNote = "Fertilizers could not be loaded";
    } else if (fertilizers.isSuccess && catalogue.length === 0) {
        fertilizerNote = "No fertilizer in the catalogue yet";
    }
    const stockNote = fertilizer ? `In stock: ${formatQuantity(fertilizer.currentStock, fertilizer.unit)}` : null;
    const dateNote = values.plannedDate ? formatDate(values.plannedDate) : null;

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
        const errors = validatePlan(values, { today, unreadableQuantity });
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit(planRequestBody(values));
    }

    return (
        <PlantsModal labelledBy="plan-application-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h2 id="plan-application-title" className="font-heading text-lg font-bold text-on-background">
                    Plan an application
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
                    <FormField
                        id="plan-fertilizer"
                        label="Fertilizer"
                        error={fieldError("fertilizerId")}
                        note={fertilizerNote}
                    >
                        <select
                            id="plan-fertilizer"
                            autoFocus
                            value={values.fertilizerId}
                            disabled={catalogue.length === 0}
                            onChange={(event) => setValue("fertilizerId", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("fertilizerId", "plan-fertilizer", fertilizerNote)}
                        >
                            {/* Empty value: sending without a choice gives "Fertilizer is required." */}
                            <option value="">Select a fertilizer</option>
                            {catalogue.map((item) => (
                                <option key={item.id} value={item.id}>
                                    {`${item.name} (${formatUnit(item.unit)})`}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField id="plan-block" label="Block" error={fieldError("blockCode")}>
                        <input
                            id="plan-block"
                            type="text"
                            list="plan-block-options"
                            value={values.blockCode}
                            onChange={(event) => setValue("blockCode", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("blockCode", "plan-block")}
                        />
                        <datalist id="plan-block-options">
                            {(blocks ?? []).map((block) => (
                                <option key={block} value={block} />
                            ))}
                        </datalist>
                    </FormField>

                    {/* The chosen date in letters, as the browser shows its own day/month order. */}
                    <FormField
                        id="plan-date"
                        label="Planned date"
                        error={fieldError("plannedDate")}
                        note={dateNote}
                    >
                        <input
                            id="plan-date"
                            type="date"
                            min={today ?? undefined}
                            value={values.plannedDate}
                            onChange={(event) => setValue("plannedDate", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("plannedDate", "plan-date", dateNote)}
                        />
                    </FormField>

                    <FormField
                        id="plan-quantity"
                        label={fertilizer ? `Planned quantity (${formatUnit(fertilizer.unit)})` : "Planned quantity"}
                        error={fieldError("plannedQuantity")}
                        note={stockNote}
                    >
                        <input
                            id="plan-quantity"
                            type="number"
                            min="0"
                            step="any"
                            inputMode="decimal"
                            value={values.plannedQuantity}
                            onChange={(event) => {
                                setValue("plannedQuantity", event.target.value);
                                setUnreadableQuantity(event.target.validity.badInput);
                            }}
                            className={INPUT_CLASS}
                            {...fieldProps("plannedQuantity", "plan-quantity", stockNote)}
                        />
                    </FormField>

                    <div className="sm:col-span-2">
                        <FormField id="plan-note" label="Note (optional)" error={fieldError("note")}>
                            <input
                                id="plan-note"
                                type="text"
                                value={values.note}
                                onChange={(event) => setValue("note", event.target.value)}
                                className={INPUT_CLASS}
                                {...fieldProps("note", "plan-note")}
                            />
                        </FormField>
                    </div>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <button type="button" onClick={onClose} className={SECONDARY_BUTTON}>
                        Cancel
                    </button>
                    <button
                        type="submit"
                        disabled={isSubmitting || fertilizerNote !== null}
                        className={PRIMARY_BUTTON}
                    >
                        {isSubmitting ? "Planning…" : "Plan application"}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
