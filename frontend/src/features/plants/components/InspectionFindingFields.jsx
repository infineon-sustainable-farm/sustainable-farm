import { Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Field, Input, Select } from "../../../shared/ui";
import { CATALOGUE_NOTE_ID, OTHER_ISSUE_CODE } from "../utils/healthIssues";

/**
 * One finding of an inspection: the problem, its name when it is "Other", and
 * the tree. In a fieldset, so each field is read with "Finding 2" before it. A
 * new finding takes the focus on its problem.
 */
export default function InspectionFindingFields({ finding, number, options, catalogueNote, errorOf, onChange, onRemove }) {
    const { t } = useTranslation("plants");
    const id = `inspection-finding-${finding.key}`;
    const isOther = finding.issueCode === OTHER_ISSUE_CODE;

    return (
        <fieldset className="rounded-control border border-border-subtle px-4 pb-4">
            <legend className="px-1 text-sm font-medium text-text-muted">
                {t("health.inspectionForm.findingNumber", { number })}
            </legend>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Field id={`${id}-problem`} label={t("health.inspectionForm.problem")} error={errorOf("issueCode")}>
                    <Select
                        autoFocus
                        value={finding.issueCode}
                        onChange={(event) => onChange("issueCode", event.target.value)}
                        aria-describedby={catalogueNote ? CATALOGUE_NOTE_ID : undefined}
                    >
                        {/* Empty value: sending without a choice gives "Problem is required." */}
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

                {isOther && (
                    <Field
                        id={`${id}-other-label`}
                        label={t("health.otherLabel")}
                        error={errorOf("otherLabel")}
                    >
                        <Input
                            type="text"
                            value={finding.otherLabel}
                            onChange={(event) => onChange("otherLabel", event.target.value)}
                        />
                    </Field>
                )}

                <Field
                    id={`${id}-tree`}
                    label={t("health.inspectionForm.tree")}
                    error={errorOf("treeLabel")}
                    hint={t("health.inspectionForm.treeNote")}
                >
                    <Input
                        type="text"
                        value={finding.treeLabel}
                        onChange={(event) => onChange("treeLabel", event.target.value)}
                    />
                </Field>
            </div>

            <div className="mt-3 flex justify-end">
                <Button
                    size="sm"
                    variant="secondary"
                    icon={Trash2}
                    onClick={onRemove}
                    aria-label={t("health.inspectionForm.removeFinding", { number })}
                >
                    {t("health.inspectionForm.remove")}
                </Button>
            </div>
        </fieldset>
    );
}
