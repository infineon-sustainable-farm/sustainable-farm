import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal } from "../../../shared/ui";
import { formatDate } from "../utils/format";
import FormAlert from "./FormAlert";
import { todayAsIsoDate, usePlantedVarieties } from "../hooks/usePlantedVarieties";
import { BlockAndVarietyFields, FormButtons } from "./HarvestFormParts";

/**
 * Form of a harvest: one picking of one variety on one block.
 *
 * The block and the variety are picked from lists, so the variety name goes
 * out exactly as it was planted, accents included. Only the varieties with a
 * recorded planting on the block are offered: the API refuses a harvest of any
 * other. The checks mirror the API rules; what the API still refuses is shown
 * the same way as for a planting:
 * - a 400 with fieldErrors: each message under its field;
 * - a 422 (no planting of that variety on the block, or a harvest before the
 *   planting), or any other error: one message above the form.
 */
export default function RecordHarvestModal({ blocks, isSubmitting, error, onSubmit, onClose }) {
    const { t } = useTranslation("plants");
    const [harvestDate, setHarvestDate] = useState("");
    const [blockCode, setBlockCode] = useState("");
    const [varietyName, setVarietyName] = useState("");
    const [quantityKg, setQuantityKg] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);

    const { plantedVarieties, varietyNote } = usePlantedVarieties(blockCode);

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    function changeBlock(value) {
        setBlockCode(value);
        // The variety belongs to the block: another block means another list.
        setVarietyName("");
    }

    function validate() {
        const errors = {};
        if (!harvestDate) {
            errors.harvestDate = t("harvest.recordHarvest.errors.dateRequired");
        } else if (harvestDate > today) {
            errors.harvestDate = t("harvest.recordHarvest.errors.dateFuture");
        }
        if (!blockCode) {
            errors.blockCode = t("harvest.form.blockRequired");
        }
        if (!varietyName) {
            errors.varietyName = t("harvest.form.varietyRequired");
        }
        const quantity = Number(quantityKg);
        if (quantityKg.trim() === "" || !Number.isFinite(quantity) || quantity <= 0) {
            errors.quantityKg = t("harvest.recordHarvest.errors.quantity");
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit({ harvestDate, blockCode, varietyName, quantityKg: Number(quantityKg) });
    }

    return (
        <Modal open onClose={onClose} title={t("harvest.recordHarvest.title")} size="xl">
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    {/* The chosen date in letters, as the browser shows its own day/month order. */}
                    <Field
                        id="harvest-date"
                        label={t("harvest.recordHarvest.date")}
                        error={fieldError("harvestDate")}
                        hint={harvestDate ? formatDate(harvestDate) : null}
                    >
                        <Input
                            type="date"
                            max={today}
                            value={harvestDate}
                            onChange={(event) => setHarvestDate(event.target.value)}
                        />
                    </Field>

                    <BlockAndVarietyFields
                        idPrefix="harvest"
                        blocks={blocks}
                        blockCode={blockCode}
                        onBlockChange={changeBlock}
                        varietyName={varietyName}
                        onVarietyChange={setVarietyName}
                        plantedVarieties={plantedVarieties}
                        varietyNote={varietyNote}
                        fieldError={fieldError}
                    />

                    <Field id="harvest-quantity" label={t("harvest.recordHarvest.quantity")} error={fieldError("quantityKg")}>
                        <Input
                            type="number"
                            min="0"
                            step="any"
                            inputMode="decimal"
                            value={quantityKg}
                            onChange={(event) => setQuantityKg(event.target.value)}
                        />
                    </Field>
                </div>

                <FormButtons
                    onCancel={onClose}
                    isSubmitting={isSubmitting}
                    submitDisabled={varietyNote !== null}
                    submitLabel={t("harvest.recordHarvest.submit")}
                    submittingLabel={t("harvest.form.recording")}
                />
            </form>
        </Modal>
    );
}
