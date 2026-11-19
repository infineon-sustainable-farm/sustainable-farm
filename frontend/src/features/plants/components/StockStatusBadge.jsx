import { useTranslation } from "react-i18next";
import { Badge } from "../../../shared/ui";

/**
 * Stock status of a fertilizer, as decided by the API: "Low stock" when the
 * stock is at or below the alert threshold, nothing otherwise. No stock or
 * threshold is compared here.
 */
export default function StockStatusBadge({ belowThreshold }) {
    const { t } = useTranslation("plants");
    if (!belowThreshold) return null;
    return <Badge tone="error">{t("fertilizer.lowStock")}</Badge>;
}
