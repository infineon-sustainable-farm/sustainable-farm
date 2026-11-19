import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Input, Modal, Select } from "../../../shared/ui";
import { formatEnum } from "../utils/format";
import FertilizerFormField from "./FertilizerFormField";
import FormAlert from "./FormAlert";

// The API codes, sent as they are; only their labels are shown.
const FERTILIZER_TYPES = ["MINERAL", "ORGANIC"];
// Unit symbols, the same in every language.
const UNITS = [
    { code: "KG", label: "kg" },
    { code: "L", label: "L" },
];

/** Same rule as the API: 0 or more, with at most 3 decimals. */
const THRESHOLD_PATTERN = /^\d+(\.\d{1,3})?$/;

/**
 * Form of a new fertilizer of the catalogue. It starts with a stock of 0,
 * until a purchase is recorded.
 *
 * The alert threshold is optional and, as the API offers no way to change
 * it, final: the note under the field says so. The checks mirror the API
 * rules; what the API still refuses is shown the same way as for a planting:
 * - a 400 with fieldErrors: each message under its field;
 * - a 409 (a fertilizer of that name exists), or any other error: one message
 *   above the form.
 */
export default function AddFertilizerModal({ isSubmitting, error, onSubmit, onClose }) {
    const { t } = useTranslation("plants");
    const [name, setName] = useState("");
    const [fertilizerType, setFertilizerType] = useState("");
    const [composition, setComposition] = useState("");
    const [unit, setUnit] = useState("");
    const [reorderThreshold, setReorderThreshold] = useState("");
    // A number field holds "" for text it cannot read, such as "1e"; this
    // keeps such a value from passing as an empty, optional threshold.
    const [isThresholdUnreadable, setIsThresholdUnreadable] = useState(false);
    const [fieldErrors, setFieldErrors] = useState({});

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];
    const thresholdNote = t("fertilizer.addForm.thresholdNote");
    const typeLabel = (type) => formatEnum("fertilizerType", type);

    function changeThreshold(event) {
        setReorderThreshold(event.target.value);
        setIsThresholdUnreadable(event.target.validity.badInput);
    }

    function validate() {
        const errors = {};
        const message = (key) => t(`fertilizer.addForm.errors.${key}`);
        if (!name.trim()) {
            errors.name = message("nameRequired");
        } else if (name.length > 255) {
            errors.name = message("nameTooLong");
        }
        if (!fertilizerType) {
            errors.fertilizerType = message("typeRequired");
        }
        if (composition.length > 100) {
            errors.composition = message("compositionTooLong");
        }
        if (!unit) {
            errors.unit = message("unitRequired");
        }
        if (isThresholdUnreadable
            || (reorderThreshold.trim() !== "" && !THRESHOLD_PATTERN.test(reorderThreshold.trim()))) {
            errors.reorderThreshold = message("thresholdInvalid");
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        // Optional fields left empty are not sent at all.
        const fertilizer = { name, fertilizerType, unit };
        if (composition.trim()) fertilizer.composition = composition;
        if (reorderThreshold.trim()) fertilizer.reorderThreshold = Number(reorderThreshold);
        onSubmit(fertilizer);
    }

    return (
        <Modal open onClose={onClose} title={t("fertilizer.addForm.title")} size="xl">
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <FertilizerFormField id="fertilizer-name" label={t("fertilizer.addForm.name")} error={fieldError("name")}>
                        <Input id="fertilizer-name" type="text" value={name} onChange={(event) => setName(event.target.value)} />
                    </FertilizerFormField>

                    <FertilizerFormField id="fertilizer-type" label={t("fertilizer.addForm.type")} error={fieldError("fertilizerType")}>
                        <Select
                            id="fertilizer-type"
                            value={fertilizerType}
                            onChange={(event) => setFertilizerType(event.target.value)}
                        >
                            {/* Empty value: sending without a choice gives "Type is required." */}
                            <option value="">{t("fertilizer.addForm.selectType")}</option>
                            {FERTILIZER_TYPES.map((type) => (
                                <option key={type} value={type}>
                                    {typeLabel(type)}
                                </option>
                            ))}
                        </Select>
                    </FertilizerFormField>

                    <FertilizerFormField
                        id="fertilizer-composition"
                        label={t("fertilizer.addForm.composition")}
                        error={fieldError("composition")}
                    >
                        <Input
                            id="fertilizer-composition"
                            type="text"
                            value={composition}
                            onChange={(event) => setComposition(event.target.value)}
                            placeholder={t("fertilizer.addForm.compositionPlaceholder")}
                        />
                    </FertilizerFormField>

                    <FertilizerFormField id="fertilizer-unit" label={t("fertilizer.addForm.unit")} error={fieldError("unit")}>
                        <Select id="fertilizer-unit" value={unit} onChange={(event) => setUnit(event.target.value)}>
                            {/* Empty value: sending without a choice gives "Unit is required." */}
                            <option value="">{t("fertilizer.addForm.selectUnit")}</option>
                            {UNITS.map(({ code, label: symbol }) => (
                                <option key={code} value={code}>
                                    {symbol}
                                </option>
                            ))}
                        </Select>
                    </FertilizerFormField>

                    <FertilizerFormField
                        id="fertilizer-threshold"
                        label={t("fertilizer.addForm.threshold")}
                        error={fieldError("reorderThreshold")}
                        note={thresholdNote}
                    >
                        <Input
                            id="fertilizer-threshold"
                            type="number"
                            min="0"
                            step="any"
                            inputMode="decimal"
                            value={reorderThreshold}
                            onChange={changeThreshold}
                            aria-describedby="fertilizer-threshold-note"
                        />
                    </FertilizerFormField>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button variant="secondary" onClick={onClose}>
                        {t("common:actions.cancel")}
                    </Button>
                    <Button type="submit" disabled={isSubmitting}>
                        {isSubmitting ? t("fertilizer.addForm.submitting") : t("fertilizer.addForm.submit")}
                    </Button>
                </div>
            </form>
        </Modal>
    );
}
