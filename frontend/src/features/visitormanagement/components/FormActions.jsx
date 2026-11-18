import { useTranslation } from "react-i18next";
import { Button } from "../../../shared/ui";

/**
 * Cancel and submit at the bottom of a visitor management form, as in machinery. The submit
 * button shows a spinner while the request runs; both are blocked meanwhile.
 */
export default function FormActions({ submitLabel, onCancel, isSubmitting = false, submitDisabled = false }) {
    const { t } = useTranslation("common");
    return (
        <div className="mt-4 flex justify-end gap-2">
            <Button variant="secondary" onClick={onCancel} disabled={isSubmitting}>
                {t("actions.cancel")}
            </Button>
            <Button type="submit" loading={isSubmitting} disabled={submitDisabled}>
                {submitLabel ?? t("actions.save")}
            </Button>
        </div>
    );
}
