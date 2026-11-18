import { useTranslation } from "react-i18next";
import { Button, Field, Select } from "../../../shared/ui";
import { formatBlock } from "../utils/format";

/** Block, then the planted varieties of that block, as both harvest forms pick them. */
export function BlockAndVarietyFields({ idPrefix, blocks, blockCode, onBlockChange, varietyName, onVarietyChange,
    plantedVarieties, varietyNote, fieldError }) {
    const { t } = useTranslation("plants");
    return (
        <>
            <Field id={`${idPrefix}-block`} label={t("harvest.form.block")} error={fieldError("blockCode")}>
                <Select value={blockCode} onChange={(event) => onBlockChange(event.target.value)}>
                    {/* Empty value: sending without a choice gives "Block is required." */}
                    <option value="">{t("harvest.form.selectBlock")}</option>
                    {blocks.map((block) => (
                        <option key={block} value={block}>
                            {formatBlock(block)}
                        </option>
                    ))}
                </Select>
            </Field>

            <Field
                id={`${idPrefix}-variety`}
                label={t("harvest.form.variety")}
                error={fieldError("varietyName")}
                hint={varietyNote}
            >
                <Select
                    value={varietyName}
                    disabled={plantedVarieties.length === 0}
                    onChange={(event) => onVarietyChange(event.target.value)}
                >
                    {/* Empty value: sending without a choice gives "Variety is required." */}
                    <option value="">{t("harvest.form.selectVariety")}</option>
                    {plantedVarieties.map((variety) => (
                        <option key={variety.id} value={variety.name}>
                            {variety.name}
                        </option>
                    ))}
                </Select>
            </Field>
        </>
    );
}

/** Cancel and the submit button at the bottom of a harvest form. */
export function FormButtons({ onCancel, isSubmitting, submitDisabled, submitLabel, submittingLabel }) {
    const { t } = useTranslation("plants");
    return (
        <div className="mt-6 flex justify-end gap-2">
            <Button variant="secondary" onClick={onCancel}>
                {t("common:actions.cancel")}
            </Button>
            <Button type="submit" disabled={submitDisabled} loading={isSubmitting}>
                {isSubmitting ? submittingLabel : submitLabel}
            </Button>
        </div>
    );
}
