import { useState } from "react";

const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-[#F7FDFB] px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

/**
 * Activity form, used to create an activity and to edit one. The API has no
 * reactivation call, so deactivation is handled by the table, not here.
 */
export default function ActivityForm({
    activity,
    isSubmitting,
    submitError,
    serverFieldErrors,
    onSubmit,
    onCancel,
}) {
    const isEditing = Boolean(activity);

    const [name, setName] = useState(activity?.name ?? "");
    const [price, setPrice] = useState(activity ? String(activity.price) : "");
    const [capacity, setCapacity] = useState(activity ? String(activity.capacity) : "");
    const [durationMinutes, setDurationMinutes] = useState(
        activity ? String(activity.durationMinutes) : "",
    );
    const [description, setDescription] = useState(activity?.description ?? "");
    const [fieldErrors, setFieldErrors] = useState({});

    function validate() {
        const errors = {};
        if (!name.trim()) {
            errors.name = "Activity name is required.";
        } else if (name.trim().length > 150) {
            errors.name = "Name must be at most 150 characters.";
        }
        const priceNumber = Number(price);
        if (price === "" || Number.isNaN(priceNumber) || priceNumber < 0) {
            errors.price = "Price must be a number of at least 0.";
        }
        const capacityNumber = Number(capacity);
        if (!Number.isInteger(capacityNumber) || capacityNumber < 1) {
            errors.capacity = "Capacity must be a whole number of at least 1.";
        }
        const durationNumber = Number(durationMinutes);
        if (!Number.isInteger(durationNumber) || durationNumber < 1) {
            errors.durationMinutes = "Duration must be a whole number of at least 1 minute.";
        }
        return errors;
    }

    function handleSubmit(event) {
        event.preventDefault();
        const errors = validate();
        setFieldErrors(errors);
        if (Object.keys(errors).length > 0) return;

        onSubmit({
            name: name.trim(),
            price: Number(price),
            capacity: Number(capacity),
            durationMinutes: Number(durationMinutes),
            description: description.trim() || null,
        });
    }

    const fieldError = (field) => fieldErrors[field] ?? serverFieldErrors?.[field];

    return (
        <form
            onSubmit={handleSubmit}
           
        >
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                    <label htmlFor="act-name" className={LABEL_CLASS}>
                        Activity name
                    </label>
                    <input
                        id="act-name"
                        type="text"
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                        placeholder="e.g. Solar workshop"
                        className={INPUT_CLASS}
                    />
                    {fieldError("name") && <p className={ERROR_CLASS}>{fieldError("name")}</p>}
                </div>
                <div>
                    <label htmlFor="act-price" className={LABEL_CLASS}>
                        Price (FCFA)
                    </label>
                    <input
                        id="act-price"
                        type="number"
                        min="0"
                        step="1"
                        value={price}
                        onChange={(event) => setPrice(event.target.value)}
                        placeholder="e.g. 5000"
                        className={INPUT_CLASS}
                    />
                    {fieldError("price") && <p className={ERROR_CLASS}>{fieldError("price")}</p>}
                </div>
                <div>
                    <label htmlFor="act-capacity" className={LABEL_CLASS}>
                        Capacity
                    </label>
                    <input
                        id="act-capacity"
                        type="number"
                        min="1"
                        value={capacity}
                        onChange={(event) => setCapacity(event.target.value)}
                        placeholder="e.g. 15"
                        className={INPUT_CLASS}
                    />
                    {fieldError("capacity") && (
                        <p className={ERROR_CLASS}>{fieldError("capacity")}</p>
                    )}
                </div>
                <div>
                    <label htmlFor="act-duration" className={LABEL_CLASS}>
                        Duration (min)
                    </label>
                    <input
                        id="act-duration"
                        type="number"
                        min="1"
                        value={durationMinutes}
                        onChange={(event) => setDurationMinutes(event.target.value)}
                        placeholder="e.g. 45"
                        className={INPUT_CLASS}
                    />
                    {fieldError("durationMinutes") && (
                        <p className={ERROR_CLASS}>{fieldError("durationMinutes")}</p>
                    )}
                </div>
            </div>

            <div className="mt-4">
                <label htmlFor="act-description" className={LABEL_CLASS}>
                    Description
                </label>
                <input
                    id="act-description"
                    type="text"
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    placeholder="Short description"
                    className={INPUT_CLASS}
                />
            </div>

            {submitError && (
                <p className="mt-4 rounded-md border border-error/30 bg-error/5 px-3 py-2 text-sm text-error">
                    {submitError}
                </p>
            )}

            <div className="mt-4 flex gap-2.5">
                <button
                    type="submit"
                    disabled={isSubmitting}
                    className="rounded-md bg-primary px-5 py-2.5 text-xs font-semibold tracking-wider text-white uppercase hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {isSubmitting ? "Saving…" : isEditing ? "Save activity" : "Create activity"}
                </button>
                <button
                    type="button"
                    onClick={onCancel}
                    disabled={isSubmitting}
                    className="rounded-md border border-line bg-white px-4 py-2.5 text-xs text-primary hover:bg-[#F2FBF9] disabled:opacity-60"
                >
                    Cancel
                </button>
            </div>
        </form>
    );
}