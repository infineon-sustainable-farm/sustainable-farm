import { useEffect, useRef, useState } from "react";
import { Pencil } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button, Field, Input } from "../../../shared/ui";
import { formatEnum, formatShare, formatText, formatTimestamp } from "../utils/format";
import FormAlert from "./FormAlert";

/** Same rule as the API: from 0 to 1, with at most 3 decimals. */
const SHARE_PATTERN = /^\d(\.\d{1,3})?$/;

/**
 * One growth phase: its share in effect and its source, with the default it
 * corrects, if any. Edit opens a small form in place, as for the exchange
 * rate; only one phase is edited at a time.
 */
export default function GrowthPhaseShareItem({ share, isEditing, saveShare, onEdit, onClose }) {
    const { t, i18n } = useTranslation("plants");
    const phase = formatEnum("growthPhase", share.code);
    // German nouns keep their capital inside a sentence.
    const phaseInSentence = i18n.resolvedLanguage === "de" ? phase : phase.toLowerCase();
    const [value, setValue] = useState("");
    // A number field holds "" for text it cannot read, such as "1e"; this
    // keeps such a value from passing as an empty field.
    const [isUnreadable, setIsUnreadable] = useState(false);
    const [source, setSource] = useState("");
    const [clientErrors, setClientErrors] = useState({});
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

    const error = isEditing ? saveShare.error : null;
    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    // A 409 (two first corrections at once) concerns the share itself.
    const conflict = error?.status === 409 ? error.message : null;
    const formError = error && !serverFieldErrors && !conflict ? error.message : null;
    const shareError = clientErrors.yieldShare ?? serverFieldErrors?.yieldShare ?? conflict;
    const sourceError = clientErrors.source ?? serverFieldErrors?.source;
    const id = `share-${share.code.toLowerCase()}`;

    function openForm() {
        setValue(String(share.yieldShare));
        setIsUnreadable(false);
        setSource("");
        setClientErrors({});
        onEdit(share.code);
    }

    function closeForm() {
        returnsFocus.current = true;
        onClose();
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
        const errors = {};
        if (!isUnreadable && text === "") {
            errors.yieldShare = t("harvest.references.shareRequired");
        } else if (isUnreadable || !SHARE_PATTERN.test(text) || Number(text) > 1) {
            errors.yieldShare = t("harvest.references.shareInvalid");
        }
        if (source.length > 255) {
            errors.source = t("harvest.references.sourceTooLong");
        }
        setClientErrors(errors);
        if (Object.keys(errors).length > 0) return;
        // A source left empty is not sent: the API records user_entry.
        const body = { yieldShare: Number(text) };
        if (source.trim()) body.source = source;
        saveShare.mutate({ code: share.code, share: body }, { onSuccess: closeForm });
    }

    const heading = (
        <p className="text-sm font-semibold text-text">
            {phase} <span className="font-normal text-text-muted">({formatText(share.yearsBand)})</span>
        </p>
    );

    return (
        <li>
            {!isEditing && (
                <div className="flex flex-wrap items-center justify-between gap-4">
                    <div>
                        {heading}
                        <p className="mt-0.5 text-lg font-semibold text-text">
                            {formatShare(share.yieldShare)}{" "}
                            <span className="text-sm font-normal text-text-muted">{t("harvest.references.ofFullYield")}</span>
                        </p>
                        <p className="mt-0.5 text-xs text-text-muted">
                            {share.corrected
                                ? t("harvest.references.sourceCorrected", {
                                      source: formatText(share.source),
                                      updated: formatTimestamp(share.lastUpdated),
                                      defaultShare: formatShare(share.defaultYieldShare),
                                      defaultSource: formatText(share.defaultSource),
                                  })
                                : t("harvest.references.sourceDefault", { source: formatText(share.source) })}
                        </p>
                    </div>
                    <Button
                        ref={editButtonRef}
                        variant="secondary"
                        size="sm"
                        icon={Pencil}
                        onClick={openForm}
                        aria-label={t("harvest.references.editShare", { phase: phaseInSentence })}
                    >
                        {t("common:actions.edit")}
                    </Button>
                </div>
            )}

            {isEditing && (
                <form noValidate onSubmit={handleSubmit} onKeyDown={handleKeyDown} className="flex flex-col gap-3">
                    {heading}
                    {formError && <FormAlert>{formError}</FormAlert>}
                    <div className="flex flex-wrap items-start gap-x-4 gap-y-3">
                        <Field
                            id={`${id}-value`}
                            label={t("harvest.references.shareLabel")}
                            error={shareError}
                            className="w-40"
                        >
                            <Input
                                ref={inputRef}
                                type="number"
                                min="0"
                                max="1"
                                step="any"
                                inputMode="decimal"
                                value={value}
                                onChange={changeValue}
                            />
                        </Field>
                        <Field
                            id={`${id}-source`}
                            label={t("harvest.references.sourceLabel")}
                            error={sourceError}
                            hint={t("harvest.references.sourceNote")}
                            className="min-w-0 max-w-sm flex-1"
                        >
                            <Input
                                type="text"
                                value={source}
                                onChange={(event) => setSource(event.target.value)}
                                placeholder={t("harvest.references.userEntry")}
                            />
                        </Field>
                    </div>
                    <div className="flex gap-2">
                        <Button type="submit" loading={saveShare.isPending}>
                            {saveShare.isPending ? t("harvest.references.saving") : t("common:actions.save")}
                        </Button>
                        <Button variant="secondary" onClick={closeForm}>
                            {t("common:actions.cancel")}
                        </Button>
                    </div>
                </form>
            )}
        </li>
    );
}
