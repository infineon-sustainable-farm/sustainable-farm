import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Field, Input, Modal, Select } from "../../../shared/ui";
import {
    measuredValueError,
    measuredValueMessage,
    METHOD_OPTIONS,
    methodTakesValue,
    methodUnit,
    observationRequest,
    STATUS_OPTIONS,
} from "../utils/maturityObservations";
import FormAlert from "./FormAlert";
import { todayAsIsoDate, usePlantedVarieties } from "../hooks/usePlantedVarieties";
import { BlockAndVarietyFields, FormButtons } from "./HarvestFormParts";

/**
 * Form of a maturity observation: what an observer saw on one variety of one
 * block, on one day (PL-05-01, "Manual entry").
 *
 * The block and the variety are picked from lists, as for a harvest, so the
 * variety name goes out exactly as it was planted; only the varieties with a
 * recorded planting on the block are offered. A measured value is offered
 * with Brix and dry matter only, in their unit, and another method starts
 * without one. No farm is sent, as for a harvest.
 *
 * The checks mirror the API rules; what the API still refuses is shown the
 * same way as for a harvest:
 * - a 400 with fieldErrors: each message under its field;
 * - a 422 (no planting of that variety on the block, or an observation before
 *   the planting), or any other error: one message above the form.
 */
export default function RecordMaturityObservationModal({ blocks, isSubmitting, error, onSubmit, onClose }) {
    const { t } = useTranslation("plants");
    const [observedOn, setObservedOn] = useState("");
    const [blockCode, setBlockCode] = useState("");
    const [varietyName, setVarietyName] = useState("");
    const [maturityStatus, setMaturityStatus] = useState("");
    const [method, setMethod] = useState("");
    const [measuredValue, setMeasuredValue] = useState("");
    // A number field holds "" for text it cannot read, such as "1e"; this
    // keeps such a value from passing as an empty one.
    const [isValueUnreadable, setIsValueUnreadable] = useState(false);
    const [observer, setObserver] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);

    const { plantedVarieties, varietyNote } = usePlantedVarieties(blockCode);

    const takesValue = methodTakesValue(method);
    const unit = methodUnit(method);

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    function changeBlock(value) {
        setBlockCode(value);
        // The variety belongs to the block: another block means another list.
        setVarietyName("");
    }

    function changeMethod(value) {
        setMethod(value);
        // A value is read with its method, in its unit: another method starts without one.
        setMeasuredValue("");
        setIsValueUnreadable(false);
        setFieldErrors((current) =>
            Object.fromEntries(Object.entries(current).filter(([field]) => field !== "measuredValue")));
    }

    function changeMeasuredValue(event) {
        setMeasuredValue(event.target.value);
        setIsValueUnreadable(event.target.validity.badInput);
    }

    function validate() {
        const errors = {};
        if (!observedOn) {
            errors.observedOn = t("harvest.observation.errors.dateRequired");
        } else if (observedOn > today) {
            errors.observedOn = t("harvest.observation.errors.dateFuture");
        }
        if (!blockCode) {
            errors.blockCode = t("harvest.form.blockRequired");
        }
        if (!varietyName) {
            errors.varietyName = t("harvest.form.varietyRequired");
        }
        if (!maturityStatus) {
            errors.maturityStatus = t("harvest.observation.errors.statusRequired");
        }
        if (!method) {
            errors.method = t("harvest.observation.errors.methodRequired");
        }
        if (takesValue) {
            const valueError = isValueUnreadable ? measuredValueMessage() : measuredValueError(measuredValue);
            if (valueError) errors.measuredValue = valueError;
        }
        if (observer.length > 255) {
            errors.observer = t("harvest.observation.errors.observerTooLong");
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit(observationRequest({
            observedOn,
            blockCode,
            varietyName,
            maturityStatus,
            method,
            measuredValue,
            observer,
        }));
    }

    return (
        <Modal open onClose={onClose} title={t("harvest.observation.title")} size="xl">
            <form noValidate onSubmit={handleSubmit}>
                {formError && <FormAlert className="mb-5">{formError}</FormAlert>}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <Field id="observation-date" label={t("harvest.observation.date")} error={fieldError("observedOn")}>
                        <Input
                            type="date"
                            max={today}
                            value={observedOn}
                            onChange={(event) => setObservedOn(event.target.value)}
                        />
                    </Field>

                    <BlockAndVarietyFields
                        idPrefix="observation"
                        blocks={blocks}
                        blockCode={blockCode}
                        onBlockChange={changeBlock}
                        varietyName={varietyName}
                        onVarietyChange={setVarietyName}
                        plantedVarieties={plantedVarieties}
                        varietyNote={varietyNote}
                        fieldError={fieldError}
                    />

                    <Field id="observation-status" label={t("harvest.observation.status")} error={fieldError("maturityStatus")}>
                        <Select value={maturityStatus} onChange={(event) => setMaturityStatus(event.target.value)}>
                            <option value="">{t("harvest.observation.selectStatus")}</option>
                            {STATUS_OPTIONS.map((status) => (
                                <option key={status.code} value={status.code}>
                                    {status.label}
                                </option>
                            ))}
                        </Select>
                    </Field>

                    <Field id="observation-method" label={t("harvest.observation.method")} error={fieldError("method")}>
                        <Select value={method} onChange={(event) => changeMethod(event.target.value)}>
                            <option value="">{t("harvest.observation.selectMethod")}</option>
                            {METHOD_OPTIONS.map((option) => (
                                <option key={option.code} value={option.code}>
                                    {option.label}
                                </option>
                            ))}
                        </Select>
                    </Field>

                    <Field
                        id="observation-value"
                        label={unit
                            ? t("harvest.observation.measuredValueUnit", { unit })
                            : t("harvest.observation.measuredValue")}
                        error={fieldError("measuredValue")}
                        hint={takesValue ? t("harvest.observation.valueHint") : t("harvest.observation.valueDisabledHint")}
                    >
                        <Input
                            type="number"
                            min="0"
                            max="100"
                            step="0.1"
                            inputMode="decimal"
                            disabled={!takesValue}
                            value={measuredValue}
                            onChange={changeMeasuredValue}
                        />
                    </Field>

                    <Field
                        id="observation-observer"
                        label={t("harvest.observation.observer")}
                        error={fieldError("observer")}
                        hint={t("harvest.observation.optional")}
                        className="sm:col-span-2"
                    >
                        <Input type="text" value={observer} onChange={(event) => setObserver(event.target.value)} />
                    </Field>
                </div>

                <FormButtons
                    onCancel={onClose}
                    isSubmitting={isSubmitting}
                    submitDisabled={varietyNote !== null}
                    submitLabel={t("harvest.observation.submit")}
                    submittingLabel={t("harvest.form.recording")}
                />
            </form>
        </Modal>
    );
}
