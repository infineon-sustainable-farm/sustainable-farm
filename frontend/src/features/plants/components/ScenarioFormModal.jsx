import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Field, Input, Modal } from "../../../shared/ui";
import { useVarietyReferences } from "../hooks/useVarietyReferences";
import { formatKilograms, formatText } from "../utils/format";
import {
    SCENARIO_CURRENCIES,
    sameVarietyName,
    scenarioFormValues,
    scenarioRequestBody,
    SOURCE_FIELDS,
    validateScenarioForm,
} from "../utils/scenarioRequests";
import FormAlert from "./FormAlert";

/** The values of a scenario, in the order of the form; their labels and notes are under scenarios.form.fields. */
const VALUE_FIELDS = [
    { field: "treeCount", required: true, inputMode: "numeric", hasNote: true },
    { field: "yieldPerTreeKg", required: true, inputMode: "decimal", hasNote: false },
    { field: "lossRate", required: false, inputMode: "decimal", hasNote: true },
    { field: "pricePerKg", required: false, inputMode: "decimal", hasNote: true },
    { field: "annualFixedCost", required: true, inputMode: "decimal", hasNote: false },
    { field: "variableCostPerKg", required: false, inputMode: "decimal", hasNote: true },
    { field: "initialInvestment", required: true, inputMode: "decimal", hasNote: false },
    { field: "costPerAdditionalTree", required: false, inputMode: "decimal", hasNote: true },
];

/** The fields of the form, by the id of their input. */
function inputId(field) {
    return `scenario-${field}`;
}

/** A label, with "(optional)" after it for a value that may stay empty. */
function FieldLabel({ label, required }) {
    const { t } = useTranslation("plants");
    return (
        <>
            {label}
            {!required && <span className="font-normal text-text-muted">{t("scenarios.optionalSuffix")}</span>}
        </>
    );
}

/**
 * Form of a planting scenario: its name, currency and assumptions, each value
 * with an optional source. `scenario` is the recorded one to correct, or null
 * to enter a new one.
 *
 * The checks mirror the API's 400 rules; what the API still refuses is shown
 * under the field it names, and a name already taken (409), like any other
 * error, above the form. The sources start empty: a new value is recorded
 * with user_entry, and a correction keeps the recorded source of a value that
 * does not change. When the variety has an agronomic reference, its yield per
 * tree is offered, with its source; "Use this value" copies both as they are.
 */
export default function ScenarioFormModal({ scenario, isSubmitting, error, onSubmit, onClose }) {
    const { t } = useTranslation("plants");
    const isEditing = scenario !== null;
    const [values, setValues] = useState(() => scenarioFormValues(scenario));
    const [fieldErrors, setFieldErrors] = useState({});
    const { data: references } = useVarietyReferences();

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field] ?? null;
    const reference = (references ?? []).find((candidate) => sameVarietyName(candidate.varietyName, values.varietyName));
    const title = isEditing ? t("scenarios.editName", { name: scenario.name }) : t("scenarios.form.newTitle");

    function setValue(field, value) {
        setValues((current) => ({ ...current, [field]: value }));
    }

    function copyReferenceYield() {
        setValues((current) => ({
            ...current,
            yieldPerTreeKg: String(reference.yieldPerTreeKg),
            yieldSource: reference.yieldSource ?? "",
        }));
    }

    const currencyError = fieldError("currency");

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validateScenarioForm(values);
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit(scenarioRequestBody(values));
    }

    return (
        <Modal open onClose={onClose} title={title} size="xl">
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                <p className="mb-5 text-sm text-text-muted">{t("scenarios.form.intro")}</p>

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <Field id={inputId("name")} label={t("scenarios.form.name")} required error={fieldError("name")}>
                        <Input type="text" value={values.name} onChange={(event) => setValue("name", event.target.value)} />
                    </Field>

                    <Field
                        id={inputId("varietyName")}
                        label={<FieldLabel label={t("scenarios.form.variety")} />}
                        error={fieldError("varietyName")}
                    >
                        <Input
                            type="text"
                            list="scenario-variety-options"
                            value={values.varietyName}
                            onChange={(event) => setValue("varietyName", event.target.value)}
                        />
                        <datalist id="scenario-variety-options">
                            {(references ?? []).map((candidate) => (
                                <option key={candidate.id} value={candidate.varietyName} />
                            ))}
                        </datalist>
                    </Field>

                    <fieldset className="sm:col-span-2">
                        <legend className="text-sm font-medium">{t("scenarios.form.currency")}</legend>
                        <div className="mt-2 flex flex-wrap gap-x-6 gap-y-2">
                            {SCENARIO_CURRENCIES.map((currency) => (
                                <label key={currency} className="flex items-center gap-2 text-sm text-text">
                                    <input
                                        type="radio"
                                        name="scenario-currency"
                                        value={currency}
                                        checked={values.currency === currency}
                                        onChange={() => setValue("currency", currency)}
                                        aria-invalid={currencyError ? true : undefined}
                                        aria-describedby={currencyError ? "scenario-currency-error" : "scenario-currency-note"}
                                        className="accent-primary"
                                    />
                                    {t(`scenarios.currencies.${currency}`)}
                                </label>
                            ))}
                        </div>
                        {currencyError && (
                            <p id="scenario-currency-error" className="mt-1 text-xs text-error-fg">
                                {currencyError}
                            </p>
                        )}
                        <p id="scenario-currency-note" className="mt-1 text-xs text-text-muted">
                            {t("scenarios.form.currencyNote")}
                        </p>
                    </fieldset>

                    {VALUE_FIELDS.map(({ field, required, inputMode, hasNote }) => {
                        const id = inputId(field);
                        const sourceField = SOURCE_FIELDS[field];
                        const recordedSource = isEditing ? scenario[sourceField] : null;
                        const showsReference = field === "yieldPerTreeKg" && reference;
                        return (
                            <div key={field} className="contents">
                                <Field
                                    id={id}
                                    label={<FieldLabel label={t(`scenarios.form.fields.${field}.label`)} required={required} />}
                                    required={required}
                                    error={fieldError(field)}
                                    hint={hasNote ? t(`scenarios.form.fields.${field}.note`) : null}
                                >
                                    <Input
                                        type="text"
                                        inputMode={inputMode}
                                        value={values[field]}
                                        onChange={(event) => setValue(field, event.target.value)}
                                        aria-describedby={showsReference ? `${id}-reference` : undefined}
                                    />
                                    {showsReference && (
                                        <div id={`${id}-reference`} className="mt-1 flex flex-wrap items-center gap-2 text-xs text-text-muted">
                                            <span>
                                                {t("scenarios.form.reference", {
                                                    variety: reference.varietyName,
                                                    yield: formatKilograms(reference.yieldPerTreeKg),
                                                    source: formatText(reference.yieldSource),
                                                })}
                                            </span>
                                            <Button variant="secondary" size="sm" onClick={copyReferenceYield}>
                                                {t("scenarios.form.useReference")}
                                            </Button>
                                        </div>
                                    )}
                                </Field>

                                <Field
                                    id={inputId(sourceField)}
                                    label={<FieldLabel label={t(`scenarios.form.fields.${field}.source`)} />}
                                    error={fieldError(sourceField)}
                                    hint={recordedSource ? t("scenarios.form.recorded", { source: recordedSource }) : null}
                                >
                                    <Input
                                        type="text"
                                        value={values[sourceField]}
                                        onChange={(event) => setValue(sourceField, event.target.value)}
                                    />
                                </Field>
                            </div>
                        );
                    })}
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <Button variant="secondary" onClick={onClose}>
                        {t("scenarios.form.cancel")}
                    </Button>
                    <Button type="submit" disabled={isSubmitting}>
                        {isSubmitting ? t("scenarios.form.saving") : t("scenarios.form.save")}
                    </Button>
                </div>
            </form>
        </Modal>
    );
}
