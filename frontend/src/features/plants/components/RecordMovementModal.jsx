import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Input, Modal, Select } from "../../../shared/ui";
import { useFertilizers } from "../hooks/useFertilizers";
import {
    DATE_FIELDS,
    INITIAL_VALUES,
    MOVEMENT_TYPES,
    movementRequestBody,
    todayAsIsoDate,
    TYPE_FIELDS,
    validateMovement,
} from "../utils/fertilizerMovements";
import { formatDate, formatEnum, formatQuantity, formatUnit } from "../utils/format";
import FertilizerFormField from "./FertilizerFormField";
import FormAlert from "./FormAlert";
import MovementTypeFields from "./MovementTypeFields";

/**
 * Form of a stock movement: a purchase, an application on a block or a loss.
 *
 * The three share the fertilizer, the date and the quantity, in the unit of
 * the fertilizer. Changing the type empties the fields of the previous one
 * and drops their errors, the API's included, and only the fields of the
 * chosen type are sent. For an application or a loss, the note under the
 * quantity gives the stock as the API reports it; whether the stock covers
 * the quantity is left to the API.
 *
 * The block of an application is typed, with the same rule as a planting:
 * the base fertilizer goes in before the tree, so the block may have no
 * planting yet. No farm is sent, as for a planting.
 *
 * What the API still refuses is shown the same way as for a planting:
 * - a 400 with fieldErrors: each message under its field;
 * - a 422 (the quantity exceeds the stock), or any other error: one message
 *   above the form.
 */
export default function RecordMovementModal({ isSubmitting, error, onSubmit, onResetError, onClose }) {
    const { t } = useTranslation("plants");
    const [movementType, setMovementType] = useState("PURCHASE");
    const [values, setValues] = useState(INITIAL_VALUES);
    // A number field holds "" for text it cannot read, such as "1e"; these keep
    // such a value from passing as an empty one.
    const [unreadable, setUnreadable] = useState({ quantity: false, totalCost: false });
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);
    const typeLabel = (type) => formatEnum("movementType", type);

    const fertilizers = useFertilizers();
    const catalogue = fertilizers.data ?? [];
    const fertilizer = catalogue.find((item) => String(item.id) === values.fertilizerId) ?? null;

    let fertilizerNote = null;
    if (fertilizers.isError) {
        fertilizerNote = t("fertilizer.stock.loadError");
    } else if (fertilizers.isSuccess && catalogue.length === 0) {
        fertilizerNote = t("fertilizer.stock.empty");
    }
    const stockNote = fertilizer && movementType !== "PURCHASE"
        ? t("fertilizer.movementForm.inStock", { quantity: formatQuantity(fertilizer.currentStock, fertilizer.unit) })
        : null;
    // The chosen date in letters, as the browser shows its own day/month order.
    const dateNote = values.date ? formatDate(values.date) : null;

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) =>
        fieldErrors[field] ?? serverFieldErrors?.[field === "date" ? DATE_FIELDS[movementType] : field];

    function setValue(field, value) {
        setValues((current) => ({ ...current, [field]: value }));
    }

    function setNumber(field, event) {
        setValue(field, event.target.value);
        setUnreadable((current) => ({ ...current, [field]: event.target.validity.badInput }));
    }

    function changeType(nextType) {
        const previousFields = TYPE_FIELDS[movementType];
        setValues((current) => ({
            ...current,
            ...Object.fromEntries(previousFields.map((field) => [field, INITIAL_VALUES[field]])),
        }));
        setUnreadable((current) => ({ ...current, totalCost: false }));
        setFieldErrors((current) =>
            Object.fromEntries(Object.entries(current).filter(([field]) => !previousFields.includes(field))));
        // The API's answer was about the previous type: none of it applies any more.
        onResetError();
        setMovementType(nextType);
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validateMovement(movementType, values, unreadable, today);
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit({ movementType, fertilizerId: Number(values.fertilizerId), body: movementRequestBody(movementType, values) });
    }

    return (
        <Modal open onClose={onClose} title={t("fertilizer.movementForm.title")} size="xl">
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                <fieldset className="mb-5">
                    <legend className="text-sm font-medium">{t("fertilizer.movementForm.type")}</legend>
                    <div className="mt-2 flex flex-wrap gap-x-6 gap-y-2">
                        {MOVEMENT_TYPES.map((type) => (
                            <label key={type} className="flex items-center gap-2 text-sm text-text">
                                <input
                                    type="radio"
                                    name="movement-type"
                                    value={type}
                                    checked={movementType === type}
                                    onChange={() => changeType(type)}
                                    className="accent-primary"
                                />
                                {typeLabel(type)}
                            </label>
                        ))}
                    </div>
                </fieldset>

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <FertilizerFormField
                        id="movement-fertilizer"
                        label={t("fertilizer.movementForm.fertilizer")}
                        error={fieldError("fertilizerId")}
                        note={fertilizerNote}
                    >
                        <Select
                            id="movement-fertilizer"
                            value={values.fertilizerId}
                            disabled={catalogue.length === 0}
                            onChange={(event) => setValue("fertilizerId", event.target.value)}
                            aria-describedby={fertilizerNote ? "movement-fertilizer-note" : undefined}
                        >
                            {/* Empty value: sending without a choice gives "Fertilizer is required." */}
                            <option value="">{t("fertilizer.movementForm.selectFertilizer")}</option>
                            {catalogue.map((item) => (
                                <option key={item.id} value={item.id}>
                                    {item.name}
                                </option>
                            ))}
                        </Select>
                    </FertilizerFormField>

                    <FertilizerFormField id="movement-date" label={t("fertilizer.movementForm.date")} error={fieldError("date")} note={dateNote}>
                        <Input
                            id="movement-date"
                            type="date"
                            max={today}
                            value={values.date}
                            onChange={(event) => setValue("date", event.target.value)}
                            aria-describedby={dateNote ? "movement-date-note" : undefined}
                        />
                    </FertilizerFormField>

                    <FertilizerFormField
                        id="movement-quantity"
                        label={fertilizer ? t("fertilizer.movementForm.quantityWithUnit", { unit: formatUnit(fertilizer.unit) }) : t("fertilizer.movementForm.quantity")}
                        error={fieldError("quantity")}
                        note={stockNote}
                    >
                        <Input
                            id="movement-quantity"
                            type="number"
                            min="0"
                            step="any"
                            inputMode="decimal"
                            value={values.quantity}
                            onChange={(event) => setNumber("quantity", event)}
                            aria-describedby={stockNote ? "movement-quantity-note" : undefined}
                        />
                    </FertilizerFormField>

                    <MovementTypeFields
                        movementType={movementType}
                        values={values}
                        setValue={setValue}
                        setNumber={setNumber}
                        fieldError={fieldError}
                    />
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button variant="secondary" onClick={onClose}>
                        {t("common:actions.cancel")}
                    </Button>
                    <Button type="submit" disabled={isSubmitting || fertilizerNote !== null}>
                        {isSubmitting ? t("fertilizer.movementForm.submitting") : t(`fertilizer.movementForm.submit.${movementType}`)}
                    </Button>
                </div>
            </form>
        </Modal>
    );
}
