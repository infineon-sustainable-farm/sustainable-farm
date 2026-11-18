import { CircleAlert } from "lucide-react";
import { useTranslation } from "react-i18next";
import { formatOrigin } from "../utils/format";
import { NURSERY_ORIGINS } from "../utils/nurseryRequests";
import { RADIO_CLASS } from "./nurseryForm";

/** Origin of a new batch, a radio group: in-house production or external purchase. */
export default function NurseryOriginField({ value, error, onChange }) {
    const { t } = useTranslation("plants");
    return (
        <fieldset className="sm:col-span-2">
            <legend className="text-sm font-medium">{t("nursery.addForm.origin")}</legend>
            <div className="mt-2 flex flex-wrap gap-x-6 gap-y-2">
                {NURSERY_ORIGINS.map((origin) => (
                    <label key={origin} className="flex items-center gap-2 text-sm text-text">
                        <input
                            type="radio"
                            name="batch-origin"
                            value={origin}
                            checked={value === origin}
                            onChange={() => onChange(origin)}
                            aria-invalid={error ? true : undefined}
                            aria-describedby={error ? "batch-origin-error" : undefined}
                            className={RADIO_CLASS}
                        />
                        {formatOrigin(origin)}
                    </label>
                ))}
            </div>
            {error && (
                <p id="batch-origin-error" className="mt-1 flex items-center gap-1 text-xs text-error-fg">
                    <CircleAlert size={14} aria-hidden="true" className="shrink-0" />
                    <span>{error}</span>
                </p>
            )}
        </fieldset>
    );
}
