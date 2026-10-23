import { useEffect, useRef, useState } from "react";
import { Loader2, Pencil, Plus, TriangleAlert } from "lucide-react";
import { useGrowthPhaseShares } from "../hooks/useGrowthPhaseShares";
import { useSaveGrowthPhaseShare } from "../hooks/useSaveGrowthPhaseShare";
import { useSaveVarietyReference } from "../hooks/useSaveVarietyReference";
import { useVarietyReferences } from "../hooks/useVarietyReferences";
import { formatCode, formatKilograms, formatSeason, formatShare, formatText, formatTimestamp } from "../utils/format";
import PlantsEmptyState from "./PlantsEmptyState";
import VarietyReferenceModal from "./VarietyReferenceModal";

const SUBTITLE_CLASS = "font-heading text-sm font-bold text-gray-900";
const TH_CLASS =
    "border-b border-gray-200 bg-gray-50 px-4 py-3 text-left text-xs font-semibold tracking-wide text-gray-500 uppercase";
const EDIT_BUTTON_CLASS =
    "inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm font-semibold text-gray-700 hover:bg-gray-50";
const INPUT_CLASS =
    "rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none";

/** Same rule as the API: from 0 to 1, with at most 3 decimals. */
const SHARE_PATTERN = /^\d(\.\d{1,3})?$/;
const SHARE_ERROR = "Share must be between 0 and 1, with at most 3 decimals.";

function LoadingBox({ label }) {
    return (
        <div className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-5 py-4 text-sm text-gray-500">
            <Loader2 size={18} className="animate-spin" />
            {label}
        </div>
    );
}

function ErrorBox({ title, onRetry, isRetrying }) {
    return (
        <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-error/30 bg-error/5 px-5 py-4">
            <div className="flex items-start gap-3">
                <TriangleAlert size={20} className="mt-0.5 shrink-0 text-error" />
                <div>
                    <p className="text-sm font-bold text-gray-900">{title}</p>
                    {/* gray-600: #4a5565 on the tinted box (#f6f1f1) -> 6.76:1, as in the other sections. */}
                    <p className="mt-0.5 text-sm text-gray-600">The request to the server did not succeed.</p>
                </div>
            </div>
            <button
                type="button"
                onClick={onRetry}
                disabled={isRetrying}
                className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
            >
                {isRetrying ? "Retrying…" : "Retry"}
            </button>
        </div>
    );
}

/** The reference of each variety, in the order of the API, each with an Edit button. */
function VarietyReferencesTable({ references, onEdit }) {
    return (
        // relative: the hidden "Actions" header is absolutely positioned. Without a positioned box
        // here it would sit outside the scrolling area and widen the whole page on a phone.
        <div className="relative overflow-x-auto rounded-xl border border-gray-200 bg-white">
            <table className="w-full border-collapse text-sm whitespace-nowrap">
                <thead>
                    <tr>
                        <th scope="col" className={TH_CLASS}>Variety</th>
                        <th scope="col" className={TH_CLASS}>Yield per tree</th>
                        <th scope="col" className={TH_CLASS}>Harvest season</th>
                        <th scope="col" className={TH_CLASS}>Sources</th>
                        <th scope="col" className={TH_CLASS}>
                            <span className="sr-only">Actions</span>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {references.map((reference) => (
                        <tr key={reference.id} className="border-b border-gray-100 last:border-b-0">
                            <td className="px-4 py-3 font-medium text-gray-800">
                                {formatText(reference.varietyName)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatKilograms(reference.yieldPerTreeKg)}
                            </td>
                            <td className="px-4 py-3 text-gray-700">
                                {formatSeason(reference.harvestStartMonth, reference.harvestEndMonth)}
                            </td>
                            {/* Allowed to wrap, as in the forecast table: two sources on one line
                                would push the table into a sideways scroll. */}
                            <td className="min-w-56 px-4 py-3 whitespace-normal text-gray-700">
                                {`Yield: ${formatText(reference.yieldSource)} · `
                                    + `Season: ${formatText(reference.seasonSource)}`}
                            </td>
                            <td className="px-4 py-3 text-right">
                                <button
                                    type="button"
                                    onClick={() => onEdit(reference)}
                                    aria-label={`Edit ${reference.varietyName}`}
                                    className={EDIT_BUTTON_CLASS}
                                >
                                    <Pencil size={14} />
                                    Edit
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

/**
 * One growth phase: its share in effect and its source, with the default it
 * corrects, if any. Edit opens a small form in place, as for the exchange
 * rate; only one phase is edited at a time.
 */
function GrowthPhaseShareItem({ share, isEditing, saveShare, onEdit, onClose }) {
    const phase = formatCode(share.code);
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
            errors.yieldShare = "Share is required.";
        } else if (isUnreadable || !SHARE_PATTERN.test(text) || Number(text) > 1) {
            errors.yieldShare = SHARE_ERROR;
        }
        if (source.length > 255) {
            errors.source = "Source must be at most 255 characters.";
        }
        setClientErrors(errors);
        if (Object.keys(errors).length > 0) return;
        // A source left empty is not sent: the API records user_entry.
        const body = { yieldShare: Number(text) };
        if (source.trim()) body.source = source;
        saveShare.mutate({ code: share.code, share: body }, { onSuccess: closeForm });
    }

    return (
        <li className="px-5 py-4">
            {!isEditing && (
                <div className="flex flex-wrap items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-gray-900">
                            {phase} <span className="font-normal text-gray-600">({formatText(share.yearsBand)})</span>
                        </p>
                        <p className="mt-0.5 text-lg font-semibold text-gray-900">
                            {formatShare(share.yieldShare)}{" "}
                            <span className="text-sm font-normal text-gray-600">of the full yield</span>
                        </p>
                        <p className="mt-0.5 text-xs text-gray-600">
                            {share.corrected
                                ? `Source: ${formatText(share.source)} · Updated ${formatTimestamp(share.lastUpdated)}`
                                    + ` · Default: ${formatShare(share.defaultYieldShare)}`
                                    + ` (${formatText(share.defaultSource)})`
                                : `Source: ${formatText(share.source)} · Default value`}
                        </p>
                    </div>
                    <button
                        ref={editButtonRef}
                        type="button"
                        onClick={openForm}
                        aria-label={`Edit the share of ${phase.toLowerCase()}`}
                        className={EDIT_BUTTON_CLASS}
                    >
                        <Pencil size={14} />
                        Edit
                    </button>
                </div>
            )}

            {isEditing && (
                <form noValidate onSubmit={handleSubmit} onKeyDown={handleKeyDown} className="flex flex-col gap-3">
                    <p className="text-sm font-semibold text-gray-900">
                        {phase} <span className="font-normal text-gray-600">({formatText(share.yearsBand)})</span>
                    </p>
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
                    <div className="flex flex-wrap items-start gap-x-4 gap-y-3">
                        <div className="flex flex-col gap-1">
                            <label htmlFor={`${id}-value`} className="text-xs font-semibold text-gray-700">
                                Share of the full yield (0 to 1)
                            </label>
                            <input
                                ref={inputRef}
                                id={`${id}-value`}
                                type="number"
                                min="0"
                                max="1"
                                step="any"
                                inputMode="decimal"
                                value={value}
                                onChange={changeValue}
                                aria-invalid={shareError ? true : undefined}
                                aria-describedby={shareError ? `${id}-value-error` : undefined}
                                className={`w-36 ${INPUT_CLASS}`}
                            />
                            {shareError && (
                                <p id={`${id}-value-error`} className="max-w-xs text-xs text-error">
                                    {shareError}
                                </p>
                            )}
                        </div>
                        <div className="flex min-w-0 flex-1 flex-col gap-1">
                            <label htmlFor={`${id}-source`} className="text-xs font-semibold text-gray-700">
                                Source
                            </label>
                            {/* The default placeholder is the text colour at half opacity, about 3:1
                                on white; gray-500 (#6a7282) reaches 4.84:1. */}
                            <input
                                id={`${id}-source`}
                                type="text"
                                value={source}
                                onChange={(event) => setSource(event.target.value)}
                                placeholder="user_entry"
                                aria-invalid={sourceError ? true : undefined}
                                aria-describedby={[`${id}-source-note`, sourceError ? `${id}-source-error` : null]
                                    .filter(Boolean)
                                    .join(" ")}
                                className={`w-full max-w-sm ${INPUT_CLASS} placeholder:text-gray-500`}
                            />
                            {sourceError && (
                                <p id={`${id}-source-error`} className="text-xs text-error">
                                    {sourceError}
                                </p>
                            )}
                            <p id={`${id}-source-note`} className="text-xs text-gray-600">
                                Optional. user_entry is recorded when left empty.
                            </p>
                        </div>
                    </div>
                    <div className="flex gap-2">
                        <button
                            type="submit"
                            disabled={saveShare.isPending}
                            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                        >
                            {saveShare.isPending ? "Saving…" : "Save"}
                        </button>
                        <button
                            type="button"
                            onClick={closeForm}
                            className="rounded-lg border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            )}
        </li>
    );
}

/**
 * Reference values: the yield per tree and harvest season of each variety,
 * and the share of the full yield given in each growth phase. The forecast
 * and the plant alerts compute from them, for every farm and block, so the
 * block filter does not apply here.
 *
 * Outside the dev profile the varieties start empty, shown as "No reference
 * values yet", while the three phases always have a share: their sourced
 * default until it is corrected. Saving a value refreshes the forecast.
 */
export default function ReferenceValuesSection() {
    const references = useVarietyReferences();
    const shares = useGrowthPhaseShares();
    const saveReference = useSaveVarietyReference();
    const saveShare = useSaveGrowthPhaseShare();
    // undefined: no dialog; null: a new variety; otherwise the reference to correct.
    const [dialogReference, setDialogReference] = useState(undefined);
    const [editingShareCode, setEditingShareCode] = useState(null);

    function openReferenceForm(reference) {
        saveReference.reset();
        setDialogReference(reference);
    }

    function closeReferenceForm() {
        saveReference.reset();
        setDialogReference(undefined);
    }

    function submitReference(values) {
        saveReference.mutate(
            { id: dialogReference === null ? null : dialogReference.id, reference: values },
            { onSuccess: closeReferenceForm },
        );
    }

    function editShare(code) {
        saveShare.reset();
        setEditingShareCode(code);
    }

    function closeShareForm() {
        saveShare.reset();
        setEditingShareCode(null);
    }

    return (
        <section aria-labelledby="reference-values-title" className="flex flex-col gap-4">
            <div>
                <h3 id="reference-values-title" className="font-heading text-base font-bold text-gray-900">
                    Reference values
                </h3>
                <p className="mt-1 max-w-2xl text-sm text-gray-600">
                    The forecast and the plant alerts compute from these values, for every farm and block. Each
                    value shows its source; a correction counts at once.
                </p>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3">
                <h4 className={SUBTITLE_CLASS}>Varieties</h4>
                <button
                    type="button"
                    onClick={() => openReferenceForm(null)}
                    className="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
                >
                    <Plus size={16} />
                    Add a variety
                </button>
            </div>

            {references.isPending && <LoadingBox label="Loading the reference values…" />}

            {references.isError && (
                <ErrorBox
                    title="The reference values could not be loaded"
                    onRetry={() => references.refetch()}
                    isRetrying={references.isFetching}
                />
            )}

            {!references.isPending && !references.isError && references.data.length === 0 && (
                <PlantsEmptyState
                    title="No reference values yet"
                    hint="Add a variety with its yield per tree and harvest months. Until then, the forecast lists its plantings apart."
                />
            )}

            {!references.isPending && !references.isError && references.data.length > 0 && (
                <VarietyReferencesTable references={references.data} onEdit={openReferenceForm} />
            )}

            <h4 className={SUBTITLE_CLASS}>Yield share by growth phase</h4>

            {shares.isPending && <LoadingBox label="Loading the yield shares…" />}

            {shares.isError && (
                <ErrorBox
                    title="The yield shares could not be loaded"
                    onRetry={() => shares.refetch()}
                    isRetrying={shares.isFetching}
                />
            )}

            {!shares.isPending && !shares.isError && (
                <ul className="divide-y divide-gray-100 rounded-xl border border-gray-200 bg-white">
                    {shares.data.map((share) => (
                        <GrowthPhaseShareItem
                            key={share.code}
                            share={share}
                            isEditing={editingShareCode === share.code}
                            saveShare={saveShare}
                            onEdit={editShare}
                            onClose={closeShareForm}
                        />
                    ))}
                </ul>
            )}

            {dialogReference !== undefined && (
                <VarietyReferenceModal
                    reference={dialogReference}
                    isSubmitting={saveReference.isPending}
                    error={saveReference.error}
                    onSubmit={submitReference}
                    onClose={closeReferenceForm}
                />
            )}
        </section>
    );
}
