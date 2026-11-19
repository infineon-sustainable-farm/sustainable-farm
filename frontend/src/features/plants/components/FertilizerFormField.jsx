import { Field } from "../../../shared/ui";

/**
 * A field of the fertilizer dialogs: the kit Field, plus a note that stays under the control even
 * while an error is shown (the stock left, the date in letters, a rule of the field). The note is
 * tied to the control by the caller, with aria-describedby="<id>-note".
 */
export default function FertilizerFormField({ id, label, error, note, children }) {
    return (
        <Field id={id} label={label} error={error}>
            {children}
            {note && (
                <p id={`${id}-note`} className="mt-1 text-xs text-text-muted">
                    {note}
                </p>
            )}
        </Field>
    );
}

