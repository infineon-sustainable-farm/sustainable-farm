import { useState } from "react";
import { useTranslation } from "react-i18next";
import { CircleAlert } from "lucide-react";
import { Input, Select } from "../../../shared/ui";

const MONTHS_OPTIONS = [6, 12, 18, 24];

/**
 * The API's month format. Browsers without a month picker turn the field into
 * plain text, so the value is checked before it is ever sent.
 */
const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

const LABEL_CLASS = "text-sm font-medium";

/**
 * Period of a monthly read: a first month and a number of months. Until a
 * month is picked, the API chooses the first month, and the "From" field shows
 * the month it answered with (`answeredFrom`). An invalid month is never
 * passed on: the values shown stay those of the last valid one.
 *
 * `idPrefix` names the fields, and each accessible name joins the section
 * title (`titleId`) and the field label, so two sections can tell their
 * "From" apart.
 */
export default function MonthPeriodControls({ idPrefix, titleId, answeredFrom, onFromChange, months, onMonthsChange }) {
    const { t } = useTranslation("plants");
    // What the "From" field holds; null until the user changes it.
    const [fromInput, setFromInput] = useState(null);

    const fromError = fromInput === null || MONTH_PATTERN.test(fromInput) ? null : t("period.fromFormat");

    function changeFrom(value) {
        setFromInput(value);
        if (MONTH_PATTERN.test(value)) onFromChange(value);
    }

    return (
        <div className="flex flex-wrap items-start gap-x-6 gap-y-3 rounded-tile border border-border-subtle bg-surface-raised px-4 py-3">
            <div>
                <div className="flex items-center gap-3">
                    <label id={`${idPrefix}-from-label`} htmlFor={`${idPrefix}-from`} className={LABEL_CLASS}>
                        {t("period.from")}
                    </label>
                    <div>
                        <Input
                            id={`${idPrefix}-from`}
                            type="month"
                            value={fromInput ?? answeredFrom ?? ""}
                            onChange={(event) => changeFrom(event.target.value)}
                            aria-labelledby={`${titleId} ${idPrefix}-from-label`}
                           
                            {...(fromError
                                ? { "aria-invalid": true, "aria-describedby": `${idPrefix}-from-error` }
                                : {})}
                        />
                    </div>
                </div>
                {fromError && (
                    <p id={`${idPrefix}-from-error`} className="mt-1 flex items-center gap-1 text-xs text-error-fg">
                        <CircleAlert size={14} aria-hidden="true" className="shrink-0" />
                        <span>{fromError}</span>
                    </p>
                )}
            </div>

            <div className="flex items-center gap-3">
                <label id={`${idPrefix}-months-label`} htmlFor={`${idPrefix}-months`} className={LABEL_CLASS}>
                    {t("period.months")}
                </label>
                <div>
                    <Select
                        id={`${idPrefix}-months`}
                        value={months}
                        onChange={(event) => onMonthsChange(Number(event.target.value))}
                        aria-labelledby={`${titleId} ${idPrefix}-months-label`}
                       
                    >
                        {MONTHS_OPTIONS.map((option) => (
                            <option key={option} value={option}>
                                {option}
                            </option>
                        ))}
                    </Select>
                </div>
            </div>
        </div>
    );
}
