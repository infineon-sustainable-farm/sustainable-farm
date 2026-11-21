import { useTranslation } from "react-i18next";
import { formatEur, formatXof } from "../utils/format";

/**
 * A cost at the last purchase price, as the API converts it: the amount in
 * FCFA with the amount in euros under it. While no rate is recorded, the API
 * sends one currency only, and that one is shown alone. While no purchase of
 * the fertilizer has a cost, there is no price: the cell says so instead of
 * showing 0.
 */
export default function PlanCost({ xof, eur }) {
    const { t } = useTranslation("plants");
    const hasXof = xof !== null && xof !== undefined;
    const hasEur = eur !== null && eur !== undefined;
    if (!hasXof && !hasEur) {
        return <span className="text-text-muted">{t("fertilizer.plans.noPrice")}</span>;
    }
    if (!hasXof) return formatEur(eur);
    return (
        <>
            {formatXof(xof)}
            {hasEur && <span className="block text-xs text-text-muted">{formatEur(eur)}</span>}
        </>
    );
}
