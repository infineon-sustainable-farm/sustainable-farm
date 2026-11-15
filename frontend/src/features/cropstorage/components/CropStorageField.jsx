const LABEL_CLASS = "mb-1.5 block text-xs tracking-wide text-primary uppercase";
const INPUT_CLASS = "w-full rounded-md border border-line bg-white px-2.5 py-2 text-sm";
const ERROR_CLASS = "mt-1 text-xs text-error";

export default function CropStorageField({
    id,
    label,
    value,
    onChange,
    error,
    type = "text",
    options,
    required = false,
    min,
    max,
    step,
    placeholder,
}) {
    return (
        <div>
            <label htmlFor={id} className={LABEL_CLASS}>
                {label}
            </label>
            {options ? (
                <select
                    id={id}
                    value={value}
                    onChange={onChange}
                    required={required}
                    className={INPUT_CLASS}
                >
                    <option value="">Select {label.toLowerCase()}</option>
                    {options.map((option) => (
                        <option key={option.value} value={option.value}>
                            {option.label}
                        </option>
                    ))}
                </select>
            ) : (
                <input
                    id={id}
                    type={type}
                    value={value}
                    onChange={onChange}
                    required={required}
                    min={min}
                    max={max}
                    step={step}
                    placeholder={placeholder}
                    className={INPUT_CLASS}
                />
            )}
            {error && <p className={ERROR_CLASS}>{error}</p>}
        </div>
    );
}
