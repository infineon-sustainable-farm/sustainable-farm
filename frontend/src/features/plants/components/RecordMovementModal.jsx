import { useState } from "react";
import { X } from "lucide-react";
import { useFertilizers } from "../hooks/useFertilizers";
import { useVarietyBlocks } from "../hooks/useVarieties";
import { formatCode, formatQuantity, formatUnit } from "../utils/format";
import PlantsModal from "./PlantsModal";

const LABEL_CLASS = "text-xs font-semibold tracking-wide text-gray-500 uppercase";
const INPUT_CLASS =
    "mt-1 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-primary focus:outline-none disabled:cursor-not-allowed disabled:bg-gray-50";
const ERROR_CLASS = "mt-1 text-xs text-error";
const NOTE_CLASS = "mt-1 text-xs text-gray-600";

const MOVEMENT_TYPES = ["PURCHASE", "APPLICATION", "LOSS"];

// The fields each type adds to the fertilizer, the date and the quantity.
const TYPE_FIELDS = {
    PURCHASE: ["supplier", "totalCost", "currency"],
    APPLICATION: ["blockCode", "applicator", "method"],
    LOSS: ["reason"],
};

// Each API route names the date after its movement.
const DATE_FIELDS = {
    PURCHASE: "purchaseDate",
    APPLICATION: "applicationDate",
    LOSS: "lossDate",
};

const SUBMIT_LABELS = {
    PURCHASE: "Record purchase",
    APPLICATION: "Record application",
    LOSS: "Record loss",
};

// XOF, the farm's currency, is the default of the API as well.
const CURRENCIES = [
    { code: "XOF", label: "FCFA" },
    { code: "EUR", label: "EUR" },
];

const INITIAL_VALUES = {
    fertilizerId: "",
    date: "",
    quantity: "",
    supplier: "",
    totalCost: "",
    currency: "XOF",
    blockCode: "",
    applicator: "",
    method: "",
    reason: "",
};

/** Same rules as the API: a quantity above 0 with at most 3 decimals, a cost with at most 2. */
const QUANTITY_PATTERN = /^\d+(\.\d{1,3})?$/;
const COST_PATTERN = /^\d+(\.\d{1,2})?$/;

/** Same rule as a planting: a short code such as "A" or "B2"; spaces around it are removed. */
const BLOCK_CODE_PATTERN = /^\s*[A-Za-z0-9]{1,10}\s*$/;

/** Today as "YYYY-MM-DD", in local time: the latest movement date the form accepts. */
function todayAsIsoDate() {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${now.getFullYear()}-${month}-${day}`;
}

function isPositiveNumber(text, pattern) {
    const trimmed = text.trim();
    return pattern.test(trimmed) && Number(trimmed) > 0;
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
 * Form of a stock movement: a purchase, an application on a block or a loss.
 *
 * The three share the fertilizer, the date and the quantity, in the unit of
 * the fertilizer. Changing the type empties the fields of the previous one
 * and drops their errors, the API's included, and only the fields of the
 * chosen type are sent. For an application or a loss, the note under the
 * quantity gives the stock as the API reports it; whether the stock covers
 * the quantity is left to the API.
 *
 * The block of an application is typed, with the same rule as a planting:
 * the base fertilizer goes in before the tree, so the block may have no
 * planting yet. The blocks of the recorded varieties are offered as
 * suggestions. No farm is sent, as for a planting.
 *
 * The checks mirror the API rules; what the API still refuses is shown the
 * same way as for a planting:
 * - a 400 with fieldErrors: each message under its field;
 * - a 422 (the quantity exceeds the stock), or any other error: one message
 *   above the form.
 */
export default function RecordMovementModal({ isSubmitting, error, onSubmit, onResetError, onClose }) {
    const [movementType, setMovementType] = useState("PURCHASE");
    const [values, setValues] = useState(INITIAL_VALUES);
    // A number field holds "" for text it cannot read, such as "1e"; these keep
    // such a value from passing as an empty one.
    const [unreadable, setUnreadable] = useState({ quantity: false, totalCost: false });
    const [fieldErrors, setFieldErrors] = useState({});
    const [today] = useState(todayAsIsoDate);

    const fertilizers = useFertilizers();
    const { data: blocks } = useVarietyBlocks();
    const catalogue = fertilizers.data ?? [];
    const fertilizer = catalogue.find((item) => String(item.id) === values.fertilizerId) ?? null;

    let fertilizerNote = null;
    if (fertilizers.isError) {
        fertilizerNote = "Fertilizers could not be loaded";
    } else if (fertilizers.isSuccess && catalogue.length === 0) {
        fertilizerNote = "No fertilizer in the catalogue yet";
    }
    const stockNote = fertilizer && movementType !== "PURCHASE"
        ? `In stock: ${formatQuantity(fertilizer.currentStock, fertilizer.unit)}`
        : null;

    const serverFieldErrors = error?.status === 400 ? error.data?.fieldErrors : null;
    const formError = error && !serverFieldErrors ? error.message : null;
    const fieldError = (field) =>
        fieldErrors[field] ?? serverFieldErrors?.[field === "date" ? DATE_FIELDS[movementType] : field];
    const fieldProps = (field, id, note = null) => ({
        "aria-invalid": fieldError(field) ? true : undefined,
        "aria-describedby":
            [fieldError(field) ? `${id}-error` : null, note ? `${id}-note` : null].filter(Boolean).join(" ")
            || undefined,
    });

    function setValue(field, value) {
        setValues((current) => ({ ...current, [field]: value }));
    }

    function setNumber(field, event) {
        setValue(field, event.target.value);
        setUnreadable((current) => ({ ...current, [field]: event.target.validity.badInput }));
    }

    function changeType(nextType) {
        const previousFields = TYPE_FIELDS[movementType];
        setValues((current) => ({
            ...current,
            ...Object.fromEntries(previousFields.map((field) => [field, INITIAL_VALUES[field]])),
        }));
        setUnreadable((current) => ({ ...current, totalCost: false }));
        setFieldErrors((current) =>
            Object.fromEntries(Object.entries(current).filter(([field]) => !previousFields.includes(field))));
        // The API's answer was about the previous type: none of it applies any more.
        onResetError();
        setMovementType(nextType);
    }

    function validate() {
        const errors = {};
        if (!values.fertilizerId) {
            errors.fertilizerId = "Fertilizer is required.";
        }
        if (!values.date) {
            errors.date = "Date is required.";
        } else if (values.date > today) {
            errors.date = "Date cannot be in the future.";
        }
        if (unreadable.quantity || !isPositiveNumber(values.quantity, QUANTITY_PATTERN)) {
            errors.quantity = "Quantity must be a number greater than 0, with at most 3 decimals.";
        }
        if (movementType === "PURCHASE") {
            if (!values.supplier.trim()) {
                errors.supplier = "Supplier is required.";
            } else if (values.supplier.length > 255) {
                errors.supplier = "Supplier must be at most 255 characters.";
            }
            if (unreadable.totalCost
                || (values.totalCost.trim() !== "" && !isPositiveNumber(values.totalCost, COST_PATTERN))) {
                errors.totalCost = "Total cost must be a number greater than 0, with at most 2 decimals.";
            }
        }
        if (movementType === "APPLICATION") {
            if (!values.blockCode.trim()) {
                errors.blockCode = "Block is required.";
            } else if (!BLOCK_CODE_PATTERN.test(values.blockCode)) {
                errors.blockCode = "Block must be a short code such as A or B2, without prefix or space.";
            }
            if (!values.applicator.trim()) {
                errors.applicator = "Applicator is required.";
            } else if (values.applicator.length > 255) {
                errors.applicator = "Applicator must be at most 255 characters.";
            }
            if (values.method.length > 255) {
                errors.method = "Method must be at most 255 characters.";
            }
        }
        if (movementType === "LOSS") {
            if (!values.reason.trim()) {
                errors.reason = "Reason is required.";
            } else if (values.reason.length > 255) {
                errors.reason = "Reason must be at most 255 characters.";
            }
        }
        return errors;
    }

    /** The request body of the chosen type; optional fields left empty are not sent. */
    function requestBody() {
        const body = { [DATE_FIELDS[movementType]]: values.date, quantity: Number(values.quantity) };
        if (movementType === "PURCHASE") {
            body.supplier = values.supplier;
            if (values.totalCost.trim()) {
                body.totalCost = Number(values.totalCost);
                body.currency = values.currency;
            }
        } else if (movementType === "APPLICATION") {
            body.blockCode = values.blockCode;
            body.applicator = values.applicator;
            if (values.method.trim()) body.method = values.method;
        } else {
            body.reason = values.reason;
        }
        return body;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;
        onSubmit({ movementType, fertilizerId: Number(values.fertilizerId), body: requestBody() });
    }

    return (
        <PlantsModal labelledBy="record-movement-title" onClose={onClose}>
            <div className="flex items-start justify-between gap-4 border-b border-gray-200 px-6 py-4">
                <h3
                    id="record-movement-title"
                    className="font-heading text-xl font-bold text-gray-900"
                >
                    Record a movement
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

                <fieldset className="mb-5">
                    <legend className={LABEL_CLASS}>Movement type</legend>
                    <div className="mt-2 flex flex-wrap gap-x-6 gap-y-2">
                        {MOVEMENT_TYPES.map((type, index) => (
                            <label key={type} className="flex items-center gap-2 text-sm text-gray-800">
                                <input
                                    type="radio"
                                    name="movement-type"
                                    value={type}
                                    autoFocus={index === 0}
                                    checked={movementType === type}
                                    onChange={() => changeType(type)}
                                    className="accent-primary"
                                />
                                {formatCode(type)}
                            </label>
                        ))}
                    </div>
                </fieldset>

                <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <FormField
                        id="movement-fertilizer"
                        label="Fertilizer"
                        error={fieldError("fertilizerId")}
                        note={fertilizerNote}
                    >
                        <select
                            id="movement-fertilizer"
                            value={values.fertilizerId}
                            disabled={catalogue.length === 0}
                            onChange={(event) => setValue("fertilizerId", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("fertilizerId", "movement-fertilizer", fertilizerNote)}
                        >
                            {/* Empty value: sending without a choice gives "Fertilizer is required." */}
                            <option value="">Select a fertilizer</option>
                            {catalogue.map((item) => (
                                <option key={item.id} value={item.id}>
                                    {item.name}
                                </option>
                            ))}
                        </select>
                    </FormField>

                    <FormField id="movement-date" label="Date" error={fieldError("date")}>
                        <input
                            id="movement-date"
                            type="date"
                            max={today}
                            value={values.date}
                            onChange={(event) => setValue("date", event.target.value)}
                            className={INPUT_CLASS}
                            {...fieldProps("date", "movement-date")}
                        />
                    </FormField>

                    <FormField
                        id="movement-quantity"
                        label={fertilizer ? `Quantity (${formatUnit(fertilizer.unit)})` : "Quantity"}
                        error={fieldError("quantity")}
                        note={stockNote}
                    >
                        <input
                            id="movement-quantity"
                            type="number"
                            min="0"
                            step="any"
                            inputMode="decimal"
                            value={values.quantity}
                            onChange={(event) => setNumber("quantity", event)}
                            className={INPUT_CLASS}
                            {...fieldProps("quantity", "movement-quantity", stockNote)}
                        />
                    </FormField>

                    {movementType === "PURCHASE" && (
                        <>
                            <FormField id="movement-supplier" label="Supplier" error={fieldError("supplier")}>
                                <input
                                    id="movement-supplier"
                                    type="text"
                                    value={values.supplier}
                                    onChange={(event) => setValue("supplier", event.target.value)}
                                    className={INPUT_CLASS}
                                    {...fieldProps("supplier", "movement-supplier")}
                                />
                            </FormField>

                            <FormField id="movement-total-cost" label="Total cost" error={fieldError("totalCost")}>
                                <input
                                    id="movement-total-cost"
                                    type="number"
                                    min="0"
                                    step="any"
                                    inputMode="decimal"
                                    value={values.totalCost}
                                    onChange={(event) => setNumber("totalCost", event)}
                                    className={INPUT_CLASS}
                                    {...fieldProps("totalCost", "movement-total-cost")}
                                />
                            </FormField>

                            <FormField id="movement-currency" label="Currency" error={fieldError("currency")}>
                                <select
                                    id="movement-currency"
                                    value={values.currency}
                                    onChange={(event) => setValue("currency", event.target.value)}
                                    className={INPUT_CLASS}
                                    {...fieldProps("currency", "movement-currency")}
                                >
                                    {CURRENCIES.map(({ code, label }) => (
                                        <option key={code} value={code}>
                                            {label}
                                        </option>
                                    ))}
                                </select>
                            </FormField>
                        </>
                    )}

                    {movementType === "APPLICATION" && (
                        <>
                            <FormField id="movement-block" label="Block" error={fieldError("blockCode")}>
                                <input
                                    id="movement-block"
                                    type="text"
                                    list="movement-block-options"
                                    value={values.blockCode}
                                    onChange={(event) => setValue("blockCode", event.target.value)}
                                    className={INPUT_CLASS}
                                    {...fieldProps("blockCode", "movement-block")}
                                />
                                <datalist id="movement-block-options">
                                    {(blocks ?? []).map((block) => (
                                        <option key={block} value={block} />
                                    ))}
                                </datalist>
                            </FormField>

                            <FormField id="movement-applicator" label="Applicator" error={fieldError("applicator")}>
                                <input
                                    id="movement-applicator"
                                    type="text"
                                    value={values.applicator}
                                    onChange={(event) => setValue("applicator", event.target.value)}
                                    className={INPUT_CLASS}
                                    {...fieldProps("applicator", "movement-applicator")}
                                />
                            </FormField>

                            <FormField id="movement-method" label="Method" error={fieldError("method")}>
                                <input
                                    id="movement-method"
                                    type="text"
                                    value={values.method}
                                    onChange={(event) => setValue("method", event.target.value)}
                                    className={INPUT_CLASS}
                                    {...fieldProps("method", "movement-method")}
                                />
                            </FormField>
                        </>
                    )}

                    {movementType === "LOSS" && (
                        <FormField id="movement-reason" label="Reason" error={fieldError("reason")}>
                            <input
                                id="movement-reason"
                                type="text"
                                value={values.reason}
                                onChange={(event) => setValue("reason", event.target.value)}
                                className={INPUT_CLASS}
                                {...fieldProps("reason", "movement-reason")}
                            />
                        </FormField>
                    )}
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
                        disabled={isSubmitting || fertilizerNote !== null}
                        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
                    >
                        {isSubmitting ? "Recording…" : SUBMIT_LABELS[movementType]}
                    </button>
                </div>
            </form>
        </PlantsModal>
    );
}
