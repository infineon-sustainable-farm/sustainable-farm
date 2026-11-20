import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Field, Input, Modal, Select } from "../../../shared/ui";
import { plantingBody, spacingErrors, VIGOR_OPTIONS } from "../utils/plantingRequests";
import BlockSelect from "./BlockSelect";
import FormAlert from "./FormAlert";

/** Longest variety name the API accepts. */
const VARIETY_MAX_LENGTH = 255;

/** Today as "YYYY-MM-DD", in local time: the latest planting date the form accepts. */
function todayAsIsoDate() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${now.getFullYear()}-${month}-${day}`;
}

/**
 * Form of a dated planting: the orchard is entered block by block, one planting
 * at a time. The spacings and the vigor are optional and go on the variety row;
 * on a row that already has them, the values entered replace them.
 *
 * The checks mirror the API rules, so most mistakes are caught before sending.
 * What the API still refuses is shown the same way:
 * - a 400 with fieldErrors: each message under its field;
 * - a 409 (variety already planted on that block), or any other error: one
 *   message above the form.
 */
export default function RecordPlantingModal({ isSubmitting, error, onSubmit, onClose }) {
    const { t } = useTranslation("plants");
    const [plantingDate, setPlantingDate] = useState("");
    const [blockCode, setBlockCode] = useState("");
    const [varietyName, setVarietyName] = useState("");
    const [treeCount, setTreeCount] = useState("");
    const [rowSpacing, setRowSpacing] = useState("");
    const [treeSpacing, setTreeSpacing] = useState("");
    const [vigor, setVigor] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];
    const message = (key, options) => t(`varieties.validation.${key}`, options);

    function validate() {
        const errors = {};
        if (!plantingDate) {
            errors.plantingDate = message("plantingDateRequired");
        } else if (plantingDate > today) {
            errors.plantingDate = message("plantingDateFuture");
        }
        if (!blockCode) {
            errors.blockCode = message("blockRequired");
        }
        if (!varietyName.trim()) {
            errors.varietyName = message("varietyRequired");
        } else if (varietyName.length > VARIETY_MAX_LENGTH) {
            errors.varietyName = message("varietyTooLong", { max: VARIETY_MAX_LENGTH });
        }
        const count = Number(treeCount);
        if (treeCount === "" || !Number.isInteger(count) || count < 1) {
            errors.treeCount = message("treeCount");
        }
        return { ...errors, ...spacingErrors({ rowSpacing, treeSpacing }) };
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit(plantingBody({ plantingDate, blockCode, varietyName, treeCount, rowSpacing, treeSpacing, vigor }));
    }

    return (
        <Modal open onClose={onClose} size="xl" title={t("varieties.planting.title")} dismissible={!isSubmitting}>
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-4">{formError}</FormAlert>}

                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <Field id="planting-date" label={t("varieties.planting.plantingDate")} error={fieldError("plantingDate")}>
                        <Input
                            type="date"
                            max={today}
                            value={plantingDate}
                            onChange={(event) => setPlantingDate(event.target.value)}
                        />
                    </Field>

                    <BlockSelect
                        id="planting-block"
                        label={t("varieties.planting.block")}
                        value={blockCode}
                        onChange={setBlockCode}
                        error={fieldError("blockCode")}
                    />

                    <Field id="planting-variety" label={t("varieties.planting.variety")} error={fieldError("varietyName")}>
                        <Input
                            type="text"
                            value={varietyName}
                            onChange={(event) => setVarietyName(event.target.value)}
                            placeholder={t("varieties.planting.varietyPlaceholder")}
                        />
                    </Field>

                    <Field id="planting-tree-count" label={t("varieties.planting.treeCount")} error={fieldError("treeCount")}>
                        <Input
                            type="number"
                            min="1"
                            step="1"
                            inputMode="numeric"
                            value={treeCount}
                            onChange={(event) => setTreeCount(event.target.value)}
                        />
                    </Field>

                    <Field
                        id="planting-row-spacing"
                        label={t("varieties.planting.rowSpacing")}
                        error={fieldError("rowSpacingM")}
                        hint={t("varieties.planting.optional")}
                    >
                        <Input
                            type="number"
                            min="0.1"
                            max="30"
                            step="0.1"
                            inputMode="decimal"
                            value={rowSpacing}
                            onChange={(event) => setRowSpacing(event.target.value)}
                            placeholder={t("varieties.planting.spacingPlaceholder")}
                        />
                    </Field>

                    <Field
                        id="planting-tree-spacing"
                        label={t("varieties.planting.treeSpacing")}
                        error={fieldError("treeSpacingM")}
                        hint={t("varieties.planting.optional")}
                    >
                        <Input
                            type="number"
                            min="0.1"
                            max="30"
                            step="0.1"
                            inputMode="decimal"
                            value={treeSpacing}
                            onChange={(event) => setTreeSpacing(event.target.value)}
                            placeholder={t("varieties.planting.spacingPlaceholder")}
                        />
                    </Field>

                    <Field id="planting-vigor" label={t("varieties.planting.vigor")} error={fieldError("vigor")} hint={t("varieties.planting.optional")}>
                        <Select value={vigor} onChange={(event) => setVigor(event.target.value)}>
                            {VIGOR_OPTIONS.map((option) => (
                                <option key={option.code} value={option.code}>
                                    {t(option.labelKey)}
                                </option>
                            ))}
                        </Select>
                    </Field>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button variant="secondary" onClick={onClose}>
                        {t("common:actions.cancel")}
                    </Button>
                    <Button type="submit" disabled={isSubmitting}>
                        {isSubmitting ? t("varieties.planting.submitting") : t("varieties.planting.submit")}
                    </Button>
                </div>
            </form>
        </Modal>
    );
}
