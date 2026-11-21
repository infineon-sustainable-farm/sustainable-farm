import { useTranslation } from "react-i18next";
import { Button } from "../../../shared/ui";

/** Cancel and submit at the bottom of an energy form; both are disabled while it saves. */
function FormActions({ onCancel, saving, submitLabel, className }) {
  const { t } = useTranslation(["energysupply", "common"]);
  return (
    <div className={`mt-2 flex justify-end gap-2 ${className ?? ""}`}>
      <Button variant="secondary" onClick={onCancel} disabled={saving}>
        {t("common:actions.cancel")}
      </Button>
      <Button type="submit" disabled={saving}>
        {saving ? t("form.saving") : submitLabel}
      </Button>
    </div>
  );
}
export default FormActions;
