import { useTranslation } from "react-i18next";
import { Input, Select } from "../../../shared/ui";
import { CURRENCIES } from "../utils/fertilizerMovements";
import BlockSelect from "./BlockSelect";
import FertilizerFormField from "./FertilizerFormField";

// The text fields of the types: name in the form state, id of the control, label.
const TEXT_FIELDS = {
    supplier: { field: "supplier", id: "movement-supplier", labelKey: "fertilizer.movementForm.supplier" },
    blockCode: { field: "blockCode", id: "movement-block", labelKey: "fertilizer.movementForm.block" },
    applicator: { field: "applicator", id: "movement-applicator", labelKey: "fertilizer.movementForm.applicator" },
    method: { field: "method", id: "movement-method", labelKey: "fertilizer.movementForm.method" },
    reason: { field: "reason", id: "movement-reason", labelKey: "fertilizer.movementForm.reason" },
};
const APPLICATION_FIELDS = [TEXT_FIELDS.blockCode, TEXT_FIELDS.applicator, TEXT_FIELDS.method];

/**
 * The fields a movement type adds to the fertilizer, the date and the quantity: supplier, total
 * cost and currency of a purchase; block, applicator and method of an application; reason of a
 * loss. The block is picked among the blocks of the farm referential.
 */
export default function MovementTypeFields({ movementType, values, setValue, setNumber, fieldError }) {
    const { t } = useTranslation("plants");

    function textField({ field, id, labelKey }) {
        if (field === "blockCode") {
            return (
                <BlockSelect
                    key={field}
                    id={id}
                    label={t(labelKey)}
                    value={values.blockCode}
                    onChange={(code) => setValue("blockCode", code)}
                    error={fieldError("blockCode")}
                />
            );
        }
        return (
            <FertilizerFormField key={field} id={id} label={t(labelKey)} error={fieldError(field)}>
                <Input
                    id={id}
                    type="text"
                    value={values[field]}
                    onChange={(event) => setValue(field, event.target.value)}
                />
            </FertilizerFormField>
        );
    }

    if (movementType === "PURCHASE") {
        return (
            <>
                {textField(TEXT_FIELDS.supplier)}

                <FertilizerFormField id="movement-total-cost" label={t("fertilizer.movementForm.totalCost")} error={fieldError("totalCost")}>
                    <Input
                        id="movement-total-cost"
                        type="number"
                        min="0"
                        step="any"
                        inputMode="decimal"
                        value={values.totalCost}
                        onChange={(event) => setNumber("totalCost", event)}
                    />
                </FertilizerFormField>

                <FertilizerFormField id="movement-currency" label={t("fertilizer.movementForm.currency")} error={fieldError("currency")}>
                    <Select
                        id="movement-currency"
                        value={values.currency}
                        onChange={(event) => setValue("currency", event.target.value)}
                    >
                        {CURRENCIES.map(({ code, label: symbol }) => (
                            <option key={code} value={code}>
                                {symbol}
                            </option>
                        ))}
                    </Select>
                </FertilizerFormField>
            </>
        );
    }

    if (movementType === "APPLICATION") {
        return (
            <>
                {APPLICATION_FIELDS.map(textField)}
            </>
        );
    }

    return textField(TEXT_FIELDS.reason);
}
