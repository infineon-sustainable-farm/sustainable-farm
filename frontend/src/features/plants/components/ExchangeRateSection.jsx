import { useEffect, useRef, useState } from "react";
import { CircleAlert, Pencil } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Card, Input } from "../../../shared/ui";
import { useEurXofRate } from "../hooks/useEurXofRate";
import { useSaveEurXofRate } from "../hooks/useSaveEurXofRate";
import { formatTimestamp } from "../utils/format";
import FormAlert from "./FormAlert";
import { SectionLoadError, SectionLoading } from "./FertilizerSectionStatus";

/** Same rule as the API: above 0 and below 1,000,000, with at most 6 decimals. */
const RATE_PATTERN = /^\d{1,6}(\.\d{1,6})?$/;

const NUMBER_LOCALES = { en: "en-US", de: "de-DE", fr: "fr-FR" };

/** A rate with all its decimals, up to the 6 the API keeps: "655.957", "656". */
function formatRate(rate, language) {
    return Number(rate).toLocaleString(NUMBER_LOCALES[language] ?? NUMBER_LOCALES.en, { maximumFractionDigits: 6 });
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
    const { t, i18n } = useTranslation("plants");
    const query = useEurXofRate();
    const { data: rate, isPending, isError } = query;
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
            error = t("fertilizer.rate.required");
        } else if (isUnreadable || !RATE_PATTERN.test(text) || Number(text) <= 0) {
            error = t("fertilizer.rate.invalid");
        }
        setClientError(error);
        if (error) return;
        saveRate.mutate({ rate: Number(text) }, { onSuccess: closeForm });
    }

    return (
        <section aria-labelledby="exchange-rate-title" className="flex flex-col gap-4">
            <h2 id="exchange-rate-title" className="text-lg font-semibold">
                {t("fertilizer.rate.title")}
            </h2>

            {isPending && <SectionLoading>{t("fertilizer.rate.loading")}</SectionLoading>}

            {isError && <SectionLoadError title={t("fertilizer.rate.loadError")} query={query} />}

            {!isPending && !isError && (
                <Card>
                    {!isEditing && (
                        <div className="flex flex-wrap items-center justify-between gap-4">
                            {rate ? (
                                <div>
                                    <p className="text-lg font-semibold text-text">
                                        {t("fertilizer.rate.value", { rate: formatRate(rate.rate, i18n.resolvedLanguage) })}
                                    </p>
                                    <p className="mt-0.5 text-xs text-text-muted">
                                        {t("fertilizer.rate.meta", {
                                            source: rate.source,
                                            updated: formatTimestamp(rate.lastUpdated),
                                        })}
                                    </p>
                                </div>
                            ) : (
                                <div>
                                    <p className="text-sm font-semibold text-text">{t("fertilizer.rate.none")}</p>
                                    <p className="mt-0.5 text-xs text-text-muted">{t("fertilizer.rate.noneHint")}</p>
                                </div>
                            )}
                            <Button
                                ref={editButtonRef}
                                variant="secondary"
                                icon={Pencil}
                                onClick={openForm}
                                aria-label={t("fertilizer.rate.editLabel")}
                            >
                                {t("fertilizer.rate.edit")}
                            </Button>
                        </div>
                    )}

                    {isEditing && (
                        <form noValidate onSubmit={handleSubmit} onKeyDown={handleKeyDown} className="flex flex-col gap-3">
                            {formError && <FormAlert>{formError}</FormAlert>}
                            <div className="flex flex-wrap items-center gap-x-2 gap-y-3">
                                <label htmlFor="eur-xof-rate" className="text-sm font-semibold text-text">
                                    {t("fertilizer.rate.fieldLabel")}
                                </label>
                                <div className="w-36">
                                    <Input
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
                                    />
                                </div>
                                <span id="eur-xof-rate-unit" className="text-sm font-semibold text-text">
                                    {t("fertilizer.rate.unit")}
                                </span>
                                <div className="flex gap-2 sm:ml-2">
                                    <Button type="submit" loading={saveRate.isPending}>
                                        {saveRate.isPending ? t("fertilizer.rate.saving") : t("common:actions.save")}
                                    </Button>
                                    <Button variant="secondary" onClick={closeForm}>
                                        {t("common:actions.cancel")}
                                    </Button>
                                </div>
                            </div>
                            {fieldError && (
                                <p id="eur-xof-rate-error" className="flex items-center gap-1 text-xs text-error-fg">
                                    <CircleAlert size={14} aria-hidden="true" className="shrink-0" />
                                    <span>{fieldError}</span>
                                </p>
                            )}
                        </form>
                    )}
                </Card>
            )}
        </section>
    );
}
