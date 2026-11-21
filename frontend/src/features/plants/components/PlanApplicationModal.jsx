import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Input, Modal, Select } from "../../../shared/ui";
import { useFertilizers } from "../hooks/useFertilizers";
import { formatDate, formatQuantity, formatUnit } from "../utils/format";
import { planRequestBody, validatePlan } from "../utils/fertilizerPlans";
import BlockSelect from "./BlockSelect";
import FertilizerFormField from "./FertilizerFormField";
import FormAlert from "./FormAlert";

const INITIAL_VALUES = {
    fertilizerId: "",
    blockCode: "",
    plannedDate: "",
    plannedQuantity: "",
    note: "",
};

/**
 * Form of a planned application: a fertilizer of the catalogue, a block, a
 * date and a quantity in the unit of that fertilizer, with an optional note.
 *
 * The quantity is the farmer's own figure: nothing is suggested, and it is
 * not compared with the stock, shown under the field for information only,
 * since the fertilizer may be bought before the date. The block is picked
 * among the blocks of the farm referential (owner: Farms & fields of Water
 * Supply, reached through the hint when the block is missing); plants never
 * creates one. No farm is sent: the block gives it.
 *
 * `today` is the day of the API, the earliest date the form accepts; while it
 * is not known the date is left to the API. What the API still refuses is
 * shown the same way as for a movement: a 400 with fieldErrors under each
 * field, any other error above the form.
 */
export default function PlanApplicationModal({ today, isSubmitting, error, onSubmit, onClose }) {
    const { t } = useTranslation("plants");
    const [values, setValues] = useState(INITIAL_VALUES);
    // A number field holds "" for text it cannot read, such as "1e"; this
    // keeps such a value from passing as an empty one.
    const [unreadableQuantity, setUnreadableQuantity] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});

    const fertilizers = useFertilizers();
    const catalogue = fertilizers.data ?? [];
    const fertilizer = catalogue.find((item) => String(item.id) === values.fertilizerId) ?? null;

    let fertilizerNote = null;
    if (fertilizers.isError) {
        fertilizerNote = t("fertilizer.stock.loadError");
    } else if (fertilizers.isSuccess && catalogue.length === 0) {
        fertilizerNote = t("fertilizer.stock.empty");
    }
    const stockNote = fertilizer
        ? t("fertilizer.plans.planForm.inStock", { quantity: formatQuantity(fertilizer.currentStock, fertilizer.unit) })
        : null;
    // The chosen date in letters, as the browser shows its own day/month order.
    const dateNote = values.plannedDate ? formatDate(values.plannedDate) : null;

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

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
        <Modal open onClose={onClose} title={t("fertilizer.plans.planForm.title")} size="xl">
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <FertilizerFormField
                        id="plan-fertilizer"
                        label={t("fertilizer.plans.planForm.fertilizer")}
                        error={fieldError("fertilizerId")}
                        note={fertilizerNote}
                    >
                        <Select
                            id="plan-fertilizer"
                            value={values.fertilizerId}
                            disabled={catalogue.length === 0}
                            onChange={(event) => setValue("fertilizerId", event.target.value)}
                            aria-describedby={fertilizerNote ? "plan-fertilizer-note" : undefined}
                        >
                            {/* Empty value: sending without a choice gives "Fertilizer is required." */}
                            <option value="">{t("fertilizer.plans.planForm.selectFertilizer")}</option>
                            {catalogue.map((item) => (
                                <option key={item.id} value={item.id}>
                                    {t("fertilizer.plans.planForm.fertilizerOption", { name: item.name, unit: formatUnit(item.unit) })}
                                </option>
                            ))}
                        </Select>
                    </FertilizerFormField>

                    <BlockSelect
                        id="plan-block"
                        label={t("fertilizer.plans.planForm.block")}
                        value={values.blockCode}
                        onChange={(code) => setValue("blockCode", code)}
                        error={fieldError("blockCode")}
                        required
                    />

                    <FertilizerFormField
                        id="plan-date"
                        label={t("fertilizer.plans.planForm.plannedDate")}
                        error={fieldError("plannedDate")}
                        note={dateNote}
                    >
                        <Input
                            id="plan-date"
                            type="date"
                            min={today ?? undefined}
                            value={values.plannedDate}
                            onChange={(event) => setValue("plannedDate", event.target.value)}
                            aria-describedby={dateNote ? "plan-date-note" : undefined}
                        />
                    </FertilizerFormField>

                    <FertilizerFormField
                        id="plan-quantity"
                        label={fertilizer
                            ? t("fertilizer.plans.planForm.quantityWithUnit", { unit: formatUnit(fertilizer.unit) })
                            : t("fertilizer.plans.planForm.quantity")}
                        error={fieldError("plannedQuantity")}
                        note={stockNote}
                    >
                        <Input
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
                            aria-describedby={stockNote ? "plan-quantity-note" : undefined}
                        />
                    </FertilizerFormField>

                    <div className="sm:col-span-2">
                        <FertilizerFormField id="plan-note" label={t("fertilizer.plans.planForm.note")} error={fieldError("note")}>
                            <Input
                                id="plan-note"
                                type="text"
                                value={values.note}
                                onChange={(event) => setValue("note", event.target.value)}
                            />
                        </FertilizerFormField>
                    </div>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button variant="secondary" onClick={onClose}>
                        {t("common:actions.cancel")}
                    </Button>
                    <Button type="submit" disabled={isSubmitting || fertilizerNote !== null}>
                        {isSubmitting ? t("fertilizer.plans.planForm.submitting") : t("fertilizer.plans.planForm.submit")}
                    </Button>
                </div>
            </form>
        </Modal>
    );
}
