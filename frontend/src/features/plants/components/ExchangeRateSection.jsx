import { useEffect, useRef, useState } from "react";
import { Loader2, Pencil, TriangleAlert } from "lucide-react";
import { useEurXofRate } from "../hooks/useEurXofRate";
import { useSaveEurXofRate } from "../hooks/useSaveEurXofRate";
import { formatTimestamp } from "../utils/format";

/** Same rule as the API: above 0 and below 1,000,000, with at most 6 decimals. */
const RATE_PATTERN = /^\d{1,6}(\.\d{1,6})?$/;
const RATE_ERROR = "Rate must be a number greater than 0 and less than 1,000,000, with at most 6 decimals.";

/** A rate with all its decimals, up to the 6 the API keeps: "655.957", "656". */
function formatRate(rate) {
    return Number(rate).toLocaleString("en-US", { maximumFractionDigits: 6 });
}

/**
 * How many FCFA one euro is worth: the rate the API uses to give each purchase
 * cost in both currencies.
 *
 * Outside the dev profile no rate exists until one is entered here. The API
 * then answers 404, shown as "No exchange rate recorded yet", and each cost
 * is shown only in the currency it was paid in. Edit opens a small form in
 * place; its check mirrors the API rule, and what the API still refuses is
 * shown as in the dialogs: a 400 under the field, any other error above it.
 */
export default function ExchangeRateSection() {
    const { data: rate, isPending, isError, refetch, isFetching } = useEurXofRate();
    const saveRate = useSaveEurXofRate();
    const [isEditing, setIsEditing] = useState(false);
    const [value, setValue] = useState("");
    // A number field holds "" for text it cannot read, such as "1e"; this
    // keeps such a value from passing as an empty field.
    const [isUnreadable, setIsUnreadable] = useState(false);
    const [clientError, setClientError] = useState(null);
    const inputRef = useRef(null);
    const editButtonRef = useRef(null);
    // Focus goes back to Edit when the form closes, never on the first render.
    const returnsFocus = useRef(false);

    useEffect(() => {
        if (isEditing) {
            inputRef.current?.focus();
        } else if (returnsFocus.current) {
            returnsFocus.current = false;
            editButtonRef.current?.focus();
        }
    }, [isEditing]);

    const serverFieldError = saveRate.error?.status === 400 ? saveRate.error.data?.fieldErrors?.rate : null;
    const formError = saveRate.error && !serverFieldError ? saveRate.error.message : null;
    const fieldError = clientError ?? serverFieldError;

    function openForm() {
        saveRate.reset();
        setValue(rate ? String(rate.rate) : "");
        setIsUnreadable(false);
        setClientError(null);
        setIsEditing(true);
    }

    function closeForm() {
        saveRate.reset();
        returnsFocus.current = true;
        setIsEditing(false);
    }

    function changeValue(event) {
        setValue(event.target.value);
        setIsUnreadable(event.target.validity.badInput);
    }

    function handleKeyDown(event) {
        if (event.key === "Escape") {
            event.preventDefault();
            closeForm();
        }
    }

    function handleSubmit(event) {
        event.preventDefault();
        const text = value.trim();
        let error = null;
        if (!isUnreadable && text === "") {
            error = "Rate is required.";
        } else if (isUnreadable || !RATE_PATTERN.test(text) || Number(text) <= 0) {
            error = RATE_ERROR;
        }
        setClientError(error);
        if (error) return;
        saveRate.mutate({ rate: Number(text) }, { onSuccess: closeForm });
    }

    return (
        <section aria-labelledby="exchange-rate-title" className="flex flex-col gap-4">
            <h3 id="exchange-rate-title" className="font-heading text-base font-bold text-gray-900">
                Exchange rate
            </h3>

            {isPending && (
                <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-4 text-sm text-gray-500">
                    <Loader2 size={18} className="animate-spin" />
                    Loading the exchange rate…
                </div>
            )}

            {isError && (
                <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-error/30 bg-error/5 px-5 py-4">
                    <div className="flex items-start gap-3">
                        <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                        <div>
                            <p className="text-sm font-bold text-gray-900">The exchange rate could not be loaded</p>
                            {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1, as in the other sections. */}
                            <p className="mt-0.5 text-sm text-gray-600">The request to the server did not succeed.</p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={() => refetch()}
                        disabled={isFetching}
                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                    >
                        {isFetching ? "Retrying…" : "Retry"}
                    </button>
                </div>
            )}

            {!isPending && !isError && (
                <div className="rounded-xl border border-gray-200 bg-white px-5 py-4">
                    {!isEditing && (
                        <div className="flex flex-wrap items-center justify-between gap-4">
                            {rate ? (
                                <div>
                                    <p className="text-lg font-semibold text-gray-900">
                                        1 € = {formatRate(rate.rate)} FCFA
                                    </p>
                                    <p className="mt-0.5 text-xs text-gray-600">
                                        Source: {rate.source} · Updated {formatTimestamp(rate.lastUpdated)}
                                    </p>
                                </div>
                            ) : (
                                <div>
                                    <p className="text-sm font-semibold text-gray-900">No exchange rate recorded yet</p>
                                    <p className="mt-0.5 text-xs text-gray-600">
                                        Until one is entered, each cost is shown only in the currency it was paid in.
                                    </p>
                                </div>
                            )}
                            <button
                                ref={editButtonRef}
                                type="button"
                                onClick={openForm}
                                aria-label="Edit the exchange rate"
                                className="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
                            >
                                <Pencil size={16} />
                                Edit
                            </button>
                        </div>
                    )}

                    {isEditing && (
                        <form noValidate onSubmit={handleSubmit} onKeyDown={handleKeyDown} className="flex flex-col gap-3">
                            {/* Text in the darkened error hue of the dialogs: #9a3f32 on the tinted
                                alert (#fcf6f5) -> 6.27:1. */}
                            {formError && (
                                <p
                                    role="alert"
                                    className="rounded-lg border border-error/30 bg-error/5 px-3 py-2 text-sm text-[#9a3f32]"
                                >
                                    {formError}
                                </p>
                            )}
                            <div className="flex flex-wrap items-center gap-x-2 gap-y-3">
                                <label htmlFor="eur-xof-rate" className="text-sm font-semibold text-gray-700">
                                    1 € =
                                </label>
                                <input
                                    ref={inputRef}
                                    id="eur-xof-rate"
                                    type="number"
                                    min="0"
                                    step="any"
                                    inputMode="decimal"
                                    value={value}
                                    onChange={changeValue}
                                    aria-invalid={fieldError ? true : undefined}
                                    aria-describedby={["eur-xof-rate-unit", fieldError ? "eur-xof-rate-error" : null]
                                        .filter(Boolean)
                                        .join(" ")}
                                    className="w-36 rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none"
                                />
                                <span id="eur-xof-rate-unit" className="text-sm font-semibold text-gray-700">
                                    FCFA
                                </span>
                                <div className="flex gap-2 sm:ml-2">
                                    <button
                                        type="submit"
                                        disabled={saveRate.isPending}
                                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                                    >
                                        {saveRate.isPending ? "Saving…" : "Save"}
                                    </button>
                                    <button
                                        type="button"
                                        onClick={closeForm}
                                        className="rounded-lg border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </div>
                            {fieldError && (
                                <p id="eur-xof-rate-error" className="text-xs text-error">
                                    {fieldError}
                                </p>
                            )}
                        </form>
                    )}
                </div>
            )}
        </section>
    );
}
