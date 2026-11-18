import { useTranslation } from "react-i18next";
import { Field, Input, Select } from "../../../shared/ui";
import { OTHER_ISSUE_CODE } from "../utils/healthIssues";

/**
 * Where and against what a preventive treatment is applied: its block, typed
 * as for an inspection with the known blocks as suggestions, and its problem,
 * the catalogue then "Other" once and last, with the name of the problem.
 */
export default function TreatmentTargetFields({ values, blocks, options, catalogueNote, fieldError, onChange, onTargetChange }) {
    const { t } = useTranslation("plants");
    return (
        <>
            <Field id="treatment-block" label={t("health.block")} error={fieldError("blockCode")} hint={t("health.blockNote")}>
                <Input
                    type="text"
                    autoFocus
                    list="treatment-block-options"
                    value={values.blockCode}
                    onChange={(event) => onChange("blockCode", event.target.value)}
                />
            </Field>
            <datalist id="treatment-block-options">
                {blocks.map((block) => (
                    <option key={block} value={block} />
                ))}
            </datalist>

            <Field
                id="treatment-target"
                label={t("health.treatmentForm.target")}
                error={fieldError("targetIssueCode")}
                hint={catalogueNote}
            >
                <Select value={values.targetIssueCode} onChange={(event) => onTargetChange(event.target.value)}>
                    {/* Empty value: sending without a choice gives "Problem targeted is required." */}
                    <option value="">{t("health.selectProblem")}</option>
                    {options.groups.map((group) => (
                        <optgroup key={group.label} label={group.label}>
                            {group.issues.map((issue) => (
                                <option key={issue.code} value={issue.code}>
                                    {issue.name}
                                </option>
                            ))}
                        </optgroup>
                    ))}
                    <option value={options.other.code}>{options.other.name}</option>
                </Select>
            </Field>

            {values.targetIssueCode === OTHER_ISSUE_CODE && (
                <Field
                    id="treatment-other-label"
                    label={t("health.otherLabel")}
                    error={fieldError("targetOtherLabel")}
                    className="sm:col-span-2"
                >
                    <Input
                        type="text"
                        value={values.targetOtherLabel}
                        onChange={(event) => onChange("targetOtherLabel", event.target.value)}
                    />
                </Field>
            )}
        </>
    );
}
