import { useState } from "react";

const MONTHS_OPTIONS = [6, 12, 18, 24];

/**
 * The API's month format. Browsers without a month picker turn the field into
 * plain text, so the value is checked before it is ever sent.
 */
const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

const LABEL_CLASS = "text-xs font-semibold text-gray-500";
const CONTROL_CLASS =
    "rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm text-gray-800 focus:border-primary focus:outline-none";

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
    // What the "From" field holds; null until the user changes it.
    const [fromInput, setFromInput] = useState(null);

    const fromError = fromInput === null || MONTH_PATTERN.test(fromInput) ? null : "Use the format YYYY-MM.";

    function changeFrom(value) {
        setFromInput(value);
        if (MONTH_PATTERN.test(value)) onFromChange(value);
    }

    return (
        <div className="flex flex-wrap items-start gap-x-6 gap-y-3 rounded-xl border border-gray-200 bg-white px-4 py-3">
            <div>
                <div className="flex items-center gap-3">
                    <label id={`${idPrefix}-from-label`} htmlFor={`${idPrefix}-from`} className={LABEL_CLASS}>
                        From
                    </label>
                    <input
                        id={`${idPrefix}-from`}
                        type="month"
                        value={fromInput ?? answeredFrom ?? ""}
                        onChange={(event) => changeFrom(event.target.value)}
                        aria-labelledby={`${titleId} ${idPrefix}-from-label`}
                        className={CONTROL_CLASS}
                        {...(fromError
                            ? { "aria-invalid": true, "aria-describedby": `${idPrefix}-from-error` }
                            : {})}
                    />
                </div>
                {fromError && (
                    <p id={`${idPrefix}-from-error`} className="mt-1 text-xs text-error">
                        {fromError}
                    </p>
                )}
            </div>

            <div className="flex items-center gap-3">
                <label id={`${idPrefix}-months-label`} htmlFor={`${idPrefix}-months`} className={LABEL_CLASS}>
                    Months
                </label>
                <select
                    id={`${idPrefix}-months`}
                    value={months}
                    onChange={(event) => onMonthsChange(Number(event.target.value))}
                    aria-labelledby={`${titleId} ${idPrefix}-months-label`}
                    className={CONTROL_CLASS}
                >
                    {MONTHS_OPTIONS.map((option) => (
                        <option key={option} value={option}>
                            {option}
                        </option>
                    ))}
                </select>
            </div>
        </div>
    );
}
