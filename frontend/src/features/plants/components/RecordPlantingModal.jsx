import { useState } from "react";
import { X } from "lucide-react";
import { plantingBody, spacingErrors, VIGOR_OPTIONS } from "../utils/plantingRequests";
import PlantsModal from "./PlantsModal";

const LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none";
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

/** Same rule as the API: a short code such as "A" or "B2"; spaces around it are removed. */
const BLOCK_CODE_PATTERN = /^\s*[A-Za-z0-9]{1,10}\s*$/;

/** Today as "YYYY-MM-DD", in local time: the latest planting date the form accepts. */
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
 * Form of a dated planting: the orchard is entered block by block, one planting
 * at a time. The spacings and the vigor are optional and go on the variety row;
 * on a row that already has them, the values entered replace them.
 *
 * The checks mirror the API rules, so most mistakes are caught before sending.
 * What the API still refuses is shown the same way:
 * - a 400 with fieldErrors: each message under its field;
 * - a 409 (variety already planted on that block), or any other error: one
 *   message above the form.
 */
export default function RecordPlantingModal({ isSubmitting, error, onSubmit, onClose }) {
    const [plantingDate, setPlantingDate] = useState("");
    const [blockCode, setBlockCode] = useState("");
    const [varietyName, setVarietyName] = useState("");
    const [treeCount, setTreeCount] = useState("");
    const [rowSpacing, setRowSpacing] = useState("");
    const [treeSpacing, setTreeSpacing] = useState("");
    const [vigor, setVigor] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];
    const errorProps = (field, id) =>
        fieldError(field)
            ? { "aria-invalid": true, "aria-describedby": `${id}-error` }
            : {};
    // The optional fields have a note, described along with an error.
    const optionalProps = (field, id) =>
        fieldError(field)
            ? { "aria-invalid": true, "aria-describedby": `${id}-error ${id}-note` }
            : { "aria-describedby": `${id}-note` };

    function validate() {
        const errors = {};
        if (!plantingDate) {
            errors.plantingDate = "Planting date is required.";
        } else if (plantingDate > today) {
            errors.plantingDate = "Planting date cannot be in the future.";
        }
        if (!blockCode.trim()) {
            errors.blockCode = "Block is required.";
        } else if (!BLOCK_CODE_PATTERN.test(blockCode)) {
            errors.blockCode = "Block must be a short code such as A or B2, without prefix or space.";
        }
        if (!varietyName.trim()) {
            errors.varietyName = "Variety is required.";
        } else if (varietyName.length > 255) {
            errors.varietyName = "Variety must be at most 255 characters.";
        }
        const count = Number(treeCount);
        if (treeCount === "" || !Number.isInteger(count) || count < 1) {
            errors.treeCount = "Number of trees must be a whole number of at least 1.";
        }
        return { ...errors, ...spacingErrors({ rowSpacing, treeSpacing }) };
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit(plantingBody({ plantingDate, blockCode, varietyName, treeCount, rowSpacing, treeSpacing, vigor }));
    }

    return (
        <PlantsModal labelledBy="record-planting-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h2
                    id="record-planting-title"
                    className="font-heading text-lg font-bold text-gray-900"
                >
                    Record a planting
                </h2>
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
                {/* Text in the darkened error hue of VigorBadge: #9a3f32 on the tinted
                    alert (#fcf6f5) -> 6.27:1. --color-error as text gave 4.38:1. */}
                {formError && (
                    <p
                        role="alert"
                        className="mb-5 rounded-lg border border-error/30 bg-error/5 px-3 py-2 text-sm text-[#9a3f32]"
                    >
                        {formError}
                    </p>
                )}

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <FormField id="planting-date" label="Planting date" error={fieldError("plantingDate")}>
                        <input
                            id="planting-date"
                            type="date"
                            autoFocus
                            max={today}
                            value={plantingDate}
                            onChange={(event) => setPlantingDate(event.target.value)}
                            className={INPUT_CLASS}
                            {...errorProps("plantingDate", "planting-date")}
                        />
                    </FormField>

                    <FormField id="planting-block" label="Block" error={fieldError("blockCode")}>
                        <input
                            id="planting-block"
                            type="text"
                            value={blockCode}
                            onChange={(event) => setBlockCode(event.target.value)}
                            placeholder="e.g. A"
                            className={INPUT_CLASS}
                            {...errorProps("blockCode", "planting-block")}
                        />
                    </FormField>

                    <FormField id="planting-variety" label="Variety" error={fieldError("varietyName")}>
                        <input
                            id="planting-variety"
                            type="text"
                            value={varietyName}
                            onChange={(event) => setVarietyName(event.target.value)}
                            placeholder="e.g. Keitt"
                            className={INPUT_CLASS}
                            {...errorProps("varietyName", "planting-variety")}
                        />
                    </FormField>

                    <FormField id="planting-tree-count" label="Number of trees" error={fieldError("treeCount")}>
                        <input
                            id="planting-tree-count"
                            type="number"
                            min="1"
                            step="1"
                            inputMode="numeric"
                            value={treeCount}
                            onChange={(event) => setTreeCount(event.target.value)}
                            className={INPUT_CLASS}
                            {...errorProps("treeCount", "planting-tree-count")}
                        />
                    </FormField>

                    <FormField id="planting-row-spacing" label="Inter-row spacing (m)"
                        error={fieldError("rowSpacingM")} note="Optional.">
                        <input
                            id="planting-row-spacing"
                            type="number"
                            min="0.1"
                            max="30"
                            step="0.1"
                            inputMode="decimal"
                            value={rowSpacing}
                            onChange={(event) => setRowSpacing(event.target.value)}
                            placeholder="e.g. 8"
                            className={`${INPUT_CLASS} placeholder:text-gray-500`}
                            {...optionalProps("rowSpacingM", "planting-row-spacing")}
                        />
                    </FormField>

                    <FormField id="planting-tree-spacing" label="Intra-row spacing (m)"
                        error={fieldError("treeSpacingM")} note="Optional.">
                        <input
                            id="planting-tree-spacing"
                            type="number"
                            min="0.1"
                            max="30"
                            step="0.1"
                            inputMode="decimal"
                            value={treeSpacing}
                            onChange={(event) => setTreeSpacing(event.target.value)}
                            placeholder="e.g. 8"
                            className={`${INPUT_CLASS} placeholder:text-gray-500`}
                            {...optionalProps("treeSpacingM", "planting-tree-spacing")}
                        />
                    </FormField>

                    <FormField id="planting-vigor" label="Vigor" error={fieldError("vigor")} note="Optional.">
                        <select
                            id="planting-vigor"
                            value={vigor}
                            onChange={(event) => setVigor(event.target.value)}
                            className={INPUT_CLASS}
                            {...optionalProps("vigor", "planting-vigor")}
                        >
                            {VIGOR_OPTIONS.map((option) => (
                                <option key={option.code} value={option.code}>
                                    {option.label}
                                </option>
                            ))}
                        </select>
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
                        disabled={isSubmitting}
                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                    >
                        {isSubmitting ? "Recording…" : "Record planting"}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
