import { useState } from "react";
import { X } from "lucide-react";
import { useVarieties } from "../hooks/useVarieties";
import { formatBlock } from "../utils/format";
import PlantsModal from "./PlantsModal";

const LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none disabled:cursor-not-allowed disabled:bg-gray-50";
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

/** Today as "YYYY-MM-DD", in local time: the latest harvest date the form accepts. */
function todayAsIsoDate() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${now.getFullYear()}-${month}-${day}`;
}

function FormField({ id, label, error, note, children }) {
    return (
        <div>
            <label htmlFor={id} className={LABEL_CLASS}>
                {label}
            </label>
            {children}
            {error && (
                <p id={`${id}-error`} className={ERROR_CLASS}>
                    {error}
                </p>
            )}
            {note && (
                <p id={`${id}-note`} className={NOTE_CLASS}>
                    {note}
                </p>
            )}
        </div>
    );
}

/**
 * Form of a harvest: one picking of one variety on one block.
 *
 * The block and the variety are picked from lists, so the variety name goes
 * out exactly as it was planted, accents included. Only the varieties with a
 * recorded planting on the block are offered: the API refuses a harvest of any
 * other. The checks mirror the API rules; what the API still refuses is shown
 * the same way as for a planting:
 * - a 400 with fieldErrors: each message under its field;
 * - a 422 (no planting of that variety on the block, or a harvest before the
 *   planting), or any other error: one message above the form.
 */
export default function RecordHarvestModal({ blocks, isSubmitting, error, onSubmit, onClose }) {
    const [harvestDate, setHarvestDate] = useState("");
    const [blockCode, setBlockCode] = useState("");
    const [varietyName, setVarietyName] = useState("");
    const [quantityKg, setQuantityKg] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);

    const varieties = useVarieties({ blockCode: blockCode || null });
    // A variety row has a current tree count once a planting is recorded for it.
    const plantedVarieties = blockCode && varieties.data
        ? varieties.data.filter((variety) => variety.currentTreeCount !== null && variety.currentTreeCount !== undefined)
        : [];

    let varietyNote = null;
    if (blockCode && varieties.isError) {
        varietyNote = "Varieties could not be loaded";
    } else if (blockCode && varieties.isSuccess && plantedVarieties.length === 0) {
        varietyNote = "No planted variety on this block";
    }
    const canChooseVariety = plantedVarieties.length > 0;

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];
    const describedBy = (field, id) =>
        [fieldError(field) ? `${id}-error` : null, field === "varietyName" && varietyNote ? `${id}-note` : null]
            .filter(Boolean)
            .join(" ") || undefined;
    const fieldProps = (field, id) => ({
        "aria-invalid": fieldError(field) ? true : undefined,
        "aria-describedby": describedBy(field, id),
    });

    function changeBlock(value) {
        setBlockCode(value);
        // The variety belongs to the block: another block means another list.
        setVarietyName("");
    }

    function validate() {
        const errors = {};
        if (!harvestDate) {
            errors.harvestDate = "Harvest date is required.";
        } else if (harvestDate > today) {
            errors.harvestDate = "Harvest date cannot be in the future.";
        }
        if (!blockCode) {
            errors.blockCode = "Block is required.";
        }
        if (!varietyName) {
            errors.varietyName = "Variety is required.";
        }
        const quantity = Number(quantityKg);
        if (quantityKg.trim() === "" || !Number.isFinite(quantity) || quantity <= 0) {
            errors.quantityKg = "Quantity must be a number greater than 0.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit({ harvestDate, blockCode, varietyName, quantityKg: Number(quantityKg) });
    }

    return (
        <PlantsModal labelledBy="record-harvest-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h3
                    id="record-harvest-title"
                    className="font-heading text-xl font-bold text-gray-900"
                >
                    Record a harvest
                </h3>
                <button
                    type="button"
                    onClick={onClose}
                    aria-label="Close"
                    className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-gray-800"
                >
                    <X size={20} />
                </button>
            </div>

            <form noValidate onSubmit={handleSubmit} className="px-6 py-5">
                {formError && (
                    <p
                        role="alert"
                        className="mb-5 rounded-lg border border-error/30 bg-error/5 px-3 py-2 text-sm text-error"
                    >
                        {formError}
                    </p>
                )}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <FormField id="harvest-date" label="Harvest date" error={fieldError("harvestDate")}>
                        <input
                            id="harvest-date"
                            type="date"
                            autoFocus
                            max={today}
                            value={harvestDate}
                            onChange={(event) => setHarvestDate(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("harvestDate", "harvest-date")}
                        />
                    </FormField>

                    <FormField id="harvest-block" label="Block" error={fieldError("blockCode")}>
                        <select
                            id="harvest-block"
                            value={blockCode}
                            onChange={(event) => changeBlock(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("blockCode", "harvest-block")}
                        >
                            {/* Empty value: sending without a choice gives "Block is required." */}
                            <option value="">Select a block</option>
                            {blocks.map((block) => (
                                <option key={block} value={block}>
                                    {formatBlock(block)}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField
                        id="harvest-variety"
                        label="Variety"
                        error={fieldError("varietyName")}
                        note={varietyNote}
                    >
                        <select
                            id="harvest-variety"
                            value={varietyName}
                            disabled={!canChooseVariety}
                            onChange={(event) => setVarietyName(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("varietyName", "harvest-variety")}
                        >
                            {/* Empty value: sending without a choice gives "Variety is required." */}
                            <option value="">Select a variety</option>
                            {plantedVarieties.map((variety) => (
                                <option key={variety.id} value={variety.name}>
                                    {variety.name}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField id="harvest-quantity" label="Quantity (kg)" error={fieldError("quantityKg")}>
                        <input
                            id="harvest-quantity"
                            type="number"
                            min="0"
                            step="any"
                            inputMode="decimal"
                            value={quantityKg}
                            onChange={(event) => setQuantityKg(event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("quantityKg", "harvest-quantity")}
                        />
                    </FormField>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        disabled={isSubmitting || varietyNote !== null}
                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                    >
                        {isSubmitting ? "Recording…" : "Record harvest"}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
